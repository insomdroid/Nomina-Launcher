package com.insomdroid.nomina.feature.settings

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.graphics.Bitmap
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import com.insomdroid.nomina.ui.component.DrawerWallpaperPicture
import com.insomdroid.nomina.data.DrawerWallpaper
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.insomdroid.nomina.data.SettingsRepository
import com.insomdroid.nomina.designsystem.theme.LauncherTextStyles
import com.insomdroid.nomina.feature.backup.BackupScreen
import com.insomdroid.nomina.model.AppItem
import com.insomdroid.nomina.model.LauncherSettings
import com.insomdroid.nomina.model.drawerSurface
import com.insomdroid.nomina.ui.DefaultLauncher
import com.insomdroid.nomina.ui.LauncherIcons
import com.insomdroid.nomina.ui.LongPressScale
import com.insomdroid.nomina.ui.component.AppIcon
import com.insomdroid.nomina.ui.component.LocalIconPack
import com.insomdroid.nomina.ui.component.LocalIconShape
import com.insomdroid.nomina.ui.component.LocalAutoResizeIcons
import com.insomdroid.nomina.ui.component.LocalIconReshapeMode
import com.insomdroid.nomina.ui.component.NotificationBadge
import com.insomdroid.nomina.ui.expressive.DarkExpressivePalette
import com.insomdroid.nomina.ui.expressive.ExpressiveActionRow
import com.insomdroid.nomina.ui.expressive.ExpressiveCard
import com.insomdroid.nomina.ui.expressive.ExpressivePalette
import com.insomdroid.nomina.ui.expressive.LightExpressivePalette
import com.insomdroid.nomina.ui.expressive.LocalExpressivePalette
import com.insomdroid.nomina.ui.expressive.ExpressiveRadius
import com.insomdroid.nomina.ui.expressive.ExpressiveSpacing

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val drawerWallpaper by viewModel.drawerWallpaperPicture.collectAsStateWithLifecycle()
    val allApps by viewModel.apps.collectAsStateWithLifecycle()
    val hiddenKeys by viewModel.hiddenKeys.collectAsStateWithLifecycle()
    val drawerFolders by viewModel.drawerFolders.collectAsStateWithLifecycle()
    val iconPacks by viewModel.iconPacks.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources
    // Size multipliers read as whole percents, so 100 is the default and the number is easy to come back to.
    val percentText: (Float) -> String = { resources.getString(R.string.settings_percent, scalePercent(it)) }
    val previewIcon = rememberLauncherIconBitmap()
    // Saveable: the activity is recreated on rotation and on a theme switch, and that can happen
    // under the Backup page's file picker — a page that forgot it was open never received the
    // picker's result (an empty file in Downloads, no message).
    var showHiddenManager by rememberSaveable { mutableStateOf(false) }
    var showFolderManager by rememberSaveable { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var settingsSearch by rememberSaveable { mutableStateOf("") }
    var showWeatherAppPicker by rememberSaveable { mutableStateOf(false) }
    var showPageSpeedPicker by rememberSaveable { mutableStateOf(false) }
    var showIconPackPicker by rememberSaveable { mutableStateOf(false) }
    var showHomeMarkerPicker by rememberSaveable { mutableStateOf(false) }
    var showBackup by rememberSaveable { mutableStateOf(false) }
    // Hoisted above the subpage early-returns so the main list's scroll position survives a visit to
    // the hidden-apps / backup subpage (an inline rememberScrollState would leave composition with the
    // list and forget the position — the "settings always restart at the top" complaint). Saveable so
    // it also survives activity recreation. A fresh Settings open starts where the list was left if
    // that was less than five minutes ago (SettingsScrollMemory), else at the top.
    val mainScrollState = rememberSaveable(saver = ScrollState.Saver) {
        ScrollState(SettingsScrollMemory.restore(SystemClock.elapsedRealtime()))
    }
    var isDefaultLauncher by remember { mutableStateOf(DefaultLauncher.isDefault(context)) }
    // The home app can also change in the system settings while this screen is behind them.
    LifecycleResumeEffect(Unit) {
        isDefaultLauncher = DefaultLauncher.isDefault(context)
        // Leaving (HOME, Back, another app on top): note the place and the time for the next open.
        onPauseOrDispose { SettingsScrollMemory.remember(mainScrollState.value, SystemClock.elapsedRealtime()) }
    }
    // Re-check after the user returns from the system "default home app" / role chooser.
    val setDefaultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { isDefaultLauncher = DefaultLauncher.isDefault(context) }
    // Notification access is granted/revoked in the SYSTEM settings (plain startActivity, no result
    // contract) — re-check on every resume so the row shows the fresh state when the user comes back.
    var notifAccessGranted by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    LifecycleResumeEffect(Unit) {
        notifAccessGranted = isNotificationAccessGranted(context)
        onPauseOrDispose { }
    }
    val palette = if (isSystemInDarkTheme()) DarkExpressivePalette else LightExpressivePalette

    CompositionLocalProvider(
        LocalExpressivePalette provides palette,
        // So AppIcons in the pickers (left-edge app, hidden apps) match the selected icon pack / themed
        // icons like every other surface.
        LocalIconPack provides s.iconPackPackage,
        LocalIconShape provides s.iconShape,
        LocalAutoResizeIcons provides s.autoResizeIcons,
        LocalIconReshapeMode provides s.reshapeLegacyIcons,
    ) {
        if (showBackup) {
            BackupScreen(onBack = { showBackup = false }, modifier = modifier.fillMaxSize())
            return@CompositionLocalProvider
        }

        if (showHiddenManager) {
            HiddenAppsManager(
                apps = allApps,
                hiddenKeys = hiddenKeys,
                onSetHidden = viewModel::setAppHidden,
                onBack = { showHiddenManager = false },
                modifier = modifier.fillMaxSize(),
            )
            return@CompositionLocalProvider
        }

        if (showFolderManager) {
            val newFolderName = stringResource(R.string.drawer_folder_default)
            DrawerFoldersManager(
                folders = drawerFolders,
                apps = allApps,
                hiddenKeys = hiddenKeys,
                onCreate = { viewModel.createDrawerFolder(newFolderName) },
                onRename = viewModel::renameDrawerFolder,
                onDelete = viewModel::deleteDrawerFolder,
                onSetMember = viewModel::setAppInDrawerFolder,
                onBack = { showFolderManager = false },
                modifier = modifier.fillMaxSize(),
            )
            return@CompositionLocalProvider
        }

        Surface(modifier = modifier.fillMaxSize(), color = palette.bg) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val spacing = 120.dp.toPx()
                    val radius = 0.7.dp.toPx()
                    var y = spacing / 2f
                    while (y < size.height) {
                        var x = spacing / 2f
                        while (x < size.width) {
                            drawCircle(
                                color = palette.faint.copy(alpha = 0.13f),
                                radius = radius,
                                center = Offset(x, y),
                            )
                            x += spacing
                        }
                        y += spacing
                    }
                }
                Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(mainScrollState)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            ) {
                BackHandler(enabled = selectedCategory != null) { selectedCategory = null }
                if (selectedCategory == null) {
                    Text(
                        text = "N O M I N A  /  SYSTEM",
                        color = palette.faint,
                        style = LauncherTextStyles.settingsEyebrow,
                        modifier = Modifier.padding(start = 6.dp, top = 18.dp, bottom = 2.dp),
                    )
                    Text(
                        text = stringResource(R.string.settings_title),
                        color = palette.text,
                        style = LauncherTextStyles.settingsTitle,
                        modifier = Modifier.padding(start = 6.dp, bottom = 12.dp),
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = settingsSearch,
                        onValueChange = { settingsSearch = it },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        singleLine = true,
                        placeholder = { Text("Search settings…") },
                    )
                    val categories = listOf(
                        Triple("home", "01", "Home screen|Layout, wallpaper, widgets"),
                        Triple("dock", "02", "Dock|Visibility, spacing, behavior"),
                        Triple("drawer", "03", "App drawer|Grid, sorting, search"),
                        Triple("icons", "04", "Icons|Packs, shapes, appearance"),
                        Triple("folders", "05", "Folders|Layout and appearance"),
                        Triple("gestures", "06", "Gestures|Navigation and shortcuts"),
                        Triple("notifications", "07", "Notifications|Badges and indicators"),
                        Triple("widgets", "08", "Widgets|Clock, weather and information"),
                        Triple("backup", "09", "Backup|Export and restore"),
                        Triple("about", "10", "About|Version and feedback"),
                    )
                    val query = settingsSearch.trim().lowercase()
                    val keywordMap = mapOf(
                        "home" to listOf("grid", "columns", "rows", "wallpaper", "page", "status bar", "widget"),
                        "dock" to listOf("auto hide", "opacity", "bottom spacing", "dock icons", "show dock"),
                        "drawer" to listOf("search", "scrubber", "frequent apps", "hidden apps", "sorting"),
                        "icons" to listOf("icon pack", "icon shape", "resize", "reshape", "legacy"),
                        "folders" to listOf("folder", "folder columns", "folder names"),
                        "gestures" to listOf("swipe", "double tap", "long press"),
                        "notifications" to listOf("notification", "badge", "dot", "count"),
                        "widgets" to listOf("weather", "calendar", "location", "permission", "background"),
                        "backup" to listOf("export", "restore", "backup"),
                        "about" to listOf("version", "feedback", "update"),
                    )
                    val filtered = categories.filter { row ->
                        query.isBlank() || row.third.lowercase().contains(query) ||
                            keywordMap[row.first].orEmpty().any { it.contains(query) || query.contains(it) }
                    }
                    if (query.isNotBlank()) {
                        Text(
                            text = "${filtered.size} RESULT${if (filtered.size == 1) "" else "S"} FOUND",
                            color = palette.dim,
                            style = LauncherTextStyles.settingsEyebrow,
                            modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 8.dp),
                        )
                    }
                    filtered.forEach { row ->
                        val parts = row.third.split("|")
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedCategory = row.first }
                                .padding(horizontal = 6.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(row.second, color = palette.dim, modifier = Modifier.widthIn(min = 44.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(parts[0], color = palette.text, style = LauncherTextStyles.settingsRowLabel)
                                Text(parts[1], color = palette.dim, style = LauncherTextStyles.settingsDescription)
                            }
                            Text("›", color = palette.text, fontSize = 24.sp)
                        }
                        androidx.compose.material3.HorizontalDivider(color = palette.trackOff.copy(alpha = 0.65f))
                    }
                    if (query.isNotBlank() && filtered.isEmpty()) {
                        Text("No matching settings", color = palette.dim, modifier = Modifier.padding(12.dp))
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selectedCategory = null }
                            .padding(top = 18.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("‹", color = palette.text, fontSize = 28.sp, modifier = Modifier.padding(end = 16.dp))
                        Text("SETTINGS  /  ${selectedCategory.uppercase()}", color = palette.dim, style = LauncherTextStyles.settingsEyebrow)
                    }
                    Text(
                        text = when (selectedCategory) {
                            "home" -> "Home screen"
                            "dock" -> "Dock"
                            "drawer" -> "App drawer"
                            "icons" -> "Icons"
                            "folders" -> "Folders"
                            "gestures" -> "Gestures"
                            "notifications" -> "Notifications"
                            "widgets" -> "Widgets"
                            "backup" -> "Backup"
                            else -> "About"
                        },
                        color = palette.text,
                        style = LauncherTextStyles.settingsTitle,
                        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
                    )
                    Text(
                        text = when (selectedCategory) {
                            "home" -> "Layout and behavior of your home screen."
                            "dock" -> "Control the appearance and behavior of your dock."
                            "drawer" -> "Organize apps and control the app drawer."
                            "icons" -> "Choose icon packs, shapes and sizing."
                            "folders" -> "Folder options are available in Home screen and App drawer."
                            "gestures" -> "Control navigation gestures and shortcuts."
                            "notifications" -> "Manage notification badges and indicators."
                            "widgets" -> "Configure weather, wallpaper and permissions."
                            "backup" -> "Export and restore your Nomina settings."
                            else -> "Version information and feedback."
                        },
                        color = palette.dim,
                        style = LauncherTextStyles.settingsDescription,
                        modifier = Modifier.padding(start = 6.dp, bottom = 16.dp),
                    )
                    if (selectedCategory == "folders") {
                        ExpressiveActionRow(label = "Home folder options", description = "Folder columns and names") { selectedCategory = "home" }
                        ExpressiveActionRow(label = "App drawer folders", description = "Folder names and management") { selectedCategory = "drawer" }
                    }
                }
                Text(
                    text = "N O M I N A  /  SYSTEM",
                    color = palette.faint,
                    style = LauncherTextStyles.settingsEyebrow,
                    modifier = Modifier.padding(start = 6.dp, top = 18.dp, bottom = 2.dp),
                )
                Text(
                    text = stringResource(R.string.settings_title),
                    color = palette.text,
                    style = LauncherTextStyles.settingsTitle,
                    modifier = Modifier.padding(start = 6.dp, top = 0.dp, bottom = 6.dp),
                )

                if (selectedCategory == "home") {
                CollapsibleSection(
                    title = stringResource(R.string.settings_home),
                    icon = LauncherIcons.Home,
                    collapsed = "home" in s.collapsedSettingsSections,
                    onToggle = { viewModel.setSectionCollapsed("home", it) },
                }
                ) {
                    StepperRow(stringResource(R.string.settings_columns), s.homeColumns, SettingsRepository.MIN_COLUMNS, SettingsRepository.MAX_COLUMNS, viewModel::setHomeColumns)
                    StepperRow(stringResource(R.string.settings_rows), s.homeRows, SettingsRepository.MIN_ROWS, SettingsRepository.MAX_ROWS, viewModel::setHomeRows)
                    SwitchRow(stringResource(R.string.settings_show_labels), s.showHomeLabels, viewModel::setShowHomeLabels)
                    SwitchRow("Hide status bar", s.hideStatusBar, viewModel::setHideStatusBar)
                    SwitchRow(stringResource(R.string.settings_two_line_labels), s.twoLineHomeLabels, viewModel::setTwoLineHomeLabels)
                    SwitchRow(stringResource(R.string.settings_show_folder_names), s.showHomeFolderNames, viewModel::setShowHomeFolderNames)
                    StepperRow(
                        stringResource(R.string.settings_folder_columns), s.folderColumns,
                        SettingsRepository.MIN_FOLDER_COLUMNS, SettingsRepository.MAX_FOLDER_COLUMNS, viewModel::setFolderColumns,
                    )
                    SwitchRow(stringResource(R.string.settings_page_indicator), s.showPageIndicator, viewModel::setShowPageIndicator)
                    if (s.showPageIndicator) {
                        ExpressiveActionRow(
                            label = stringResource(R.string.settings_home_page_marker),
                            description = stringResource(
                                when (s.homePageMarker) {
                                    LauncherSettings.MARKER_HOUSE -> R.string.settings_home_page_marker_house
                                    else -> R.string.settings_home_page_marker_ring
                                },
                            ),
                        ) { showHomeMarkerPicker = true }
                    }
                    PageSwitchSpeedRow(selected = s.pageSwitchSpeed) { showPageSpeedPicker = true }
                    SwitchRow(
                        stringResource(R.string.settings_infinite_scroll),
                        stringResource(R.string.settings_infinite_scroll_desc),
                        s.infiniteScroll,
                        viewModel::setInfiniteScroll,
                    )
                    SwitchRow(stringResource(R.string.settings_lock_desktop), s.desktopLocked, viewModel::setDesktopLocked)
                    SwitchRow(stringResource(R.string.settings_widget_tonal), s.widgetTonalBackground, viewModel::setWidgetTonalBackground)
                    SwitchRow(
                        stringResource(R.string.settings_widget_inset),
                        stringResource(R.string.settings_widget_inset_desc),
                        s.widgetEdgesAlignWithIcons,
                        viewModel::setWidgetEdgesAlignWithIcons,
                    )
                    WeatherToggle(enabled = s.showWeather, onSetEnabled = viewModel::setShowWeather)
                    if (s.showWeather) {
                        WeatherAppRow(appLabel = allApps.firstOrNull { it.key == s.weatherAppKey }?.label) {
                            showWeatherAppPicker = true
                        }
                    }
                }

                if (selectedCategory == "dock") {
                CollapsibleSection(
                    title = stringResource(R.string.settings_dock),
                    icon = LauncherIcons.Dock,
                    collapsed = "dock" in s.collapsedSettingsSections,
                    onToggle = { viewModel.setSectionCollapsed("dock", it) },
                }
                ) {
                    SwitchRow(stringResource(R.string.settings_dock_show), s.dockEnabled, viewModel::setDockEnabled)
                    SwitchRow(stringResource(R.string.settings_dock_auto_hide), s.autoHideDock, viewModel::setAutoHideDock)
                    StepperRow(stringResource(R.string.settings_dock_icons), s.dockColumns, 3, 10, viewModel::setDockColumns)
                    Text(
                        text = stringResource(R.string.settings_dock_icons_desc),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
                        style = LauncherTextStyles.settingsSupportingText,
                        color = palette.dim,
                    )
                    SwitchRow(stringResource(R.string.settings_show_labels), s.showDockLabels, viewModel::setShowDockLabels)
                    DockBackgroundRow(s.dockBackgroundOpacity, viewModel::setDockBackgroundOpacity)
                    DockBottomSpacingRow(s.dockBottomSpacing, viewModel::setDockBottomSpacing)
                    DockPreview(opacity = s.dockBackgroundOpacity, icon = previewIcon)
                }

                if (selectedCategory == "drawer") {
                CollapsibleSection(
                    title = stringResource(R.string.settings_drawer),
                    icon = LauncherIcons.AppsGrid,
                    collapsed = "drawer" in s.collapsedSettingsSections,
                    onToggle = { viewModel.setSectionCollapsed("drawer", it) },
                }
                ) {
                    StepperRow(stringResource(R.string.settings_columns), s.drawerColumns, 3, 7, viewModel::setDrawerColumns)
                    SwitchRow(stringResource(R.string.settings_show_labels), s.showDrawerLabels, viewModel::setShowDrawerLabels)
                    SwitchRow(stringResource(R.string.settings_two_line_labels), s.twoLineDrawerLabels, viewModel::setTwoLineDrawerLabels)
                    SwitchRow(stringResource(R.string.settings_show_folder_names), s.showDrawerFolderNames, viewModel::setShowDrawerFolderNames)
                    SwitchRow(stringResource(R.string.settings_drawer_search), s.showDrawerSearch, viewModel::setShowDrawerSearch)
                    SwitchRow("Show fast-scroll scrubber", s.showDrawerScrubber, viewModel::setShowDrawerScrubber)
                    SwitchRow(stringResource(R.string.settings_frequent_apps), s.showFrequentApps, viewModel::setShowFrequentApps)
                    SwitchRow(
                        stringResource(R.string.settings_hide_home_apps),
                        stringResource(R.string.settings_hide_home_apps_desc),
                        s.hideHomeAppsFromDrawer,
                        viewModel::setHideHomeAppsFromDrawer,
                    )
                    SwitchRow(stringResource(R.string.settings_drawer_open_top), s.drawerOpensAtTop, viewModel::setDrawerOpensAtTop)
                    ExpressiveActionRow(
                        label = stringResource(R.string.settings_hidden_apps),
                        description = pluralStringResource(R.plurals.settings_hidden_apps_desc, hiddenKeys.size, hiddenKeys.size),
                    ) { showHiddenManager = true }
                    ExpressiveActionRow(
                        label = stringResource(R.string.settings_drawer_folders),
                        description = pluralStringResource(
                            R.plurals.settings_drawer_folders_count, drawerFolders.orEmpty().size, drawerFolders.orEmpty().size,
                        ),
                    ) { showFolderManager = true }
                    val newFolderName = stringResource(R.string.drawer_folder_default)
                    val folderCreatedMsg = stringResource(R.string.settings_drawer_folder_created)
                    ExpressiveActionRow(
                        label = stringResource(R.string.settings_new_drawer_folder),
                        description = stringResource(R.string.settings_new_drawer_folder_desc),
                        trailingIcon = LauncherIcons.Add,
                    ) {
                        viewModel.createDrawerFolder(newFolderName)
                        android.widget.Toast.makeText(context, folderCreatedMsg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                    SwitchRow(
                        stringResource(R.string.settings_sort_drawer_folders),
                        stringResource(R.string.settings_sort_drawer_folders_desc),
                        s.sortDrawerFolders,
                        viewModel::setSortDrawerFolders,
                    )
                    SwitchRow(
                        stringResource(R.string.settings_mix_drawer_folders),
                        stringResource(R.string.settings_mix_drawer_folders_desc),
                        s.mixDrawerFolders,
                        viewModel::setMixDrawerFolders,
                    )
                }

                if (selectedCategory == "icons") {
                CollapsibleSection(
                    title = stringResource(R.string.settings_icons_typography),
                    icon = LauncherIcons.Appearance,
                    collapsed = "icons" in s.collapsedSettingsSections,
                    onToggle = { viewModel.setSectionCollapsed("icons", it) },
                }
                ) {
                    SettingsGroupLabel(stringResource(R.string.settings_app_icons_group))
                    val iconPackName = iconPacks.firstOrNull { it.packageName == s.iconPackPackage }?.label
                        ?: stringResource(R.string.settings_icon_pack_system)
                    ExpressiveActionRow(
                        label = stringResource(R.string.settings_icon_pack),
                        description = iconPackName,
                    ) { showIconPackPicker = true }
                    IconShapeRow(selected = s.iconShape, onPick = viewModel::setIconShape)
                    SwitchRow(
                        stringResource(R.string.settings_auto_resize_icons),
                        stringResource(R.string.settings_auto_resize_icons_desc),
                        s.autoResizeIcons,
                        viewModel::setAutoResizeIcons,
                    )
                    IconReshapeRow(
                        selected = s.reshapeLegacyIcons,
                        onPick = viewModel::setReshapeLegacyIcons,
                    )
                    ScaleSliderRow(
                        label = stringResource(R.string.settings_home_icon_size),
                        value = s.iconScale,
                        onValueChange = viewModel::setIconScale,
                        valueRange = 0.8f..1.3f,
                        valueText = percentText,
                    )
                    FollowHomeScaleRow(
                        label = stringResource(R.string.settings_dock_icon_size),
                        value = s.dockIconScale,
                        followsHome = s.dockIconScaleFollowsHome,
                        onFollowHome = { viewModel.setDockIconScale(null) },
                        onCustom = { viewModel.setDockIconScale(s.dockIconScale) },
                        onValueChange = viewModel::setDockIconScale,
                        valueRange = 0.8f..1.3f,
                        valueText = percentText,
                    )
                    FollowHomeScaleRow(
                        label = stringResource(R.string.settings_drawer_icon_size),
                        value = s.drawerIconScale,
                        followsHome = s.drawerIconScaleFollowsHome,
                        onFollowHome = { viewModel.setDrawerIconScale(null) },
                        onCustom = { viewModel.setDrawerIconScale(s.drawerIconScale) },
                        onValueChange = viewModel::setDrawerIconScale,
                        valueRange = 0.8f..1.3f,
                        valueText = percentText,
                    )

                    ScaleSliderRow(
                        label = stringResource(R.string.settings_home_label_size),
                        value = s.appLabelTextScale,
                        onValueChange = viewModel::setAppLabelTextScale,
                        valueRange = 0.8f..1.6f,
                        valueText = percentText,
                    )
                    FollowHomeScaleRow(
                        label = stringResource(R.string.settings_dock_label_size),
                        value = s.dockLabelTextScale,
                        followsHome = s.dockLabelTextScaleFollowsHome,
                        onFollowHome = { viewModel.setDockLabelTextScale(null) },
                        onCustom = { viewModel.setDockLabelTextScale(s.dockLabelTextScale) },
                        onValueChange = viewModel::setDockLabelTextScale,
                        valueRange = 0.8f..1.6f,
                        valueText = percentText,
                    )
                    FollowHomeScaleRow(
                        label = stringResource(R.string.settings_drawer_label_size),
                        value = s.drawerLabelTextScale,
                        followsHome = s.drawerLabelTextScaleFollowsHome,
                        onFollowHome = { viewModel.setDrawerLabelTextScale(null) },
                        onCustom = { viewModel.setDrawerLabelTextScale(s.drawerLabelTextScale) },
                        onValueChange = viewModel::setDrawerLabelTextScale,
                        valueRange = 0.8f..1.6f,
                        valueText = percentText,
                    )
                    LabelColorRow(
                        label = stringResource(R.string.settings_home_label_color),
                        selected = s.appLabelColor,
                        onPick = viewModel::setAppLabelColor,
                    )
                    FollowHomeColorRow(
                        label = stringResource(R.string.settings_dock_label_color),
                        followsHome = s.dockLabelColorFollowsHome,
                        selected = s.dockLabelColor ?: s.appLabelColor,
                        onFollowHome = { viewModel.setDockLabelColor(null) },
                        onCustom = { viewModel.setDockLabelColor(s.dockLabelColor ?: s.appLabelColor) },
                        onPick = viewModel::setDockLabelColor,
                    )
                    DrawerLabelColorModeRow(
                        mode = s.drawerLabelColorMode,
                        onPick = { mode ->
                            viewModel.setDrawerLabelColorMode(mode)
                            if (mode == LauncherSettings.LABEL_COLOR_CUSTOM && s.drawerLabelColor == null) {
                                viewModel.setDrawerLabelColor(s.appLabelColor)
                            }
                        },
                    )
                    if (s.drawerLabelColorMode == LauncherSettings.LABEL_COLOR_CUSTOM) {
                        LabelColorRow(
                            label = stringResource(R.string.settings_drawer_label_color),
                            selected = s.drawerLabelColor ?: s.appLabelColor,
                            onPick = viewModel::setDrawerLabelColor,
                        )
                    }
                }

                if (selectedCategory == "widgets") {
                CollapsibleSection(
                    title = stringResource(R.string.settings_wallpaper_background),
                    icon = LauncherIcons.Widgets,
                    collapsed = "wallpaper" in s.collapsedSettingsSections,
                    onToggle = { viewModel.setSectionCollapsed("wallpaper", it) },
                }
                ) {
                    DrawerBackdropRows(
                        settings = s,
                        wallpaper = drawerWallpaper,
                        blurAvailability = rememberBlurAvailability(),
                        actions = DrawerBackdropActions(
                            onColor = viewModel::setDrawerBackgroundColor,
                            onColorOpacity = viewModel::setDrawerBackgroundOpacity,
                            onPickPicture = viewModel::importDrawerWallpaper,
                            onClearPicture = viewModel::clearDrawerWallpaper,
                            onPictureDim = viewModel::setDrawerPictureDim,
                            onBlur = viewModel::setBlurBehindDrawer,
                            onBlurStrength = viewModel::setDrawerBlurStrength,
                        ),
                    )
                }

                if (selectedCategory == "gestures") {
                CollapsibleSection(
                    title = stringResource(R.string.settings_gestures_interaction),
                    icon = LauncherIcons.Gestures,
                    collapsed = "gestures" in s.collapsedSettingsSections,
                    onToggle = { viewModel.setSectionCollapsed("gestures", it) },
                }
                ) {
                    SwitchRow(stringResource(R.string.settings_swipe_up_drawer), s.swipeUpForDrawer, viewModel::setSwipeUp)
                    SwitchRow(stringResource(R.string.settings_swipe_down_notif), s.swipeDownForNotifications, viewModel::setSwipeDown)
                    SwitchRow(
                        stringResource(R.string.settings_folder_swipe_first_app),
                        stringResource(R.string.settings_folder_swipe_first_app_desc),
                        s.folderSwipeOpensFirstApp,
                        viewModel::setFolderSwipeOpensFirstApp,
                    )
                    DoubleTapLockToggle(enabled = s.doubleTapToLock, onSetEnabled = viewModel::setDoubleTapToLock)
                    // Read out in milliseconds of this phone's own long-press timeout times the setting, so the
                    // number is the real wait here and the default chip names it.
                    val systemLongPress = rememberSystemLongPressTimeout()
                    ScaleSliderRow(
                        label = stringResource(R.string.settings_long_press_delay),
                        description = stringResource(R.string.settings_long_press_delay_desc),
                        value = s.longPressScale,
                        onValueChange = viewModel::setLongPressScale,
                        valueRange = LongPressScale.MIN..LongPressScale.MAX,
                        step = 0.1f,
                        valueText = { resources.getString(R.string.settings_millis, (systemLongPress * it).roundToInt()) },
                    )
                }

                if (selectedCategory == "notifications") {
                CollapsibleSection(
                    title = stringResource(R.string.settings_notifications),
                    icon = LauncherIcons.Notifications,
                    collapsed = "notifications" in s.collapsedSettingsSections,
                    onToggle = { viewModel.setSectionCollapsed("notifications", it) },
                }
                ) {
                    SwitchRow(stringResource(R.string.settings_notif_dots), s.showNotificationDots, viewModel::setShowNotificationDots)
                    SwitchRow(stringResource(R.string.settings_notif_count), s.notificationDotCount, viewModel::setNotificationDotCount)
                    ScaleSliderRow(
                        label = stringResource(R.string.settings_notif_size),
                        value = s.notificationDotScale,
                        onValueChange = viewModel::setNotificationDotScale,
                        valueRange = 0.6f..1.8f,
                        valueText = percentText,
                    )
                    BadgePreview(
                        icon = previewIcon,
                        showDots = s.showNotificationDots,
                        showCount = s.notificationDotCount,
                        scale = s.notificationDotScale,
                    )
                }

                if (selectedCategory == "widgets") {
                CollapsibleSection(
                    title = stringResource(R.string.settings_permissions),
                    icon = LauncherIcons.Permissions,
                    collapsed = "permissions" in s.collapsedSettingsSections,
                    onToggle = { viewModel.setSectionCollapsed("permissions", it) },
                }
                ) {
                    ExpressiveActionRow(
                        label = stringResource(R.string.settings_set_default),
                        description = stringResource(
                            if (isDefaultLauncher) R.string.settings_default_current else R.string.settings_default_tap,
                        ),
                    ) {
                        runCatching { setDefaultLauncher.launch(DefaultLauncher.requestIntent(context)) }
                    }
                    ExpressiveActionRow(
                        label = stringResource(R.string.settings_notif_access),
                        description = stringResource(
                            if (notifAccessGranted) {
                                R.string.settings_notif_access_granted
                            } else {
                                R.string.settings_notif_access_not_granted
                            },
                        ),
                    ) {
                        openNotificationAccess(context)
                    }
                    PermissionRow(
                        label = stringResource(R.string.settings_permission_calendar),
                        permission = android.Manifest.permission.READ_CALENDAR,
                    )
                    PermissionRow(
                        label = stringResource(R.string.settings_permission_location),
                        permission = android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                }

                if (selectedCategory == "backup") {
                CollapsibleSection(
                    title = stringResource(R.string.settings_backup_restore),
                    icon = LauncherIcons.Backup,
                    collapsed = "backup" in s.collapsedSettingsSections,
                    onToggle = { viewModel.setSectionCollapsed("backup", it) },
                }
                ) {
                    ExpressiveActionRow(
                        label = stringResource(R.string.settings_backup),
                        description = "",
                    ) { showBackup = true }
                }

                if (selectedCategory == "about") {
                CollapsibleSection(
                    title = stringResource(R.string.settings_about),
                    icon = LauncherIcons.Info,
                    collapsed = "updates" in s.collapsedSettingsSections,
                    onToggle = { viewModel.setSectionCollapsed("updates", it) },
                }
                ) {
                    // Keep the installed app version with the other information about Nomina.
                    val installedVersion = remember(context) { installedVersionLabel(context) }
                    if (installedVersion != null) {
                        Text(
                            text = installedVersion,
                            color = palette.dim,
                            style = LauncherTextStyles.settingsDescription,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 28.dp, bottom = 4.dp),
                        )
                    }
                    ExpressiveActionRow(
                        label = stringResource(R.string.settings_updates_check),
                        description = stringResource(R.string.settings_updates_check_desc),
                    ) { openLink(context, RELEASES_URL) }
                    FeedbackRows(onOpen = { openLink(context, it) })

                }


                }
            }
        }

        if (showPageSpeedPicker) {
            ChoicePicker(
                title = R.string.settings_page_switch_speed,
                options = listOf(
                    LauncherSettings.SPEED_NORMAL to R.string.settings_page_speed_normal,
                    LauncherSettings.SPEED_FAST to R.string.settings_page_speed_fast,
                    LauncherSettings.SPEED_FASTEST to R.string.settings_page_speed_fastest,
                ),
                selected = s.pageSwitchSpeed,
                onPick = { speed ->
                    viewModel.setPageSwitchSpeed(speed)
                    showPageSpeedPicker = false
                },
                onDismiss = { showPageSpeedPicker = false },
            )
        }
        if (showWeatherAppPicker) {
            SingleAppPicker(
                apps = allApps,
                onPick = { key ->
                    viewModel.setWeatherAppKey(key)
                    showWeatherAppPicker = false
                },
                onDismiss = { showWeatherAppPicker = false },
            )
        }
        if (showIconPackPicker) {
            IconPackPicker(
                packs = iconPacks,
                selected = s.iconPackPackage,
                onPick = { pkg ->
                    viewModel.setIconPack(pkg)
                    showIconPackPicker = false
                },
                onDismiss = { showIconPackPicker = false },
            )
        }
        if (showHomeMarkerPicker) {
            ChoicePicker(
                title = R.string.settings_home_page_marker,
                options = listOf(
                    LauncherSettings.MARKER_RING to R.string.settings_home_page_marker_ring,
                    LauncherSettings.MARKER_HOUSE to R.string.settings_home_page_marker_house,
                ),
                selected = s.homePageMarker,
                onPick = {
                    viewModel.setHomePageMarker(it)
                    showHomeMarkerPicker = false
                },
                onDismiss = { showHomeMarkerPicker = false },
            )
        }
    }
}

