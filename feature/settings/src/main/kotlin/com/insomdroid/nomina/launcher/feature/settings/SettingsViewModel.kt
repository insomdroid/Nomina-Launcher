package com.insomdroid.nomina.feature.settings

import androidx.lifecycle.ViewModel
import com.insomdroid.nomina.data.DrawerWallpaperStore
import com.insomdroid.nomina.data.DrawerWallpaper
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.insomdroid.nomina.data.AppRepository
import com.insomdroid.nomina.data.CustomIconStore
import com.insomdroid.nomina.data.DrawerFolder
import com.insomdroid.nomina.data.HomeLayoutRepository
import com.insomdroid.nomina.data.IconPackInfo
import com.insomdroid.nomina.data.IconPackRepository
import com.insomdroid.nomina.data.SettingsRepository
import com.insomdroid.nomina.model.AppItem
import com.insomdroid.nomina.model.LauncherSettings
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val homeLayoutRepository: HomeLayoutRepository,
    private val iconPackRepository: IconPackRepository,
    private val customIcons: CustomIconStore,
    appRepository: AppRepository,
    private val drawerWallpaper: DrawerWallpaperStore,
) : ViewModel() {

    /** The user's own picture behind the app drawer, or null (a Telegram request after 0.9.9). */
    val drawerWallpaperPicture: StateFlow<DrawerWallpaper?> = drawerWallpaper.current

    /** Saves the picture at [uri] as the drawer's wallpaper. */
    fun importDrawerWallpaper(uri: Uri) = viewModelScope.launch { drawerWallpaper.import(uri) }

    fun clearDrawerWallpaper() = viewModelScope.launch { drawerWallpaper.clear() }

    val settings: StateFlow<LauncherSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherSettings(),
    )

    /** Installed icon packs (queried off-main once), for the picker. */
    val iconPacks: StateFlow<List<IconPackInfo>> =
        flow { emit(iconPackRepository.installedPacks()) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All launchable apps (sorted), for the hidden-apps manager list. */
    val apps: StateFlow<List<AppItem>> = appRepository.apps
        .map { list -> list.sortedBy { it.label.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Keys currently hidden from the drawer. */
    val hiddenKeys: StateFlow<Set<String>> = repository.hiddenApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun setAppHidden(key: String, hidden: Boolean) = update { repository.setAppHidden(key, hidden) }

    /** Creates a new empty folder in the app drawer (named [name]); it's filled from the drawer. */
    fun createDrawerFolder(name: String) = update { repository.createDrawerFolder(name) }

    /** The drawer's folders, for the Folders page; null until the store has answered, so a folder's
     *  page restored after process death is not closed on an empty list it never had. */
    val drawerFolders: StateFlow<List<DrawerFolder>?> = repository.drawerFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun renameDrawerFolder(id: Long, name: String) = update { repository.renameDrawerFolder(id, name) }

    /** Deletes the folder and the icon picked for it (see CustomIconStore.deleteDrawerFolder). */
    fun deleteDrawerFolder(id: Long) = update { customIcons.deleteDrawerFolder(id) }

    fun setAppInDrawerFolder(id: Long, key: String, member: Boolean) = update {
        if (member) repository.addAppsToDrawerFolder(id, listOf(key)) else repository.removeAppFromDrawerFolder(id, key)
    }

    fun setSwipeUp(value: Boolean) = update { repository.setSwipeUpForDrawer(value) }
    fun setSwipeDown(value: Boolean) = update { repository.setSwipeDownForNotifications(value) }
    fun setFolderSwipeOpensFirstApp(value: Boolean) = update { repository.setFolderSwipeOpensFirstApp(value) }
    fun setHideHomeAppsFromDrawer(value: Boolean) = update { repository.setHideHomeAppsFromDrawer(value) }
    fun setWidgetEdgesAlignWithIcons(value: Boolean) = update { repository.setWidgetEdgesAlignWithIcons(value) }
    fun setDockEnabled(value: Boolean) = update { repository.setDockEnabled(value) }
    fun setDockColumns(value: Int) = update { repository.setDockColumns(value) }
    fun setDrawerColumns(value: Int) = update { repository.setDrawerColumns(value) }
    fun setShowDrawerSearch(value: Boolean) = update { repository.setShowDrawerSearch(value) }
    fun setHideStatusBar(value: Boolean) = update { repository.setHideStatusBar(value) }
    fun setShowDrawerScrubber(value: Boolean) = update { repository.setShowDrawerScrubber(value) }
    fun setShowFrequentApps(value: Boolean) = update { repository.setShowFrequentApps(value) }
    fun setDrawerOpensAtTop(value: Boolean) = update { repository.setDrawerOpensAtTop(value) }
    fun setSortDrawerFolders(value: Boolean) = update { repository.setSortDrawerFolders(value) }
    fun setMixDrawerFolders(value: Boolean) = update { repository.setMixDrawerFolders(value) }
    fun setBlurBehindDrawer(value: Boolean) = update { repository.setBlurBehindDrawer(value) }
    fun setDrawerBlurStrength(value: Float) = update { repository.setDrawerBlurStrength(value) }
    fun setDrawerBackgroundColor(argb: Int?) = update { repository.setDrawerBackgroundColor(argb) }
    fun setDrawerLabelColor(argb: Int?) = update { repository.setDrawerLabelColor(argb) }
    fun setDrawerPictureDim(value: Float) = update { repository.setDrawerPictureDim(value) }
    fun setIconShape(shape: String) = update { repository.setIconShape(shape) }
    fun setAutoResizeIcons(value: Boolean) = update { repository.setAutoResizeIcons(value) }

    fun setThemeAllIcons(value: Boolean) = update { repository.setThemeAllIcons(value) }
    fun setFolderColumns(value: Int) = update { repository.setFolderColumns(value) }
    fun setSectionCollapsed(id: String, collapsed: Boolean) = update { repository.setSettingsSectionCollapsed(id, collapsed) }
    fun setHomeColumns(value: Int) = viewModelScope.launch {
        // Shrinking the grid can leave shortcuts at an out-of-range cellX; repack them back on-screen
        // before the new (smaller) column count reaches the home screen, so nothing draws off-grid.
        val s = repository.settings.first()
        if (value < s.homeColumns) homeLayoutRepository.reflow(value, s.homeRows)
        repository.setHomeColumns(value)
    }

    fun setHomeRows(value: Int) = viewModelScope.launch {
        // Same off-grid guard as the column stepper, on the vertical axis.
        val s = repository.settings.first()
        if (value < s.homeRows) homeLayoutRepository.reflow(s.homeColumns, value)
        repository.setHomeRows(value)
    }
    fun setShowDockLabels(value: Boolean) = update { repository.setShowDockLabels(value) }
    fun setShowHomeLabels(value: Boolean) = update { repository.setShowHomeLabels(value) }
    fun setShowDrawerLabels(value: Boolean) = update { repository.setShowDrawerLabels(value) }
    fun setShowHomeFolderNames(value: Boolean) = update { repository.setShowHomeFolderNames(value) }
    fun setShowDrawerFolderNames(value: Boolean) = update { repository.setShowDrawerFolderNames(value) }
    fun setDockBackgroundOpacity(value: Float) = update { repository.setDockBackgroundOpacity(value) }
    fun setDockBottomSpacing(value: Int) = update { repository.setDockBottomSpacing(value) }
    fun setDrawerBackgroundOpacity(value: Float) = update { repository.setDrawerBackgroundOpacity(value) }
    fun setShowPageIndicator(value: Boolean) = update { repository.setShowPageIndicator(value) }
    fun setHomePageMarker(value: String) = update { repository.setHomePageMarker(value) }
    fun setInfiniteScroll(value: Boolean) = update { repository.setInfiniteScroll(value) }
    fun setShowNotificationDots(value: Boolean) = update { repository.setShowNotificationDots(value) }
    fun setNotificationDotCount(value: Boolean) = update { repository.setNotificationDotCount(value) }
    fun setNotificationDotScale(value: Float) = update { repository.setNotificationDotScale(value) }
    fun setUseThemedIcons(value: Boolean) = update { repository.setUseThemedIcons(value) }
    fun setIconPack(pkg: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Parse the pack once before publishing it to every icon surface. The next batch of
            // Coil requests can then resolve icons from the warm repository cache.
            if (pkg.isNotBlank()) iconPackRepository.get(pkg)
            repository.setIconPackPackage(pkg)
        }
    }

    /** Sets (or clears, for null) the app a tap on the smartspace weather opens. */
    fun setWeatherAppKey(key: String?) = update { repository.setWeatherAppKey(key) }

    /** How fast the home pager settles on a page (normal / fast / fastest). */
    fun setPageSwitchSpeed(speed: String) = update { repository.setPageSwitchSpeed(speed) }

    fun setDesktopLocked(value: Boolean) = update { repository.setDesktopLocked(value) }
    fun setDoubleTapToLock(value: Boolean) = update { repository.setDoubleTapToLock(value) }
    fun setLongPressScale(value: Float) = update { repository.setLongPressScale(value) }

    fun setAppLabelTextScale(value: Float) = update { repository.setAppLabelTextScale(value) }
    fun setDockLabelTextScale(value: Float?) = update { repository.setDockLabelTextScale(value) }
    fun setDrawerLabelTextScale(value: Float?) = update { repository.setDrawerLabelTextScale(value) }
    fun setIconScale(value: Float) = update { repository.setIconScale(value) }
    fun setDockIconScale(value: Float?) = update { repository.setDockIconScale(value) }
    fun setDrawerIconScale(value: Float?) = update { repository.setDrawerIconScale(value) }
    fun setAppLabelColor(argb: Int) = update { repository.setAppLabelColor(argb) }
    fun setDockLabelColor(argb: Int?) = update { repository.setDockLabelColor(argb) }
    fun setDrawerLabelColorMode(mode: String) = update { repository.setDrawerLabelColorMode(mode) }
    fun setTwoLineHomeLabels(value: Boolean) = update { repository.setTwoLineHomeLabels(value) }
    fun setTwoLineDrawerLabels(value: Boolean) = update { repository.setTwoLineDrawerLabels(value) }
    fun setShowWeather(value: Boolean) = update { repository.setShowWeather(value) }
    fun setWidgetTonalBackground(value: Boolean) = update { repository.setWidgetTonalBackground(value) }
    fun setNotificationWidgetCountStyle(value: String) = update { repository.setNotificationWidgetCountStyle(value) }
    fun setPeoplePrivacy(value: String) = update { repository.setPeoplePrivacy(value) }
    fun setPeopleShowApps(value: Boolean) = update { repository.setPeopleShowApps(value) }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

