plugins {
    kotlin("jvm")
}

dependencies {
    // The domain module must not have any Android dependencies.
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core.coroutines)
}