/** Single-select picker for the home page marker. */
@Composable
private fun ChoicePicker(
    @androidx.annotation.StringRes title: Int,
    options: List<Pair<String, Int>>,
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val p = LocalExpressivePalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.surfaceHi,
        title = { Text(stringResource(title), color = p.text) },
        text = {
            Column {
                options.forEach { (value, labelRes) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(value) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(labelRes),
                            color = if (selected == value) p.text else p.dim,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_close), color = MaterialTheme.colorScheme.onSurface) }
        },
    )
}

/** Single-select icon-pack picker: a "System default" entry on top, then every installed pack. */
@Composable
private fun IconPackPicker(
    packs: List<com.insomdroid.nomina.data.IconPackInfo>,
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val p = LocalExpressivePalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.surfaceHi,
        title = { Text(stringResource(R.string.settings_icon_pack), color = p.text) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.settings_icon_pack_desc),
                    color = p.dim,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick("") }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_icon_pack_system),
                            color = if (selected.isBlank()) p.text else p.dim,
                            fontSize = 16.sp,
                        )
                        Text(
                            stringResource(R.string.settings_icon_pack_system_desc),
                            color = p.dim,
                            fontSize = 11.sp,
                        )
                    }
                    if (selected.isBlank()) Text("✓", color = p.text, fontSize = 18.sp)
                }
                packs.forEach { pack ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(pack.packageName) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                        ) {
                            Text(
                                pack.label,
                                color = if (selected == pack.packageName) p.text else p.dim,
                                fontSize = 16.sp,
                            )
                            Text(
                                pack.packageName,
                                color = p.dim,
                                fontSize = 11.sp,
                            )
                        }
                        if (selected == pack.packageName) Text("✓", color = p.text, fontSize = 18.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_close), color = MaterialTheme.colorScheme.onSurface) }
        },
    )
}

