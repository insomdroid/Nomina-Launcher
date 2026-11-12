package com.insomdroid.nomina.feature.home

import android.Manifest
import android.content.Context
import android.content.ContentUris
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.insomdroid.nomina.data.AppRepository
import com.insomdroid.nomina.data.SettingsRepository
import com.insomdroid.nomina.data.smartspace.CalendarEvent
import com.insomdroid.nomina.data.smartspace.CalendarRepository
import com.insomdroid.nomina.data.smartspace.CurrentWeather
import com.insomdroid.nomina.data.smartspace.NextEventPicker
import com.insomdroid.nomina.data.smartspace.WeatherRepository
import com.insomdroid.nomina.model.AppItem
import com.insomdroid.nomina.model.BatteryStatus
import com.insomdroid.nomina.launcher.system.BatteryMonitor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import com.insomdroid.nomina.ui.launcherTechnicalFontFamily
import com.insomdroid.nomina.ui.launcherDisplayFontFamily
import androidx.core.content.ContextCompat
import com.insomdroid.nomina.ui.themedTextStyle
import com.insomdroid.nomina.ui.expressive.ExpressiveRadius

@HiltViewModel
class SmartspaceViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val weatherRepository: WeatherRepository,
    private val settingsRepository: SettingsRepository,
    private val appRepository: AppRepository,
    batteryMonitor: BatteryMonitor,
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

    fun onHomeResumed() {
        calendarRepository.startObserving()
        refresh()
    }

    fun onHomePaused() {
        calendarRepository.stopObserving()
    }

    /** Re-checks the permissions (after a runtime grant) and re-queries the sources. */
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

    /** One shared minute clock drives both the date/time row and next-event selection. */
    val minuteTimestamp: StateFlow<Long> = minuteTicks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), System.currentTimeMillis())

    val batteryStatus: StateFlow<BatteryStatus?> = batteryMonitor.status

    /** Re-picked every minute so an ended event flips to the next one without a provider change. */
    val nextEvent: StateFlow<CalendarEvent?> =
        combine(calendarRepository.events, minuteTimestamp) { events, now -> NextEventPicker.pick(events, now) }
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
    val batteryStatus: BatteryStatus? = null,
)

/**
 * The built-in smartspace widget: three compact information rows for local date/time, the next
 * agenda item and weather condition, then battery and network status.
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
        batteryStatus = viewModel.batteryStatus.collectAsStateWithLifecycle().value,
    )
    val now by viewModel.minuteTimestamp.collectAsStateWithLifecycle()
    SmartspaceContent(
        state,
        onRefresh = viewModel::refresh,
        onOpenWeatherApp = viewModel::openWeatherApp,
        modifier = modifier,
        now = now,
        onResume = viewModel::onHomeResumed,
        onPause = viewModel::onHomePaused,
    )
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
    now: Long = System.currentTimeMillis(),
    onResume: () -> Unit = onRefresh,
    onPause: () -> Unit = {},
) {
    val context = LocalContext.current
    val event = state.nextEvent
    val deviceStatus = rememberNetworkStatus()

    LifecycleResumeEffect(onResume, onPause) {
        onResume()
        onPauseOrDispose { onPause() }
    }

    val calendarPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onRefresh() }
    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onRefresh() }

    val timeFormat = rememberTimeFormat()
    val dateText = remember(now) {
        SimpleDateFormat("EEEE d 'of' MMMM yyyy", Locale.getDefault()).format(Date(now))
    }
    val timeText = remember(now, timeFormat) { timeFormat.format(Date(now)) }
    val timeZoneText = remember(now) {
        SimpleDateFormat("z", Locale.getDefault()).format(Date(now))
    }

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
        state.weather != null -> weatherCondition(state.weather.weatherCode)
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
        fontSize = 11.sp,
        color = ink,
    )
    val dateTimeStyle = technicalStyle.copy(
        lineHeight = 11.sp,
        fontWeight = FontWeight.Medium,
    )
    val timeLine = remember(timeText, timeZoneText) {
        "${timeText.uppercase(Locale.getDefault())} ${timeZoneText.uppercase(Locale.getDefault())}"
    }
    val batteryLine = remember(state.batteryStatus, deviceStatus.networkText) {
        buildAnnotatedString {
            val batteryStatus = state.batteryStatus
            val battery = batteryStatus?.let {
                "${if (it.charging) "Charging" else "Battery"} ${it.percent.coerceIn(0, 100)}%"
            } ?: "Battery —"
            val upperBattery = battery.uppercase(Locale.getDefault())
            val numberStart = upperBattery.lastIndexOf(' ').let { if (it >= 0) it + 1 else 0 }
            if (numberStart in upperBattery.indices && upperBattery.substring(numberStart).any(Char::isDigit)) {
                append(upperBattery.substring(0, numberStart))
                withStyle(SpanStyle(fontFamily = launcherDisplayFontFamily())) {
                    append(upperBattery.substring(numberStart))
                }
            } else {
                append(upperBattery)
            }
            append(" · ${deviceStatus.networkText.uppercase(Locale.getDefault())}")
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(widgetSurfaceColor(), RoundedCornerShape(ExpressiveRadius.card))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        TapTarget(
            onTap = { openClockApp(context) },
            interactionSource = noIndication,
            alignment = Alignment.CenterStart,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(0.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = dateText.uppercase(Locale.getDefault()),
                    color = ink,
                    style = themedTextStyle(dateTimeStyle),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = timeLine,
                    color = ink,
                    style = themedTextStyle(dateTimeStyle),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            TapTarget(
                onTap = agendaAction,
                interactionSource = noIndication,
                alignment = Alignment.CenterStart,
            ) {
                Text(
                    text = agendaText.uppercase(Locale.getDefault()),
                    color = ink.copy(alpha = 0.92f),
                    style = themedTextStyle(dateTimeStyle),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (weatherText != null) {
                TapTarget(
                    onTap = weatherAction,
                    interactionSource = noIndication,
                    alignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = weatherText.uppercase(Locale.getDefault()),
                        color = ink.copy(alpha = 0.92f),
                        style = themedTextStyle(dateTimeStyle.copy(fontWeight = FontWeight.Normal)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Text(
            text = batteryLine,
            color = ink.copy(alpha = 0.92f),
            style = themedTextStyle(dateTimeStyle),
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
    val networkText: String = "Offline",
)

@Composable
private fun rememberNetworkStatus(): SmartspaceDeviceStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(SmartspaceDeviceStatus()) }

    LifecycleResumeEffect(context) {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        var callbackRegistered = false
        var callback: ConnectivityManager.NetworkCallback? = null
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

            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = updateNetwork(network)
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    updateNetwork(network, networkCapabilities)
                }
                override fun onLost(network: Network) = updateNetwork()
            }
            callback = networkCallback
            callbackRegistered = runCatching {
                connectivity.registerDefaultNetworkCallback(networkCallback)
                true
            }.getOrDefault(false)
            updateNetwork()
        }
        onPauseOrDispose {
            if (callbackRegistered) {
                callback?.let { networkCallback ->
                    runCatching { connectivity?.unregisterNetworkCallback(networkCallback) }
                }
            }
        }
    }
    return status
}

/** A compact tappable text row; without [onTap] it remains plain text. */
@Composable
private fun TapTarget(
    onTap: (() -> Unit)?,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = true,
    alignment: Alignment = Alignment.Center,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .then(
                if (onTap == null) Modifier
                else Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onTap,
                ),
            ),
        contentAlignment = alignment,
    ) { content() }
}

