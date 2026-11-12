plugins {
    id("arkikeskus.android.feature")
}

android {
    namespace = "com.insomdroid.nomina.feature.appdrawer"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
}