/** Settings > Home: how fast the home pager settles on a page (issue #18); the current choice sits under the label. */
@Composable
internal fun PageSwitchSpeedRow(selected: String, onClick: () -> Unit) {
    ExpressiveActionRow(
        label = stringResource(R.string.settings_page_switch_speed),
        description = stringResource(
            when (selected) {
                LauncherSettings.SPEED_FAST -> R.string.settings_page_speed_fast
                LauncherSettings.SPEED_FASTEST -> R.string.settings_page_speed_fastest
                else -> R.string.settings_page_speed_normal
            },
        ),
        onClick = onClick,
    )
}

/** Settings > Home: the app a tap on the smartspace weather opens, named under the label (None = plain text). */
@Composable
internal fun WeatherAppRow(appLabel: String?, onClick: () -> Unit) {
    ExpressiveActionRow(
        label = stringResource(R.string.settings_weather_app),
        description = appLabel ?: stringResource(R.string.settings_weather_app_none),
        onClick = onClick,
    )
}

/** Single-select weather-app picker: a "None" entry on top, then every app. */
@Composable
private fun SingleAppPicker(
    apps: List<AppItem>,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val p = LocalExpressivePalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.surfaceHi,
        title = { Text(stringResource(R.string.settings_weather_app_pick), color = p.text) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(null) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.settings_weather_app_none), color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                }
                apps.forEach { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(app.key) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(appItem = app, labelColor = p.text, showLabel = false, iconSize = 32.dp)
                        Text(app.label, modifier = Modifier.padding(start = 12.dp), color = p.text)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_close), color = MaterialTheme.colorScheme.onSurface) }
        },
    )
}

