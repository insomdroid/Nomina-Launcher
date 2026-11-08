package com.insomdroid.nomina.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.insomdroid.nomina.data.DrawerWallpaper
import com.insomdroid.nomina.data.DrawerWallpaperStore
import com.insomdroid.nomina.data.LauncherAppsSource
import com.insomdroid.nomina.data.NotificationBadgeRepository
import com.insomdroid.nomina.data.SettingsRepository
import com.insomdroid.nomina.model.DrawerSurface
import com.insomdroid.nomina.model.IconEpochs
import com.insomdroid.nomina.model.drawerSurface
import javax.inject.Inject

/** The app drawer's surface and wallpaper determine the native system-bar glyph contrast. */
data class StatusBarSettings(
    val ownBarShown: Boolean = false,
    val systemBarHidden: Boolean = false,
    val scrimOpacity: Float = 0f,
    /** What the drawer paints and how much of it shows: its lightness, weighted by the opacity, decides the glyphs. */
    val drawerSurface: DrawerSurface = DrawerSurface(tint = null, paint = null, opacity = 1f),
    /** The user's own picture behind the drawer, or null without one: with a picture it is what a see-through drawer
     *  lets through, in place of home and the wallpaper -- its lightness for the window's crop of it
     *  ([DrawerWallpaper.luminanceFor]). */
    val drawerPicture: DrawerWallpaper? = null,
)

/** The app-icon style (icon pack, themed, the icon-size multiplier), so the shell's floating drag icon
 *  matches the surfaces. */
data class IconStyle(val iconPack: String = "", val themed: Boolean = false, val iconScale: Float = 1f, val themeAll: Boolean = false, val iconShape: String = com.insomdroid.nomina.model.LauncherSettings.ICON_SHAPE_SYSTEM)

/**
 * Exposes just the icon-style settings to [LauncherShell] so the single floating drag icon — drawn
 * above the home and drawer, outside their own CompositionLocal scopes — renders with the selected
 * icon pack / themed icons instead of the default system icon. Also relays the icon re-fetch epochs
 * (see [com.insomdroid.nomina.model.IconEpochs]) that the shell provides to every icon surface.
 */
@HiltViewModel
class LauncherShellViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    launcherAppsSource: LauncherAppsSource,
    notificationBadgeRepository: NotificationBadgeRepository,
    drawerWallpaper: DrawerWallpaperStore,
) : ViewModel() {
    /** A heads-up notification's bar reveal is on: home blanks its own status bar for the window (see
     *  [NotificationBadgeRepository.headsUpActive]), so the bar is no dark backing for the glyphs then. */
    val headsUpActive: StateFlow<Boolean> = notificationBadgeRepository.headsUpActive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val iconStyle: StateFlow<IconStyle> = settingsRepository.settings
        .map { IconStyle(it.iconPackPackage, it.useThemedIcons, it.iconScale, it.themeAllIcons, it.iconShape) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IconStyle())

    val iconEpochs: StateFlow<IconEpochs> = launcherAppsSource.iconEpochs

    /** The drawer surface used to keep native system-bar glyphs readable (see SystemBarContrast). */
    val statusBars: StateFlow<StatusBarSettings> = settingsRepository.settings
        .combine(drawerWallpaper.current) { it, picture ->
            StatusBarSettings(
                drawerSurface = drawerSurface(it.drawerBackgroundColor, it.drawerBackgroundOpacity, picture != null, it.drawerPictureDim),
                drawerPicture = picture,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatusBarSettings())

    /** The long-press delay setting, applied once at the shell root for every press site under it. */
    val longPressScale: StateFlow<Float> = settingsRepository.settings
        .map { it.longPressScale }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1f)

    /** The wallpaper blurs behind the rising drawer (Settings > App drawer, opt-in), at the chosen strength. */
    val drawerBlur: StateFlow<DrawerBlur> = settingsRepository.settings
        .map { DrawerBlur(enabled = it.blurBehindDrawer, strength = it.drawerBlurStrength) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DrawerBlur())
}

/** The wallpaper blur behind the rising drawer: on or off, and its radius as a multiple of the 0.9.9 blur. */
data class DrawerBlur(val enabled: Boolean = false, val strength: Float = 1f)
