plugins {
    id("arkikeskus.android.feature")
}

android {
    namespace = "com.insomdroid.nomina.feature.backup"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(libs.androidx.core.ktx)

}