/** One runtime permission's live status; tap requests it. When Android suppresses the dialog
 *  (permanently denied), the only remaining path is the app's system settings page — opened on a
 *  dialog-less denial. A dialog the user merely backed out of answers the same (no grant, no
 *  rationale), so the first such answer counts as a dismissal; only a repeat opens the system
 *  page. Re-checked on resume so a grant/revoke made in the system settings shows the moment the
 *  user comes back (the v0.6.7 notif-access pattern). */
@Composable
internal fun PermissionRow(label: String, permission: String) {
    // Whether this row already asked once: the second dialog-less "no" is the permanent one.
    var asked by rememberSaveable(permission) { mutableStateOf(false) }
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED,
        )
    }
    LifecycleResumeEffect(permission) {
        granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        granted = result
        val activity = context.findActivity()
        if (!result && asked && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        ) {
            openAppDetailsSettings(context)
        }
        asked = true
    }
    ExpressiveActionRow(
        label = label,
        description = stringResource(
            if (granted) R.string.settings_permission_granted else R.string.settings_permission_not_granted,
        ),
    ) {
        if (!granted) launcher.launch(permission)
    }
}

/** Unwraps the Activity behind a Compose LocalContext (a ContextWrapper chain). */
private tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** The home module's lock service; referenced by name (feature modules don't depend on each other). */
private const val LOCK_SERVICE_CLASS = "com.insomdroid.nomina.feature.home.LockAccessibilityService"

