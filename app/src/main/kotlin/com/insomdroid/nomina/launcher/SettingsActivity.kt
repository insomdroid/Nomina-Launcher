package com.insomdroid.nomina

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import com.insomdroid.nomina.ui.LauncherAppTheme
import com.insomdroid.nomina.feature.settings.SettingsScreen

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LauncherAppTheme {
                SettingsScreen()
            }
        }
    }
}
