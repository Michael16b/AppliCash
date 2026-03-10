plugins {
    kotlin("jvm")
    alias(libs.plugins.ktlint)
}

dependencies {
    // The domain module must not have any Android dependencies.
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core.coroutines)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
}
