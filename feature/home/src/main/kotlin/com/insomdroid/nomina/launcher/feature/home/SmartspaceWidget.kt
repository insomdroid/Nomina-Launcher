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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.insomdroid.nomina.data.smartspace.WeatherRepository
import com.insomdroid.nomina.model.AppItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import com.insomdroid.nomina.ui.launcherTechnicalFontFamily
import com.insomdroid.nomina.ui.launcherDisplayFontFamily
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
        SimpleDateFormat("EEEE d 'of' MMMM yyyy", Locale.getDefault()).format(Date(now))
    }
    val timeText = remember(now, timeFormat) { timeFormat.format(Date(now)) }
    val timeZoneText = remember(now) {
        SimpleDateFormat("z", Locale.getDefault()).format(Date(now))
    }
    val dateTimeText = "$dateText · $timeText $timeZoneText"

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
        fontSize = 12.sp,
        color = ink,
    )
    val batteryLine = remember(deviceStatus.batteryText, deviceStatus.networkText) {
        buildAnnotatedString {
            val battery = deviceStatus.batteryText
            val numberStart = battery.lastIndexOf(' ').let { if (it >= 0) it + 1 else 0 }
            if (numberStart in battery.indices && battery.substring(numberStart).any(Char::isDigit)) {
                append(battery.substring(0, numberStart))
                withStyle(SpanStyle(fontFamily = launcherDisplayFontFamily())) {
                    append(battery.substring(numberStart))
                }
            } else {
                append(battery)
            }
            append("  " + deviceStatus.networkText)
        }
    }

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
                style = themedTextStyle(technicalStyle.copy(fontWeight = FontWeight.Medium)),
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
                modifier = Modifier.weight(1f, fill = false),
                fillWidth = false,
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_weather_outline),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = ink.copy(alpha = 0.92f),
                        )
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
        }

        Text(
            text = batteryLine,
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

