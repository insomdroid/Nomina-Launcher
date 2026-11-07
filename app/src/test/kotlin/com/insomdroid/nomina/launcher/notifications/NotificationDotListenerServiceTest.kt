package com.insomdroid.nomina.notifications

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Process
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import com.insomdroid.nomina.data.NotificationBadgeRepository
import com.insomdroid.nomina.data.PersonEntry
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowNotificationListenerService
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter.from

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30, 35], application = Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationDotListenerServiceTest {
    private lateinit var service: NotificationDotListenerService
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val me = Person.Builder().setName("Me").setKey("self").build()
    private val anna = Person.Builder().setName("Anna").setKey("anna").build()

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // The parser and callback are exercised without binding a real notification listener.
        service = NotificationDotListenerService()
        ReflectionHelpers.callInstanceMethod<Unit>(service, "attachBaseContext", from(Context::class.java, context))
        service.badgeRepository = NotificationBadgeRepository()
    }

    @After fun tearDown() {
        service.onDestroy()
        Dispatchers.resetMain()
    }

    private fun post(notification: StatusBarNotification) {
        service.cancelNotification(notification.key)
        Shadow.extract<ShadowNotificationListenerService>(service).addActiveNotification(notification)
        service.onNotificationPosted(notification)
    }

    private fun sbn(notification: Notification) = StatusBarNotification(
        "chat.app", "chat.app", 1, null, 1000, 0, 0, notification, Process.myUserHandle(), System.currentTimeMillis(),
    )

    private fun person(notification: StatusBarNotification): PersonEntry? = ReflectionHelpers.callInstanceMethod(
        service, "personEntry", from(StatusBarNotification::class.java, notification),
        from(Long::class.javaPrimitiveType!!, 0L), from(RankingMap::class.java, null), from(Ranking::class.java, Ranking()),
    )

    private fun app(notification: StatusBarNotification): PersonEntry? = ReflectionHelpers.callInstanceMethod(
        service, "appEntry", from(StatusBarNotification::class.java, notification), from(Long::class.javaPrimitiveType!!, 0L),
    )

    private fun chat(vararg incoming: Boolean): StatusBarNotification {
        val style = NotificationCompat.MessagingStyle(me)
        incoming.forEachIndexed { i, other -> style.addMessage(if (other) "Incoming" else "My reply", i.toLong(), if (other) anna else me) }
        return sbn(NotificationCompat.Builder(context, "chat").setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Me").setContentText("My reply").setStyle(style).build())
    }

    @Test fun ownRepliesCannotReappearThroughPersonOrAppFallback() {
        val own = chat(false)
        assertThat(person(own)).isNull()
        assertThat(app(own)).isNull()
    }

    @Test fun mixedConversationKeepsOnlyIncomingTextAndCount() {
        val entry = person(chat(true, false))!!
        assertThat(entry.name).isEqualTo("Anna")
        assertThat(entry.text).isEqualTo("Incoming")
        assertThat(entry.count).isEqualTo(1)
    }

    /** The shape a community post takes on Android 16+: a big picture offloaded to a content URI,
     *  null picture / large-icon keys, two plain actions, no category. */
    private fun bigPicture(title: String, text: String, id: Int): StatusBarNotification {
        val open = PendingIntent.getActivity(context, id, Intent("test.open"), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, "notifications")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title).setContentText(text).setAutoCancel(true).setContentIntent(open)
            .addAction(0, "Go to community", open).addAction(0, "Turn off updates", open)
            .setStyle(NotificationCompat.BigPictureStyle()
                .bigPicture(android.graphics.drawable.Icon.createWithContentUri("content://com.android.bitmapoffload/bitmaps/$id")))
            .build()
        notification.extras.putParcelable(Notification.EXTRA_PICTURE, null)
        notification.extras.putParcelable(Notification.EXTRA_LARGE_ICON, null)
        return StatusBarNotification(
            "news.app", "news.app", 0, "tag-$id", 1000, 0, 0, notification, Process.myUserHandle(), System.currentTimeMillis(),
        )
    }

    @Test fun bigPictureAppNotificationsBecomeOneAppTile() {
        ReflectionHelpers.setField(service, "connected", true)
        val first = bigPicture("Now in r/SEO", "Why does the favicon not show?", 1)
        val second = bigPicture("Now in r/android_beta", "Crash when entering the pin code", 2)
        assertThat(person(first)).isNull()
        assertThat(app(first)!!.title).isEqualTo("Now in r/SEO")
        post(first)
        post(second)
        val tile = service.badgeRepository.people.value.single()
        assertThat(tile.personKey).isEqualTo("app:news.app/0")
        assertThat(tile.count).isEqualTo(2)
    }

    @Test fun packageChangeRefreshesMailClassificationWithoutReconnecting() = runTest {
        val notification = sbn(NotificationCompat.Builder(context, "mail")
            .setContentTitle("Sender").setContentText("Subject").build())
        assertThat(person(notification)).isNull()
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "chat.app"
                name = "MailActivity"
                applicationInfo = ApplicationInfo().apply { packageName = "chat.app" }
            }
        }
        shadowOf(context.packageManager).addResolveInfoForIntent(
            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:someone@example.com")), resolveInfo,
        )
        ReflectionHelpers.getField<BroadcastReceiver>(service, "packageReceiver")
            .onReceive(context, Intent(Intent.ACTION_PACKAGE_ADDED, Uri.parse("package:chat.app")))
        ReflectionHelpers.getField<Job>(service, "packageRefreshJob").join()
        assertThat(person(notification)!!.name).isEqualTo("Sender")
    }
}
