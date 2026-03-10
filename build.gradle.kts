// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.roborazzi) apply false
}

// Configure le répertoire de sortie des snapshots Roborazzi pour tous les sous-projets (CA4/RG3)
subprojects {
    tasks.withType<Test>().configureEach {
        systemProperty(
            "roborazzi.output.dir",
            "${rootProject.projectDir}/snapshots/${project.name}"
        )
    }
}

