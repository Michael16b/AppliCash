// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.roborazzi) apply false
}

/**
 * Aggregates snapshot verification across all feature modules (RG4/CA3).
 * Equivalent of verifyPaparazziDebug using Roborazzi.
 * Usage: ./gradlew verifyPaparazziDebug
 */
val snapshotModules = listOf(
    ":core:ui",
    ":feature:login",
    ":feature:home",
    ":feature:expense",
    ":feature:profil",
    ":feature:splashscreen",
)

tasks.register("verifyPaparazziDebug") {
    group = "verification"
    description = "Verifies that all Roborazzi snapshots match the reference images."
    dependsOn(snapshotModules.map { "$it:verifyRoborazziDebug" })
}

tasks.register("recordPaparazziDebug") {
    group = "verification"
    description = "Records / updates all Roborazzi reference images in /snapshots/."
    dependsOn(snapshotModules.map { "$it:recordRoborazziDebug" })
}
