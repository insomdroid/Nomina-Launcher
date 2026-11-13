plugins {
    id("arkikeskus.android.library")
    id("arkikeskus.android.hilt")
}

android {
    namespace = "com.insomdroid.nomina.data"
}

ksp {
    // Export Room schemas here so every version bump leaves a JSON the next Migration can diff against.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
