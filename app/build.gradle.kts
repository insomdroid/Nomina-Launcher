import java.io.FileInputStream
import java.util.Properties

plugins {
    id("arkikeskus.android.application")
    id("arkikeskus.android.compose")
    id("arkikeskus.android.hilt")
    alias(libs.plugins.baselineprofile)
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) FileInputStream(keystorePropsFile).use { load(it) }
}

android {
    namespace = "com.insomdroid.nomina"
    testOptions.unitTests.isIncludeAndroidResources = true

    defaultConfig {
        manifestPlaceholders["appLabel"] = "@string/app_name"
        applicationId = "com.insomdroid.nomina"
        versionCode = 69
        versionName = "1.0.2"

        // Nomina ships an English-only UI. Drop unused translated resources pulled in by
        // AndroidX/Compose libraries while keeping the app's own locale configuration intact.
        resourceConfigurations += listOf("en")
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    // Let AGP strip native debug symbols instead of shipping them in the APK.
    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appLabel"] = "Nomina debug"
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo {
                include = false
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

androidComponents {
    finalizeDsl { extension ->
        for (name in listOf("nonMinifiedRelease", "benchmarkRelease")) {
            extension.buildTypes.findByName(name)?.apply {
                applicationIdSuffix = ".bench"
                manifestPlaceholders["appLabel"] = "Nomina bench"
            }
        }
    }
}

baselineProfile {
    automaticGenerationDuringBuild = false
    saveInSrc = true
    mergeIntoMain = true
    dexLayoutOptimization = true
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:launcher"))
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":feature:home"))
    implementation(project(":feature:appdrawer"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:backup"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))
    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.datastore.preferences)
    testImplementation(libs.kotlinx.coroutines.test)
}
