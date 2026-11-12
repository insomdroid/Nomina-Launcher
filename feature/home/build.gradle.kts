plugins {
    id("arkikeskus.android.feature")
}

android {
    namespace = "com.insomdroid.nomina.feature.home"
    defaultConfig {
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:launcher"))
}