private fun lockServiceEnabled(context: android.content.Context): Boolean {
    val enabled = android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    val target = android.content.ComponentName(context.packageName, LOCK_SERVICE_CLASS)
    return enabled.split(':').any { android.content.ComponentName.unflattenFromString(it) == target }
}

private const val RELEASES_URL = "https://github.com/insomdroid/Nomina-Launcher/releases"

internal fun openLink(context: android.content.Context, url: String) {
    val opened = runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.isSuccess
    if (!opened) {
        // No browser (or the handler is disabled): say so and show the address instead of a dead tap.
        android.widget.Toast.makeText(
            context,
            context.getString(R.string.settings_open_link_failed, url),
            android.widget.Toast.LENGTH_LONG,
        ).show()
    }
}

/** The app's system details page — where a permanently denied permission can still be granted. */
private fun openAppDetailsSettings(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** True when our notification listener has been granted notification access in the system settings. */
private fun isNotificationAccessGranted(context: android.content.Context): Boolean =
    runCatching {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }.getOrDefault(false)

/** Opens the system notification-access screen (deep-linked to this app when supported). */
private fun openNotificationAccess(context: android.content.Context) {
    val component = ComponentName(
        context.packageName,
        "com.insomdroid.nomina.notifications.NotificationDotListenerService",
    )
    val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
        .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val opened = runCatching { context.startActivity(detail) }.isSuccess
    if (!opened) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

/** Smartspace-weather toggle. Turns on regardless; when enabled it also asks for the coarse location
 *  the Open-Meteo query needs (the widget hides the weather until the permission is granted). */
@Composable
private fun WeatherToggle(enabled: Boolean, onSetEnabled: (Boolean) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* shows once granted */ }
    SwitchRow(stringResource(R.string.settings_show_weather), enabled) { wantOn ->
        onSetEnabled(wantOn)
        if (wantOn &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
}

/** Double-tap-to-lock toggle. Locking needs the lock accessibility service (used ONLY for
 *  performGlobalAction(LOCK_SCREEN)); enabling the switch opens the system accessibility settings
 *  when the service isn't allowed yet, and a grant row stays visible until it is. */
@Composable
private fun DoubleTapLockToggle(enabled: Boolean, onSetEnabled: (Boolean) -> Unit) {
    val context = LocalContext.current
    var serviceEnabled by remember { mutableStateOf(lockServiceEnabled(context)) }
    // Re-check when returning from the system accessibility settings.
    LifecycleResumeEffect(Unit) {
        serviceEnabled = lockServiceEnabled(context)
        onPauseOrDispose { }
    }
    fun openAccessibilitySettings() {
        runCatching {
            context.startActivity(
                android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
    SwitchRow(stringResource(R.string.settings_double_tap_lock), enabled) { wantOn ->
        onSetEnabled(wantOn)
        if (wantOn && !lockServiceEnabled(context)) openAccessibilitySettings()
    }
    if (enabled && !serviceEnabled) {
        ExpressiveActionRow(
            label = stringResource(R.string.settings_double_tap_lock_grant),
            description = stringResource(R.string.settings_double_tap_lock_grant_desc),
        ) { openAccessibilitySettings() }
    }
}

@Composable
private fun RowLabel(text: String, modifier: Modifier = Modifier, color: Color = LocalExpressivePalette.current.text) {
    Text(text, modifier = modifier, color = color, style = LauncherTextStyles.settingsRowLabel)
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) =
    SwitchRow(label, description = null, checked, onCheckedChange)

/** A setting switch; [enabled] false greys the label and the switch, the description then says why. */
@Composable
internal fun SwitchRow(
    label: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val p = LocalExpressivePalette.current
    val labelColor = if (enabled) p.text else p.dim
    ExpressiveCard {
        if (description == null) {
            RowLabel(label, Modifier.weight(1f), color = labelColor)
        } else {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                RowLabel(label, color = labelColor)
                Text(description, color = p.dim, style = LauncherTextStyles.settingsDescription)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = p.text,
                checkedThumbColor = p.bg,
                checkedBorderColor = p.text,
                uncheckedTrackColor = p.trackOff,
                uncheckedThumbColor = p.thumbOff,
                uncheckedBorderColor = p.trackOff,
            ),
        )
    }
}

@Composable
private fun StepperRow(label: String, value: Int, min: Int, max: Int, onValueChange: (Int) -> Unit) {
    val p = LocalExpressivePalette.current
    ExpressiveCard {
        RowLabel(label, Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StepButton(LauncherIcons.Remove, stringResource(R.string.settings_step_decrease)) {
                if (value > min) onValueChange(value - 1)
            }
            Text(
                text = "$value",
                modifier = Modifier.widthIn(min = 26.dp),
                textAlign = TextAlign.Center,
                color = p.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            StepButton(LauncherIcons.Add, stringResource(R.string.settings_step_increase)) {
                if (value < max) onValueChange(value + 1)
            }
        }
    }
}

@Composable
private fun StepButton(@DrawableRes icon: Int, contentDescription: String, onClick: () -> Unit) {
    val p = LocalExpressivePalette.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(p.btn)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** The dock background darkness as a percent of black, in 5 % steps with a tick of haptic feedback
 *  on each and a chip back to the default. */
@Composable
internal fun DockBackgroundRow(value: Float, onValueChange: (Float) -> Unit) {
    val resources = LocalResources.current
    ScaleSliderRow(
        label = stringResource(R.string.settings_dock_bg),
        value = value,
        onValueChange = onValueChange,
        valueRange = 0f..1f,
        valueText = { resources.getString(R.string.settings_percent, scalePercent(it)) },
        default = LauncherSettings.DEFAULT_DOCK_BACKGROUND,
    )
}

/** Room under the dock (issue #24): 0 to 48 dp in 4 dp steps felt as ticks, read in dp beside the label, with a
 *  chip back to none. On top of the navigation-bar inset, so the dock lifts to where the thumb reaches. */
@Composable
internal fun DockBottomSpacingRow(value: Int, onValueChange: (Int) -> Unit) {
    val resources = LocalResources.current
    ScaleSliderRow(
        label = stringResource(R.string.settings_dock_bottom_space),
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt()) },
        valueRange = 0f..LauncherSettings.MAX_DOCK_BOTTOM_SPACING.toFloat(),
        valueText = { resources.getString(R.string.settings_dp, it.roundToInt()) },
        step = DOCK_BOTTOM_SPACING_STEP,
        default = 0f,
    )
}

/** The dock-spacing slider's step in dp. */
private const val DOCK_BOTTOM_SPACING_STEP = 4f

/** The app drawer's background opacity (issue #21): the dock row's twin, solid by default. */
/** The app drawer's own icon size (80–130 %, a Telegram request): until the user moves it, it shows and follows
 *  the home icon size (see LauncherSettings.drawerIconScale). */
@Composable
internal fun DrawerIconSizeRow(value: Float, onValueChange: (Float) -> Unit) {
    val resources = LocalResources.current
    ScaleSliderRow(
        label = stringResource(R.string.settings_drawer_icon_size),
        value = value,
        onValueChange = onValueChange,
        valueRange = 0.8f..1.3f,
        valueText = { resources.getString(R.string.settings_percent, scalePercent(it)) },
    )
}

/** The opacity of the drawer's colour over the wallpaper (issue #21); shown only while a colour is in effect. */
@Composable
internal fun DrawerBackgroundRow(value: Float, onValueChange: (Float) -> Unit) {
    val resources = LocalResources.current
    ScaleSliderRow(
        label = stringResource(R.string.settings_drawer_bg),
        value = value,
        onValueChange = onValueChange,
        valueRange = 0f..1f,
        valueText = { resources.getString(R.string.settings_percent, scalePercent(it)) },
        default = LauncherSettings.DEFAULT_DRAWER_BACKGROUND,
    )
}

/** The wallpaper-blur switch, greyed with the reason where this device cannot blur behind its windows. With the blur
 *  on behind a solid drawer ([drawerOpaque], the opacity slider at 100 %) nothing of it can show, so the description
 *  then says to lower the opacity instead (the owner saw a black drawer and looked for the blur). */
@Composable
internal fun BlurBehindDrawerRow(
    availability: BlurAvailability,
    checked: Boolean,
    drawerOpaque: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SwitchRow(
        label = stringResource(R.string.settings_blur_behind_drawer),
        description = stringResource(
            when {
                availability == BlurAvailability.NEEDS_ANDROID_12 -> R.string.settings_blur_behind_drawer_needs_12
                availability == BlurAvailability.DEVICE_CANNOT -> R.string.settings_blur_behind_drawer_unavailable
                availability == BlurAvailability.PAUSED -> R.string.settings_blur_behind_drawer_paused
                checked && drawerOpaque -> R.string.settings_blur_behind_drawer_opaque
                else -> R.string.settings_blur_behind_drawer_desc
            },
        ),
        checked = checked,
        onCheckedChange = onCheckedChange,
        // A pause is the system's momentary state, not the device's: the setting stays the user's to change.
        enabled = availability == BlurAvailability.AVAILABLE || availability == BlurAvailability.PAUSED,
    )
}

/** How strong the wallpaper blur behind the open drawer is, 10–200 % of the 0.9.9 blur (a Telegram request). */
@Composable
internal fun DrawerBlurStrengthRow(value: Float, onValueChange: (Float) -> Unit) {
    val resources = LocalResources.current
    ScaleSliderRow(
        label = stringResource(R.string.settings_blur_strength),
        value = value,
        onValueChange = onValueChange,
        valueRange = SettingsRepository.MIN_DRAWER_BLUR_STRENGTH..SettingsRepository.MAX_DRAWER_BLUR_STRENGTH,
        valueText = { resources.getString(R.string.settings_percent, scalePercent(it)) },
    )
}

/** How much the user's own picture behind the drawer is darkened for legibility, 0–100 % black; none out of the box. */
@Composable
internal fun DrawerPictureDimRow(value: Float, onValueChange: (Float) -> Unit) {
    val resources = LocalResources.current
    ScaleSliderRow(
        label = stringResource(R.string.settings_drawer_picture_dim),
        value = value,
        onValueChange = onValueChange,
        valueRange = 0f..1f,
        valueText = { resources.getString(R.string.settings_percent, scalePercent(it)) },
        default = LauncherSettings.DEFAULT_DRAWER_PICTURE_DIM,
    )
}

/** The callbacks of [DrawerBackdropRows], one per setting they change. */
internal class DrawerBackdropActions(
    val onColor: (Int?) -> Unit = {},
    val onColorOpacity: (Float) -> Unit = {},
    val onPickPicture: (Uri) -> Unit = {},
    val onClearPicture: () -> Unit = {},
    val onPictureDim: (Float) -> Unit = {},
    val onBlur: (Boolean) -> Unit = {},
    val onBlurStrength: (Float) -> Unit = {},
)

/**
 * What the app drawer paints over what is behind it (the owner's 1.0.0 notes), top to bottom as the layers stack: the
 * colour strip ("Transparent" first, then the theme and the swatches) and the colour's opacity while a colour is in
 * effect; the user's own picture, which turns the colour off (the strip locks and says why) and takes a dim of its own
 * instead; then the wallpaper blur, whose "the drawer is solid" hint follows what the drawer really paints
 * ([drawerSurface]).
 */
@Composable
internal fun DrawerBackdropRows(
    settings: LauncherSettings,
    wallpaper: DrawerWallpaper?,
    blurAvailability: BlurAvailability,
    actions: DrawerBackdropActions,
) {
    val hasPicture = wallpaper != null
    val surface = drawerSurface(settings.drawerBackgroundColor, settings.drawerBackgroundOpacity, hasPicture, settings.drawerPictureDim)
    DrawerColorRow(selected = settings.drawerBackgroundColor, locked = hasPicture, onPick = actions.onColor)
    if (!hasPicture && settings.drawerBackgroundColor != LauncherSettings.DRAWER_COLOR_TRANSPARENT) {
        DrawerBackgroundRow(settings.drawerBackgroundOpacity, actions.onColorOpacity)
    }
    DrawerWallpaperRow(wallpaper = wallpaper, onPick = actions.onPickPicture, onClear = actions.onClearPicture)
    if (hasPicture) DrawerPictureDimRow(settings.drawerPictureDim, actions.onPictureDim)
    // Greyed with the reason where the system cannot blur behind the window (Android 11, a Fairphone 6 report),
    // instead of a switch that silently does nothing -- except with the user's own picture, which is blurred inside
    // the window on any Android 12 (forOwnPicture).
    val blur = if (hasPicture) blurAvailability.forOwnPicture() else blurAvailability
    BlurBehindDrawerRow(
        availability = blur,
        checked = settings.blurBehindDrawer,
        drawerOpaque = surface.opacity >= 1f,
        onCheckedChange = actions.onBlur,
    )
    if (settings.blurBehindDrawer && blur == BlurAvailability.AVAILABLE) {
        DrawerBlurStrengthRow(settings.drawerBlurStrength, actions.onBlurStrength)
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    val p = LocalExpressivePalette.current
    Surface(
        color = p.surfaceHi,
        shape = RoundedCornerShape(ExpressiveRadius.card),
        shadowElevation = p.shadow,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).border(0.5.dp, p.trackOff.copy(alpha = 0.7f), RoundedCornerShape(ExpressiveRadius.card)).border(0.5.dp, p.trackOff.copy(alpha = 0.7f), RoundedCornerShape(ExpressiveRadius.card)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            RowLabel(label)
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = p.text,
                    activeTrackColor = p.text,
                    inactiveTrackColor = p.trackOff,
                ),
            )
        }
    }
}

@Composable
private fun SettingsGroupLabel(text: String) {
    val p = LocalExpressivePalette.current
    Text(
        text = text.uppercase(),
        color = p.dim,
        style = LauncherTextStyles.settingsGroupLabel,
        modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 8.dp),
    )
}

@Composable
private fun FollowHomeScaleRow(
    label: String,
    value: Float,
    followsHome: Boolean,
    onFollowHome: () -> Unit,
    onCustom: () -> Unit,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: (Float) -> String,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        FollowHomeChoiceCard(
            label = label,
            followsHome = followsHome,
            onFollowHome = onFollowHome,
            onCustom = onCustom,
        )
        if (!followsHome) {
            ScaleSliderRow(
                label = label,
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                valueText = valueText,
            )
        }
    }
}

@Composable
private fun FollowHomeColorRow(
    label: String,
    followsHome: Boolean,
    selected: Int,
    onFollowHome: () -> Unit,
    onCustom: () -> Unit,
    onPick: (Int) -> Unit,
) {
    FollowHomeChoiceCard(
        label = label,
        followsHome = followsHome,
        onFollowHome = onFollowHome,
        onCustom = onCustom,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    ) {
        if (!followsHome) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(top = 14.dp).horizontalScroll(rememberScrollState()),
            ) {
                COLOR_SWATCHES.forEach { color ->
                    ColorSwatch(argb = color, selected = color == selected, onClick = { onPick(color) })
                }
            }
        }
    }
}

