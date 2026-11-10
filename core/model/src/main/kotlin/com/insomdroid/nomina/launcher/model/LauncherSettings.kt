package com.insomdroid.nomina.model

/** User-configurable launcher settings (persisted in DataStore). */
data class LauncherSettings(
    val dockEnabled: Boolean = true,
    val dockColumns: Int = 4,
    val dockExpanded: Boolean = true,
    /** Automatically hide the dock after five seconds without interaction. */
    val autoHideDock: Boolean = false,
    val homeColumns: Int = 4,
    /** Home-grid row count — more rows = tighter icon rows + more free cells for widgets. */
    val homeRows: Int = 6,
    val drawerColumns: Int = 4,
    val showDrawerSearch: Boolean = true,
    val hideStatusBar: Boolean = false,
    val showDrawerScrubber: Boolean = true,
    val swipeUpForDrawer: Boolean = true,
    val swipeDownForNotifications: Boolean = true,
    /** Issue #17 (opt-in, Nova-style): a swipe up that starts on a HOME folder icon opens the folder's first app
     *  (in the folder's own order) instead of the drawer. Off, the swipe drives the drawer as from anywhere on
     *  home. Home only: dock folders and the drawer keep their gestures. */
    val folderSwipeOpensFirstApp: Boolean = false,
    val showDockLabels: Boolean = false,
    val showHomeLabels: Boolean = true,
    val showDrawerLabels: Boolean = true,
    /** The name under a home folder's tile (issue #17); off keeps the tile alone. Labels off hide it too. */
    val showHomeFolderNames: Boolean = true,
    /** The name under an app-drawer folder's tile; off keeps the tile alone. Labels off hide it too. */
    val showDrawerFolderNames: Boolean = true,
    val dockBackgroundOpacity: Float = DEFAULT_DOCK_BACKGROUND,
    /** Extra room under the dock in dp (issue #24), on top of the navigation-bar inset, so the thumb reaches it
     *  on a tall phone; 0 keeps the dock where it has always been, at most [MAX_DOCK_BOTTOM_SPACING]. */
    val dockBottomSpacing: Int = 0,
    /** Opacity of the app drawer's background (0 = the wallpaper shows through, 1 = solid); issue #21. */
    val drawerBackgroundOpacity: Float = DEFAULT_DRAWER_BACKGROUND,
    /** ARGB colour of the app drawer's background, null for the theme's own surface (a Telegram request after 0.9.9),
     *  or [DRAWER_COLOR_TRANSPARENT] for no colour layer at all. The drawer's text takes black or white by what it
     *  really looks like ([drawerSurface]). Off, and locked in Settings, while the user's own picture is set. */
    val drawerBackgroundColor: Int? = null,
    /** How much the user's own picture behind the drawer is darkened for legibility (0 = the picture as it is, 1 =
     *  black), in place of the colour and its opacity while the picture is set; the owner's 1.0.0 notes. */
    val drawerPictureDim: Float = DEFAULT_DRAWER_PICTURE_DIM,
    val showPageIndicator: Boolean = true,
    /** How the page dots mark the home page: a ring around its dot ([MARKER_RING]) or a house glyph
     *  in place of the dot ([MARKER_HOUSE]). */
    val homePageMarker: String = MARKER_RING,
    /** How fast the home pager settles on a page (its snap and every programmatic page change):
     *  [SPEED_NORMAL], [SPEED_FAST] or [SPEED_FASTEST] (issue #18). */
    val pageSwitchSpeed: String = SPEED_NORMAL,
    /** Mask used when reshaping legacy app icons. */
    val iconShape: String = ICON_SHAPE_SYSTEM,
    /** Nova-style policy for applying the selected mask to legacy icons. */
    val reshapeLegacyIcons: String = RESHAPE_OFF,
    /** Normalizes transparent icon padding so legacy icons have a more consistent visual size. */
    val autoResizeIcons: Boolean = false,
    /** Swiping past the last home page continues to the first one, and back from the first to the
     *  last. Off keeps the bounce back from the end. */
    val infiniteScroll: Boolean = true,
    /** "Not now" on the set-as-default card: it stays hidden on this device (Settings still offers
     *  the default-launcher row). Device-local, never in a backup. */
    val defaultReminderDismissed: Boolean = false,
    val showNotificationDots: Boolean = true,
    /** When notification dots are on: true shows the count (Nova-style), false shows a plain dot. */
    val notificationDotCount: Boolean = true,
    /** Size multiplier for the notification dot/badge (1.0 = default). */
    val notificationDotScale: Float = 1.0f,
    /** Package name of the selected third-party icon pack (empty = system default). Mapped apps
     *  use the pack drawable; unmapped apps keep their original full-colour icon. */
    val iconPackPackage: String = "",
    /** App key opened by a tap on the smartspace weather line (Settings > Home); blank = plain text. */
    val weatherAppKey: String = "",
    /** Lock the desktop layout: when true, home + dock items can't be moved, removed, or added. */
    val desktopLocked: Boolean = false,
    /** Show a "most used" row (top apps by decayed launch frequency) above the drawer's app list. */
    val showFrequentApps: Boolean = false,
    /** Reopen the app drawer scrolled back to the top (Pixel-style) rather than remembering the last
     *  scroll position — keeps the "most used" row and A–Z start visible on every open. */
    val drawerOpensAtTop: Boolean = true,
    /** List the app drawer's folders A–Z (case aside) instead of in the user's own order. */
    val sortDrawerFolders: Boolean = false,
    /** Seat the app drawer's folders among the apps by name (issue #22) instead of leading the grid. */
    val mixDrawerFolders: Boolean = false,
    /** Icons per row in an open folder, on the home screen and in the app drawer (a Telegram request). */
    val folderColumns: Int = 4,
    /** The settings sections folded under their titles, by id (a Telegram request); empty = all open. */
    val collapsedSettingsSections: Set<String> = emptySet(),
    /** Blur the wallpaper behind the window as the app drawer rises (a Telegram request, Android 12+). */
    val blurBehindDrawer: Boolean = false,
    /** Multiplier on the drawer blur's radius (1.0 = the 0.9.9 blur, 0.1–2.0; a Telegram request). */
    val drawerBlurStrength: Float = 1.0f,
    /** Apps placed on the home screen (its pages, its folders, the dock) leave the drawer's list; the search
     *  still finds them (a Telegram request after 0.9.7, opt-in). */
    val hideHomeAppsFromDrawer: Boolean = false,
    /** Size multiplier for app icon labels across home/dock/drawer/folders (1.0 = the default 11sp). */
    /** Home label size is the base value; Dock and Drawer can follow it or use their own value. */
    val appLabelTextScale: Float = 1.0f,
    /** Home icon size is the base value; Dock and Drawer can follow it or use their own value. */
    val iconScale: Float = 1.0f,
    val dockIconScale: Float = 1.0f,
    val dockIconScaleFollowsHome: Boolean = true,
    val drawerIconScale: Float = 1.0f,
    val drawerIconScaleFollowsHome: Boolean = true,
    val dockLabelTextScale: Float = 1.0f,
    val dockLabelTextScaleFollowsHome: Boolean = true,
    val drawerLabelTextScale: Float = 1.0f,
    val drawerLabelTextScaleFollowsHome: Boolean = true,
    /** Multiplier on the system long-press timeout before an icon's menu opens or it can be moved
     *  (1.0 = the system's own delay, 0.5–2.0). */
    val longPressScale: Float = 1.0f,
    /** Let home-screen labels (apps, folders, shortcuts) wrap onto a second line. Off keeps the
     *  single line the home grid has always had; the dock stays single-line either way. */
    val twoLineHomeLabels: Boolean = false,
    /** Let app-drawer labels (apps, folders, the "most used" row) wrap onto a second line — the
     *  drawer's long-standing look, so this defaults on. */
    val twoLineDrawerLabels: Boolean = true,
    /** Home label colour is the base; Dock and Drawer can follow it or use their own colour. */
    val appLabelColor: Int = 0xFFFFFFFF.toInt(),
    val dockLabelColor: Int? = null,
    val dockLabelColorFollowsHome: Boolean = true,
    val drawerLabelColor: Int? = null,
    val drawerLabelColorMode: String = LABEL_COLOR_FOLLOW_HOME,
    /** Show the current weather (Open-Meteo; needs a location permission) in the smartspace widget. */
    val showWeather: Boolean = true,
    /** Count indicator of the built-in notifications widget: [COUNT_NUMBER], [COUNT_DOT] or [COUNT_NONE]. */
    /** Use theme surfaces for built-in widgets; false keeps the translucent wallpaper style. */
    val widgetTonalBackground: Boolean = false,
    /** Widgets keep the same side margin inside their cells as an app icon has inside its cell, instead of
     *  filling the span edge to edge (a Telegram request after 0.9.7, opt-in; Launcher3's widget padding). */
    val widgetEdgesAlignWithIcons: Boolean = false,
    val notificationWidgetCountStyle: String = COUNT_NUMBER,
    /** Double-tap on empty home-screen space locks the screen (needs the lock accessibility service). */
    val doubleTapToLock: Boolean = false,
    /** The page the launcher opens on and the HOME button returns to (0 = the leftmost). */
    val homePage: Int = 0,
    /** Pages the user added explicitly (0 = none): the home keeps at least this many pages even
     *  when some are empty. Pages that hold an icon or widget exist regardless. */
    val homePageCount: Int = 0,
) {
    companion object {
        const val COUNT_NUMBER = "number"
        const val COUNT_DOT = "dot"
        const val COUNT_NONE = "none"

        const val MARKER_RING = "ring"
        const val MARKER_HOUSE = "house"

        const val SPEED_NORMAL = "normal"
        const val SPEED_FAST = "fast"
        const val SPEED_FASTEST = "fastest"

        const val LABEL_COLOR_FOLLOW_HOME = "follow_home"
        const val LABEL_COLOR_AUTOMATIC = "automatic"
        const val LABEL_COLOR_CUSTOM = "custom"

        const val RESHAPE_ALWAYS = "always"
        const val RESHAPE_UNTHEMED = "unthemed"
        const val RESHAPE_OFF = "off"
        val RESHAPE_LEGACY_MODES = listOf(RESHAPE_ALWAYS, RESHAPE_UNTHEMED, RESHAPE_OFF)

        const val ICON_SHAPE_SYSTEM = "system"
        const val ICON_SHAPE_CIRCLE = "circle"
        const val ICON_SHAPE_SQUIRCLE = "squircle"
        const val ICON_SHAPE_ROUNDED_SQUARE = "rounded_square"
        const val ICON_SHAPE_SQUARE = "square"
        const val ICON_SHAPE_TEARDROP = "teardrop"
        const val ICON_SHAPE_HEXAGON = "hexagon"
        const val ICON_SHAPE_OCTAGON = "octagon"
        const val ICON_SHAPE_FLOWER = "flower"
        const val ICON_SHAPE_CAT = "cat"
        val ICON_SHAPES = listOf(
            ICON_SHAPE_SYSTEM, ICON_SHAPE_CIRCLE, ICON_SHAPE_SQUIRCLE,
            ICON_SHAPE_ROUNDED_SQUARE, ICON_SHAPE_SQUARE, ICON_SHAPE_TEARDROP,
            ICON_SHAPE_HEXAGON, ICON_SHAPE_OCTAGON, ICON_SHAPE_FLOWER, ICON_SHAPE_CAT,
        )


        /** [dockBackgroundOpacity] out of the box: transparent, letting the wallpaper show through. */
        const val DEFAULT_DOCK_BACKGROUND = 0f

        /** The most [dockBottomSpacing] allows, in dp. */
        const val MAX_DOCK_BOTTOM_SPACING = 48

        /** [drawerBackgroundOpacity] out of the box: solid, the drawer as it always was. */
        const val DEFAULT_DRAWER_BACKGROUND = 1f

        /** [drawerBackgroundColor]'s "Transparent" choice: no colour layer, only the (blurred) wallpaper behind the
         *  drawer. The ARGB of a fully transparent black, which no swatch is. */
        const val DRAWER_COLOR_TRANSPARENT = 0

        /** [drawerPictureDim] out of the box: none, only the picture shows. */
        const val DEFAULT_DRAWER_PICTURE_DIM = 0f
    }
}
