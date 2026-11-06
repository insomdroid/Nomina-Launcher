import java.util.Base64

plugins {
    id("arkikeskus.android.library")
    id("arkikeskus.android.compose")
}

android {
    namespace = "com.insomdroid.nomina.ui"
    testOptions.unitTests.isIncludeAndroidResources = true
}

val generatedLauncherFontResources = layout.buildDirectory.dir("generated/res/launcherFonts")
val generateLauncherFonts = tasks.register("generateLauncherFonts") {
    val fontSourceDirectory = layout.projectDirectory.dir("src/main/font-source")
    inputs.dir(fontSourceDirectory)
    outputs.dir(generatedLauncherFontResources)
    doLast {
        val outputDirectory = generatedLauncherFontResources.get().asFile
        outputDirectory.deleteRecursively()
        outputDirectory.mkdirs()
        val fontDirectory = outputDirectory.resolve("font")
        fontDirectory.mkdirs()
        fontSourceDirectory.asFile.listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.endsWith(".ttf.b64") }
            .forEach { source ->
                val resourceName = source.name
                    .removeSuffix(".ttf.b64")
                    .lowercase()
                    .replace('-', '_')
                val fontBytes = Base64.getDecoder().decode(source.readText().filterNot { it.isWhitespace() })
                fontDirectory.resolve("$resourceName.ttf").writeBytes(fontBytes)
            }
    }
}

android.sourceSets.getByName("main").res.srcDir(generatedLauncherFontResources)
tasks.named("preBuild").configure {
    dependsOn(generateLauncherFonts)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.coil.compose)
    implementation(libs.androidx.activity.compose)

    // JVM unit tests (pure icon/label sizing); Robolectric for the shortcut popup and LauncherApps calls.
    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.robolectric)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
