plugins {
    id("arkikeskus.android.library")
    id("arkikeskus.android.compose")
}

android {
    namespace = "com.insomdroid.nomina.designsystem"
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
}