/** Shared surface and choice controls for settings that can follow the Home value or be customized. */
@Composable
private fun FollowHomeChoiceCard(
    label: String,
    followsHome: Boolean,
    onFollowHome: () -> Unit,
    onCustom: () -> Unit,
    modifier: Modifier = Modifier,
    customContent: @Composable () -> Unit = {},
) {
    val p = LocalExpressivePalette.current
    Surface(
        color = p.surfaceHi,
        shape = RoundedCornerShape(ExpressiveRadius.card),
        shadowElevation = p.shadow,
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = ExpressiveSpacing.lg, vertical = 14.dp)) {
            RowLabel(label)
            Row(
                horizontalArrangement = Arrangement.spacedBy(ExpressiveSpacing.xs),
                modifier = Modifier.padding(top = 10.dp),
            ) {
                FilterChip(
                    selected = followsHome,
                    onClick = onFollowHome,
                    label = { Text(stringResource(R.string.settings_follow_home)) },
                )
                FilterChip(
                    selected = !followsHome,
                    onClick = onCustom,
                    label = { Text(stringResource(R.string.settings_custom)) },
                )
            }
            customContent()
        }
    }
}

@Composable
private fun DrawerLabelColorModeRow(mode: String, onPick: (String) -> Unit) {
    val p = LocalExpressivePalette.current
    Surface(
        color = p.surfaceHi,
        shape = RoundedCornerShape(ExpressiveRadius.card),
        shadowElevation = p.shadow,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            RowLabel(stringResource(R.string.settings_drawer_label_color))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()),
            ) {
                FilterChip(
                    selected = mode == LauncherSettings.LABEL_COLOR_FOLLOW_HOME,
                    onClick = { onPick(LauncherSettings.LABEL_COLOR_FOLLOW_HOME) },
                    label = { Text(stringResource(R.string.settings_follow_home)) },
                )
                FilterChip(
                    selected = mode == LauncherSettings.LABEL_COLOR_AUTOMATIC,
                    onClick = { onPick(LauncherSettings.LABEL_COLOR_AUTOMATIC) },
                    label = { Text(stringResource(R.string.settings_color_automatic)) },
                )
                FilterChip(
                    selected = mode == LauncherSettings.LABEL_COLOR_CUSTOM,
                    onClick = { onPick(LauncherSettings.LABEL_COLOR_CUSTOM) },
                    label = { Text(stringResource(R.string.settings_custom)) },
                )
            }
        }
    }
}

