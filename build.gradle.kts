// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.roborazzi) apply false
}

// Include shared Gradle test tasks and JaCoCo collection
apply(from = "gradle/test-tasks.gradle.kts")
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

// Safe aggregate task used by CI to run a quick test suite. We capture the
// activation flag at configuration time to avoid accessing `project` during
// task execution (which breaks the configuration cache).
val fastTestsRunTests: Boolean = (project.findProperty("fastTestsRunTests") as? String) == "true"

if (tasks.findByName("fastTests") == null) {
    tasks.register("fastTests") {
        group = "verification"
        description = "Aggregate quick tests for CI. By default this is a no-op."

        if (fastTestsRunTests) {
            doLast {
                println("fastTests: configured to run unit tests.")
            }
        } else {
            doLast {
                println("fastTests: no-op (unit tests disabled).")
            }
        }
    }
}

// Wire the task dependencies after project evaluation only if the opt-in flag is set.
gradle.projectsEvaluated {
    if (fastTestsRunTests) {
        tasks.named("fastTests").configure {
            val testTasks = subprojects.flatMap { sp ->
                sp.tasks.matching { it.name.endsWith("UnitTest") || it.name.startsWith("test") }.toList()
            }
            if (testTasks.isNotEmpty()) dependsOn(testTasks)
        }
    }
}

