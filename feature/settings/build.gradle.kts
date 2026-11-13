plugins {
    id("arkikeskus.android.feature")
}

android {
    namespace = "com.insomdroid.nomina.feature.settings"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:launcher"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":feature:backup"))
    implementation(libs.androidx.core.ktx)
}