/** Row of preset label-color swatches for the home-surface app labels (home/dock/folders). The
 *  selected swatch gets an accent ring; every swatch has a faint outline so white reads on the card. */
@Composable
private fun LabelColorRow(label: String, selected: Int, onPick: (Int) -> Unit) {
    val p = LocalExpressivePalette.current
    Surface(
        color = p.surfaceHi,
        shape = RoundedCornerShape(ExpressiveRadius.card),
        shadowElevation = p.shadow,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).border(0.5.dp, p.trackOff.copy(alpha = 0.7f), RoundedCornerShape(ExpressiveRadius.card)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            RowLabel(label)
            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                COLOR_SWATCHES.forEach { c -> ColorSwatch(argb = c, selected = c == selected, onClick = { onPick(c) }) }
            }
        }
    }
}

/** The colours offered for app labels and the drawer's background: white, black, grey and six Material hues. */
internal val COLOR_SWATCHES: List<Int> = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFFBDBDBD, 0xFFE53935,
).map { it.toInt() }

/** A test tag per swatch, by its colour. */
internal fun colorSwatchTag(argb: Int): String = "color-swatch-%06X".format(argb and 0xFFFFFF)

/** One round colour swatch; the chosen one wears the accent ring. Disabled ([enabled] false) it fades and takes no taps. */
@Composable
private fun ColorSwatch(argb: Int, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val p = LocalExpressivePalette.current
    val name = "#%06X".format(argb and 0xFFFFFF)
    val fade = if (enabled) 1f else 0.4f
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(argb).copy(alpha = fade))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = (if (selected) p.text else p.faint).copy(alpha = fade),
                shape = CircleShape,
            )
            .testTag(colorSwatchTag(argb))
            .semantics { contentDescription = name; this.selected = selected }
            .clickable(enabled = enabled) { onClick() },
    )
}

internal fun iconShapeChipTag(key: String): String = "icon-shape-$key"

private fun iconShapeNameRes(key: String): Int = when (key) {
    LauncherSettings.ICON_SHAPE_CIRCLE -> R.string.settings_icon_shape_circle
    LauncherSettings.ICON_SHAPE_SQUIRCLE -> R.string.settings_icon_shape_squircle
    LauncherSettings.ICON_SHAPE_ROUNDED_SQUARE -> R.string.settings_icon_shape_rounded_square
    LauncherSettings.ICON_SHAPE_SQUARE -> R.string.settings_icon_shape_square
    LauncherSettings.ICON_SHAPE_TEARDROP -> R.string.settings_icon_shape_teardrop
    LauncherSettings.ICON_SHAPE_HEXAGON -> R.string.settings_icon_shape_hexagon
    LauncherSettings.ICON_SHAPE_OCTAGON -> R.string.settings_icon_shape_octagon
    LauncherSettings.ICON_SHAPE_FLOWER -> R.string.settings_icon_shape_flower
    LauncherSettings.ICON_SHAPE_CAT -> R.string.settings_icon_shape_cat
    else -> R.string.settings_icon_shape_system
}

