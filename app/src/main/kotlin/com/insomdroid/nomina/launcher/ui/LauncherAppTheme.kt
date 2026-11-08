package com.insomdroid.nomina.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.insomdroid.nomina.data.SettingsRepository
import com.insomdroid.nomina.designsystem.theme.LauncherTheme
import com.insomdroid.nomina.model.LauncherSettings
import com.insomdroid.nomina.ui.component.LocalAutoResizeIcons
import com.insomdroid.nomina.ui.component.LocalIconShape
import com.insomdroid.nomina.ui.LocalLauncherFontFamily
import javax.inject.Inject

/** The settings the theme itself depends on: the user's font (Settings > Icons > Font). */
@HiltViewModel
class ThemeViewModel @Inject constructor(settingsRepository: SettingsRepository) : ViewModel() {
    val appearance: StateFlow<LauncherSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LauncherSettings())
}

/** [LauncherTheme] with the user's font, for every activity of the launcher. */
@Composable
fun LauncherAppTheme(content: @Composable () -> Unit) {
    val appearance by hiltViewModel<ThemeViewModel>().appearance.collectAsStateWithLifecycle()
    val font = remember(appearance.fontFamily, appearance.customFontPath) {
        launcherFontFamily(appearance.fontFamily, appearance.customFontPath)
    }
    CompositionLocalProvider(
        LocalLauncherFontFamily provides font,
        LocalIconShape provides appearance.iconShape,
        LocalAutoResizeIcons provides appearance.autoResizeIcons,
    ) {
        LauncherTheme(fontFamily = font, content = content)
    }
}
