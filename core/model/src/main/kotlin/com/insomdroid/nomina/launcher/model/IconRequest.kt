package com.insomdroid.nomina.model

/**
 * Coil model for rendering an app icon. Every visual input is part of this model/cache key so
 * changing the icon pack, legacy reshape policy, mask, or auto-resize setting refreshes icons.
 */
data class IconRequest(
    val app: AppItem,
    val iconPack: String = "",
    val epoch: Int = 0,
    val reshapeLegacyIcons: String = LauncherSettings.RESHAPE_UNTHEMED,
    val iconShape: String = LauncherSettings.ICON_SHAPE_SYSTEM,
    val autoResize: Boolean = true,
)
