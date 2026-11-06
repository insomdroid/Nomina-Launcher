package com.insomdroid.nomina.feature.home

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContentUris
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.provider.CalendarContract
import android.text.format.DateUtils
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.insomdroid.nomina.data.AppRepository
import com.insomdroid.nomina.data.SettingsRepository
import com.insomdroid.nomina.data.smartspace.CalendarEvent
import com.insomdroid.nomina.data.smartspace.CalendarRepository
import com.insomdroid.nomina.data.smartspace.CurrentWeather
import com.insomdroid.nomina.data.smartspace.NextEventPicker
import com.insomdroid.nomina.data.smartspace.WeatherCodes
import com.insomdroid.nomina.data.smartspace.WeatherRepository
import com.insomdroid.nomina.model.AppItem
import kotlin.math.roundToInt
import java.util.Date
import javax.inject.Inject
import com.insomdroid.nomina.ui.launcherTechnicalFontFamily
import androidx.core.content.ContextCompat
import com.insomdroid.nomina.ui.themedTextStyle

@HiltViewModel
class SmartspaceViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val weatherRepository: WeatherRepository,
    private val settingsRepository: SettingsRepository,
    private val appRepository: AppRepository,
) : ViewModel() {

    val hasCalendarPermission = MutableStateFlow(calendarRepository.hasPermission())
    val hasLocationPermission = MutableStateFlow(weatherRepository.hasPermission())

    /** Weather slot enabled in Settings ▸ Home (the permission gate is separate). */
    val showWeather: StateFlow<Boolean> = settingsRepository.settings
        .map { it.showWeather }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val weather: StateFlow<CurrentWeather?> =
        combine(weatherRepository.weather, settingsRepository.settings) { weather, settings ->
            weather.takeIf { settings.showWeather }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Re-checks the permissions (resume / after a runtime grant) and re-queries the sources. */
    fun refresh() {
        hasCalendarPermission.value = calendarRepository.hasPermission()
        hasLocationPermission.value = weatherRepository.hasPermission()
        calendarRepository.refresh()
        // Gate on the PERSISTED setting, not the StateFlow: at process start the stateIn initial
        // value (true) would fire one location read + network query even with weather turned off.
        viewModelScope.launch {
            val s = settingsRepository.settings.first()
            if (s.showWeather) weatherRepository.refresh()
        }
    }

    /** Re-picked every minute so an ended event flips to the next one without a provider change. */
    val nextEvent: StateFlow<CalendarEvent?> =
        combine(calendarRepository.events, minuteTicks()) { events, now -> NextEventPicker.pick(events, now) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun minuteTicks(): Flow<Long> = flow {
        while (true) {
            val now = System.currentTimeMillis()
            emit(now)
            delay(60_000 - now % 60_000)
        }
    }

    /** The app a tap on the weather line opens (Settings > Home), only while it is installed. */
    val weatherApp: StateFlow<AppItem?> =
        combine(settingsRepository.settings, appRepository.apps) { s, apps -> weatherAppIn(apps, s.weatherAppKey) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Opens the chosen weather app; a no-op when none is chosen or it is no longer installed. */
    fun openWeatherApp() {
        viewModelScope.launch {
            val key = settingsRepository.settings.first().weatherAppKey
            weatherAppIn(appRepository.apps.first(), key)?.let { app ->
                appRepository.launch(app).onFailure { Log.w("SmartspaceViewModel", "Failed to launch ${app.key}", it) }
            }
        }
    }

    init {
        refresh()
    }
}

/** The app [key] names when it is among [apps]; a blank or unknown key resolves to nothing. */
internal fun weatherAppIn(apps: List<AppItem>, key: String): AppItem? =
    if (key.isBlank()) null else apps.firstOrNull { it.key == key }

/** What the smartspace shows, read from [SmartspaceViewModel] (or built directly by a test). */
internal data class SmartspaceState(
    val hasCalendarPermission: Boolean,
    val nextEvent: CalendarEvent?,
    val hasLocationPermission: Boolean,
    val showWeather: Boolean,
    val weather: CurrentWeather?,
    val weatherApp: AppItem?,
)

/**
 * The built-in smartspace widget: a large clock (12/24 h from the SYSTEM setting), the localized
 * date, and the ongoing-or-next calendar event within 7 days. Taps open the system clock app, the
 * calendar's day view, and the event itself. The v2 weather slot goes beside the date row.
 */
@Composable
fun SmartspaceWidget(
    modifier: Modifier = Modifier,
    viewModel: SmartspaceViewModel = hiltViewModel(),
) {
    val state = SmartspaceState(
        hasCalendarPermission = viewModel.hasCalendarPermission.collectAsStateWithLifecycle().value,
        nextEvent = viewModel.nextEvent.collectAsStateWithLifecycle().value,
        hasLocationPermission = viewModel.hasLocationPermission.collectAsStateWithLifecycle().value,
        showWeather = viewModel.showWeather.collectAsStateWithLifecycle().value,
        weather = viewModel.weather.collectAsStateWithLifecycle().value,
        weatherApp = viewModel.weatherApp.collectAsStateWithLifecycle().value,
    )
    SmartspaceContent(state, onRefresh = viewModel::refresh, onOpenWeatherApp = viewModel::openWeatherApp, modifier = modifier)
}

/**
 * Three left-aligned information rows:
 * 1. Full local date and the device's 12/24-hour local time.
 * 2. The next calendar agenda item and the current weather condition.
 * 3. Battery level/charging state and the active internet transport.
 */
@Composable
internal fun SmartspaceContent(
    state: SmartspaceState,
    onRefresh: () -> Unit,
    onOpenWeatherApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val event = state.nextEvent
    val deviceStatus = rememberDeviceStatus()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000 - now % 60_000)
        }
    }
    LifecycleResumeEffect(Unit) {
        now = System.currentTimeMillis()
        onRefresh()
        onPauseOrDispose { }
    }

    val calendarPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onRefresh() }
    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onRefresh() }

    val timeFormat = rememberTimeFormat()
    val dateText = remember(now) {
        DateUtils.formatDateTime(
            context,
            now,
            DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR,
        )
    }
    val timeText = remember(now, timeFormat) { timeFormat.format(Date(now)) }
    val dateTimeText = "$dateText · $timeText"

    val agendaText = when {
        !state.hasCalendarPermission -> "Calendar permission needed"
        event == null -> "No upcoming agenda"
        else -> {
            val whenText = when {
                event.begin <= now && event.end > now -> "Now"
                event.allDay -> "All day"
                DateUtils.isToday(event.begin) -> timeFormat.format(Date(event.begin))
                else -> DateUtils.formatDateTime(
                    context,
                    event.begin,
                    DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY,
                ) + " " + timeFormat.format(Date(event.begin))
            }
            val title = event.title.ifBlank { "Untitled event" }
            "$whenText · $title"
        }
    }
    val agendaAction: () -> Unit = when {
        !state.hasCalendarPermission -> ({ calendarPermission.launch(Manifest.permission.READ_CALENDAR) })
        event != null -> ({
            runCatching {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.eventId),
                    )
                        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.begin)
                        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end),
                )
            }
        })
        else -> ({
            val uri = CalendarContract.CONTENT_URI.buildUpon()
                .appendPath("time").appendPath(now.toString()).build()
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        })
    }

    val weatherText = when {
        !state.showWeather -> null
        !state.hasLocationPermission -> "Location needed"
        state.weather != null -> "${WeatherCodes.emoji(state.weather.weatherCode)} ${weatherCondition(state.weather.weatherCode)}"
        else -> "Weather unavailable"
    }
    val weatherAction: (() -> Unit)? = when {
        !state.showWeather -> null
        !state.hasLocationPermission -> ({ locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) })
        state.weatherApp != null -> onOpenWeatherApp
        else -> null
    }

    val ink = widgetContentColor()
    val noIndication = remember { MutableInteractionSource() }
    val technicalStyle = TextStyle(
        fontFamily = launcherTechnicalFontFamily(),
        fontSize = 13.sp,
        color = ink,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(widgetSurfaceColor(), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        TapTarget(
            onTap = { openClockApp(context) },
            interactionSource = noIndication,
            alignment = Alignment.CenterStart,
        ) {
            Text(
                text = dateTimeText,
                color = ink,
                style = themedTextStyle(technicalStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TapTarget(
                onTap = agendaAction,
                modifier = Modifier.weight(1f),
                interactionSource = noIndication,
                alignment = Alignment.CenterStart,
            ) {
                Text(
                    text = agendaText,
                    color = ink.copy(alpha = 0.92f),
                    style = themedTextStyle(technicalStyle),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (weatherText != null) {
                TapTarget(
                    onTap = weatherAction,
                    fillWidth = false,
                    interactionSource = noIndication,
                    alignment = Alignment.CenterEnd,
                    modifier = Modifier.padding(start = 10.dp),
                ) {
                    Text(
                        text = weatherText,
                        color = ink.copy(alpha = 0.92f),
                        style = themedTextStyle(technicalStyle),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Text(
            text = "${deviceStatus.batteryText} · ${deviceStatus.networkText}",
            color = ink.copy(alpha = 0.92f),
            style = themedTextStyle(technicalStyle),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun weatherCondition(code: Int): String = when (code) {
    0 -> "Clear"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51, 53, 55 -> "Drizzle"
    56, 57 -> "Freezing drizzle"
    61, 63, 65 -> "Rain"
    66, 67 -> "Freezing rain"
    71, 73, 75 -> "Snowfall"
    77 -> "Snow grains"
    80, 81, 82 -> "Rain showers"
    85, 86 -> "Snow showers"
    95 -> "Thunderstorm"
    96, 99 -> "Thunderstorm with hail"
    else -> "Cloudy"
}

private data class SmartspaceDeviceStatus(
    val batteryText: String = "Battery —",
    val networkText: String = "Offline",
)

@Composable
private fun rememberDeviceStatus(): SmartspaceDeviceStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(SmartspaceDeviceStatus()) }

    DisposableEffect(context) {
        fun updateBattery(intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level < 0 || scale <= 0) return
            val percent = (level * 100 / scale).coerceIn(0, 100)
            val charging = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_STATUS_FULL -> true
                else -> false
            }
            status = status.copy(batteryText = "${if (charging) "Charging" else "Battery"} $percent%")
        }

        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) = updateBattery(intent)
        }
        val batteryIntent = ContextCompat.registerReceiver(
            context,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        updateBattery(batteryIntent)

        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        var callbackRegistered = false
        if (connectivity != null) {
            fun updateNetwork(
                network: Network? = connectivity.activeNetwork,
                capabilities: NetworkCapabilities? = network?.let(connectivity::getNetworkCapabilities),
            ) {
                val label = when {
                    network == null || capabilities == null ||
                        !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> "Offline"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                    else -> "Internet"
                }
                status = status.copy(networkText = label)
            }

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = updateNetwork(network)
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    updateNetwork(network, networkCapabilities)
                }
                override fun onLost(network: Network) = updateNetwork()
            }
            callbackRegistered = runCatching {
                connectivity.registerDefaultNetworkCallback(callback)
                true
            }.getOrDefault(false)
            updateNetwork()
            onDispose {
                runCatching { context.unregisterReceiver(batteryReceiver) }
                if (callbackRegistered) runCatching { connectivity.unregisterNetworkCallback(callback) }
            }
        } else {
            onDispose { runCatching { context.unregisterReceiver(batteryReceiver) } }
        }
    }
    return status
}

/** WCAG 2.5.5 target size: a tappable line grows to at least this height when the footprint has room. */
internal val TOUCH_TARGET = 44.dp

/**
 * The clock and the lines under it, stacked and centred like a Column, with the lines' heights
 * budgeted from the room the footprint leaves after the clock: every line keeps at least its
 * natural height (a two-line event stays two lines), and the room left after that raises the
 * shortest lines first, all of them to the same level at most [TOUCH_TARGET] tall. Natural heights
 * that do not fit are cut to the room left, top down, so the card never draws past its footprint.
 * The first child is the clock, always at its natural height; a line's [lineGap] is the space
 * above it, outside its hit area and outside the budget.
 */
@Composable
private fun BudgetedLines(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val width = constraints.maxWidth
        val childConstraints = Constraints(minWidth = constraints.minWidth, maxWidth = width)
        val clock = measurables.first().measure(childConstraints)
        val lines = measurables.drop(1)
        val gaps = lines.map { ((it.parentData as? LineGap)?.gap ?: 0.dp).roundToPx() }
        val natural = lines.map { it.minIntrinsicHeight(width) }
        val room = if (constraints.hasBoundedHeight) constraints.maxHeight - clock.height - gaps.sum() else 0
        val heights = if (constraints.hasBoundedHeight && natural.sum() > room) {
            // More content than footprint (a tight footprint with a huge font): the lines are cut to
            // the room left, top down, so nothing draws over the grid row below the widget.
            var left = room
            natural.map { h -> h.coerceAtMost(left.coerceAtLeast(0)).also { left -= it } }
        } else {
            budgetedLineHeights(natural, room, TOUCH_TARGET.roundToPx())
        }
        val placeables = lines.mapIndexed { i, line ->
            line.measure(childConstraints.copy(minHeight = heights[i], maxHeight = heights[i]))
        }
        val layoutWidth = (placeables + clock).maxOf { it.width }.coerceIn(constraints.minWidth, constraints.maxWidth)
        val layoutHeight = (clock.height + gaps.sum() + heights.sum()).coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(layoutWidth, layoutHeight) {
            var y = 0
            clock.placeRelative((layoutWidth - clock.width) / 2, y)
            y += clock.height
            placeables.forEachIndexed { i, p ->
                y += gaps[i]
                p.placeRelative((layoutWidth - p.width) / 2, y)
                y += p.height
            }
        }
    }
}

/**
 * The heights of lines with [natural] content heights sharing [room] px: each at least natural,
 * the leftover raising the shortest lines first to one common level, capped at [target].
 */
internal fun budgetedLineHeights(natural: List<Int>, room: Int, target: Int): List<Int> {
    if (natural.isEmpty()) return natural
    val sorted = natural.sorted()
    var level = 0
    var above = sorted.sum() // the natural heights of the lines still taller than the level
    for (k in 1..sorted.size) {
        above -= sorted[k - 1]
        val candidate = (room - above) / k
        if (candidate < sorted[k - 1]) break
        level = candidate
    }
    level = level.coerceAtMost(target)
    return natural.map { maxOf(it, level) }
}

/** The space above a line in [BudgetedLines]: outside its hit area and outside the budget. */
private fun Modifier.lineGap(gap: Dp): Modifier = this.then(LineGap(gap))

private data class LineGap(val gap: Dp) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = this@LineGap
}

/**
 * A tappable line of the widget. Its hit area is at least [minHeight] tall (or, with [fillHeight],
 * as tall as its parent lets it be) with the content centred ([alignment]) in it and, with
 * [fillWidth], as wide as the card, so a small label is as easy to hit as a button. Without
 * [onTap] it is plain content with no click action. [ripple] keeps the default indication (the
 * short layout on its surface); the wallpaper layouts pass their shared no-indication
 * [interactionSource] instead.
 */
@Composable
private fun TapTarget(
    onTap: (() -> Unit)?,
    modifier: Modifier = Modifier,
    minHeight: Dp = 0.dp,
    fillWidth: Boolean = true,
    fillHeight: Boolean = false,
    alignment: Alignment = Alignment.Center,
    ripple: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
            .heightIn(min = minHeight)
            .then(
                when {
                    onTap == null -> Modifier
                    ripple -> Modifier.clickable(onClick = onTap)
                    else -> Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onTap)
                },
            ),
        contentAlignment = alignment,
    ) { content() }
}

/** The separator between the merged middle-row parts (compact mode). */
@Composable
private fun MiddleDot(scale: Float, shadow: Shadow) {
    Text(
        text = "·",
        color = widgetContentColor().copy(alpha = 0.8f),
        style = themedTextStyle(TextStyle(fontSize = (15 * scale).sp, shadow = shadow)),
        modifier = Modifier.padding(horizontal = (5 * scale).dp),
    )
}
