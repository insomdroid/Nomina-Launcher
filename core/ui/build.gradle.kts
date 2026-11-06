import java.util.Base64
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    id("arkikeskus.android.library")
    id("arkikeskus.android.compose")
}

abstract class GenerateLauncherFonts : DefaultTask() {
    @get:InputDirectory
    abstract val fontSourceDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val output = outputDirectory.get().asFile
        output.deleteRecursively()
        val fontDirectory = output.resolve("font")
        fontDirectory.mkdirs()

        fontSourceDirectory.get().asFile.listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.endsWith(".ttf.b64") }
            .forEach { source ->
                val resourceName = source.name
                    .removeSuffix(".ttf.b64")
                    .lowercase()
                    .replace('-', '_')
                val bytes = Base64.getDecoder().decode(
                    source.readText().filterNot { it.isWhitespace() },
                )
                fontDirectory.resolve("$resourceName.ttf").writeBytes(bytes)
            }
    }
}

val generateLauncherFonts = tasks.register<GenerateLauncherFonts>("generateLauncherFonts") {
    fontSourceDirectory.set(layout.projectDirectory.dir("src/main/font-source"))
    outputDirectory.set(layout.buildDirectory.dir("generated/res/launcherFonts"))
}

android {
    namespace = "com.insomdroid.nomina.ui"
    testOptions.unitTests.isIncludeAndroidResources = true
}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.sources.res?.addGeneratedSourceDirectory(generateLauncherFonts) { task ->
            task.outputDirectory
        }
    }
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
