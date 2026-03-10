plugins {
    kotlin("jvm")
    alias(libs.plugins.ktlint)
}

dependencies {
    // Pure Kotlin — no Android dependencies allowed in the domain layer.
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
}