/** Nova-style policy for applying the selected mask to legacy icons. */
@Composable
internal fun IconReshapeRow(selected: String, onPick: (String) -> Unit) {
    val p = LocalExpressivePalette.current
    Surface(
        color = p.surfaceHi,
        shape = RoundedCornerShape(ExpressiveRadius.card),
        shadowElevation = p.shadow,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            .border(0.5.dp, p.trackOff.copy(alpha = 0.7f), RoundedCornerShape(ExpressiveRadius.card)),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            RowLabel(stringResource(R.string.settings_reshape_legacy_icons))
            Text(
                text = stringResource(R.string.settings_reshape_legacy_icons_desc),
                color = p.dim,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                LauncherSettings.RESHAPE_LEGACY_MODES.forEach { mode ->
                    val label = when (mode) {
                        LauncherSettings.RESHAPE_ALWAYS -> R.string.settings_reshape_always
                        LauncherSettings.RESHAPE_UNTHEMED -> R.string.settings_reshape_unthemed
                        else -> R.string.settings_reshape_off
                    }
                    FilterChip(
                        selected = selected == mode,
                        onClick = { onPick(mode) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
        }
    }
}

/** All available masks, including the actual adaptive-icon mask supplied by Android. */
@Composable
internal fun IconShapeRow(selected: String, onPick: (String) -> Unit) {
    val p = LocalExpressivePalette.current
    Surface(
        color = p.surfaceHi,
        shape = RoundedCornerShape(ExpressiveRadius.card),
        shadowElevation = p.shadow,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            .border(0.5.dp, p.trackOff.copy(alpha = 0.7f), RoundedCornerShape(ExpressiveRadius.card)),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            RowLabel(stringResource(R.string.settings_icon_shape))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                LauncherSettings.ICON_SHAPES.forEach { key ->
                    FilterChip(
                        selected = selected == key,
                        onClick = { onPick(key) },
                        label = { Text(stringResource(iconShapeNameRes(key))) },
                        modifier = Modifier.testTag(iconShapeChipTag(key)),
                    )
                }
            }
        }
    }
}

/** The test tags of the drawer wallpaper row's preview and buttons. */
internal const val DRAWER_WALLPAPER_PREVIEW_TAG = "drawer-wallpaper-preview"
internal const val DRAWER_WALLPAPER_PICK_TAG = "drawer-wallpaper-pick"
internal const val DRAWER_WALLPAPER_REMOVE_TAG = "drawer-wallpaper-remove"

/** The user's own picture behind the app drawer (a Telegram request after 0.9.9): a preview of the one set,
 *  a button to pick one from the gallery (the system photo picker, no permission), one to remove it. The
 *  picture stays on this phone: it is not in a backup. */
@Composable
internal fun DrawerWallpaperRow(wallpaper: DrawerWallpaper?, onPick: (Uri) -> Unit, onClear: () -> Unit) {
    val p = LocalExpressivePalette.current
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPick(uri)
    }
    Surface(
        color = p.surfaceHi,
        shape = RoundedCornerShape(ExpressiveRadius.card),
        shadowElevation = p.shadow,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).border(0.5.dp, p.trackOff.copy(alpha = 0.7f), RoundedCornerShape(ExpressiveRadius.card)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            RowLabel(stringResource(R.string.settings_drawer_wallpaper))
            Text(stringResource(R.string.settings_drawer_wallpaper_desc), color = p.dim, style = LauncherTextStyles.settingsDescription)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (wallpaper != null) {
                    DrawerWallpaperPicture(
                        file = wallpaper.file,
                        blur = 0.dp,
                        contentDescription = stringResource(R.string.settings_drawer_wallpaper),
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).testTag(DRAWER_WALLPAPER_PREVIEW_TAG),
                    )
                } else {
                    Text(stringResource(R.string.settings_drawer_wallpaper_none), color = p.dim, fontSize = 14.sp)
                }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { runCatching { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } },
                    modifier = Modifier.testTag(DRAWER_WALLPAPER_PICK_TAG),
                ) {
                    Text(stringResource(R.string.settings_drawer_wallpaper_pick), color = MaterialTheme.colorScheme.onSurface)
                }
                if (wallpaper != null) {
                    TextButton(onClick = onClear, modifier = Modifier.testTag(DRAWER_WALLPAPER_REMOVE_TAG)) {
                        Text(stringResource(R.string.settings_drawer_wallpaper_remove), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

/** The app drawer's background colour (a Telegram request after 0.9.9): "Transparent" for no colour layer at all, the
 *  theme's own surface, or one of the label swatches; the drawer's text turns black or white to match by itself. While
 *  the user's own picture is set ([locked]) the colour is off: the strip keeps showing the choice but takes no taps and
 *  says why, so the colour is back as it was once the picture goes. */
@Composable
internal fun DrawerColorRow(selected: Int?, locked: Boolean = false, onPick: (Int?) -> Unit) {
    val p = LocalExpressivePalette.current
    Surface(
        color = p.surfaceHi,
        shape = RoundedCornerShape(ExpressiveRadius.card),
        shadowElevation = p.shadow,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).border(0.5.dp, p.trackOff.copy(alpha = 0.7f), RoundedCornerShape(ExpressiveRadius.card)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            RowLabel(stringResource(R.string.settings_drawer_color))
            if (locked) {
                Text(stringResource(R.string.settings_drawer_color_locked), color = p.dim, style = LauncherTextStyles.settingsDescription)
            }
            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                FilterChip(
                    selected = selected == LauncherSettings.DRAWER_COLOR_TRANSPARENT,
                    enabled = !locked,
                    onClick = { onPick(LauncherSettings.DRAWER_COLOR_TRANSPARENT) },
                    label = { Text(stringResource(R.string.settings_color_transparent)) },
                )
                FilterChip(
                    selected = selected == null,
                    enabled = !locked,
                    onClick = { onPick(null) },
                    label = { Text(stringResource(R.string.settings_color_theme)) },
                )
                COLOR_SWATCHES.forEach { c ->
                    ColorSwatch(argb = c, selected = c == selected, enabled = !locked, onClick = { onPick(c) })
                }
            }
        }
    }
}

/** The drawer's label colour row (the owner's 1.0.1 ask: the choice home's labels have): an "Automatic" chip for the
 *  black or white the drawer picks by its own look, then the label swatches. [selected] null = automatic. */
@Composable
internal fun DrawerLabelColorRow(selected: Int?, onPick: (Int?) -> Unit) {
    val p = LocalExpressivePalette.current
    Surface(
        color = p.surfaceHi,
        shape = RoundedCornerShape(ExpressiveRadius.card),
        shadowElevation = p.shadow,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).border(0.5.dp, p.trackOff.copy(alpha = 0.7f), RoundedCornerShape(ExpressiveRadius.card)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            RowLabel(stringResource(R.string.settings_label_color))
            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                FilterChip(
                    selected = selected == null,
                    onClick = { onPick(null) },
                    label = { Text(stringResource(R.string.settings_color_automatic)) },
                )
                COLOR_SWATCHES.forEach { c -> ColorSwatch(argb = c, selected = c == selected, onClick = { onPick(c) }) }
            }
        }
    }
}

/**
 * Full-screen manager listing every app with a switch — toggle to hide/show it in the app drawer.
 * (Apps can also be hidden via the drawer long-press, but only restored here.)
 */
@Composable
private fun HiddenAppsManager(
    apps: List<AppItem>,
    hiddenKeys: Set<String>,
    onSetHidden: (String, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalExpressivePalette.current
    BackHandler(onBack = onBack)
    Surface(modifier = modifier, color = p.bg) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Text(
                text = stringResource(R.string.settings_hidden_apps),
                color = p.text,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = apps, key = { it.key }) { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(
                            appItem = app,
                            labelColor = p.text,
                            showLabel = false,
                            iconSize = 40.dp,
                        )
                        Text(
                            text = app.label,
                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                            color = p.text,
                            fontSize = 16.sp,
                        )
                        Switch(
                            checked = app.key in hiddenKeys,
                            onCheckedChange = { onSetHidden(app.key, it) },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = p.text,
                                checkedThumbColor = p.bg,
                                checkedBorderColor = p.text,
                                uncheckedTrackColor = p.trackOff,
                                uncheckedThumbColor = p.thumbOff,
                                uncheckedBorderColor = p.trackOff,
                            ),
                        )
                    }
                }
            }
        }
    }
}

/** The launcher's own app icon, for use as the settings preview sample. */
@Composable
private fun rememberLauncherIconBitmap(): ImageBitmap? {
    val context = LocalContext.current
    return remember {
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val size = 144
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        }.getOrNull()
    }
}

/** Live preview of the dock at the chosen background opacity, over a neutral backdrop so the dark
 *  dock scrim stays visible on both light and dark themes. */
@Composable
private fun DockPreview(opacity: Float, icon: ImageBitmap?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(Color(0xFFB9C3D4), RoundedCornerShape(22.dp))
            .padding(8.dp),
    ) {
        Surface(
            color = Color.Black.copy(alpha = opacity),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                repeat(4) {
                    if (icon != null) {
                        Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(36.dp))
                    } else {
                        Box(Modifier.size(36.dp).background(Color.White.copy(alpha = 0.6f), CircleShape))
                    }
                }
            }
        }
    }
}

/** Live preview of the notification badge on a sample icon (the launcher's own). */
@Composable
private fun BadgePreview(icon: ImageBitmap?, showDots: Boolean, showCount: Boolean, scale: Float) {
    Box(
        modifier = Modifier.padding(bottom = 8.dp),
        contentAlignment = Alignment.TopEnd,
    ) {
        if (icon != null) {
            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(56.dp))
        } else {
            Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(ExpressiveRadius.card)))
        }
        if (showDots) {
            NotificationBadge(count = 5, showCount = showCount, scale = scale)
        }
    }
}
