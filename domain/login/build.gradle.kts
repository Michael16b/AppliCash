plugins {
    kotlin("jvm")
}

dependencies {
    // The domain module must not have any Android dependencies.
    implementation(libs.koin.core)
}
