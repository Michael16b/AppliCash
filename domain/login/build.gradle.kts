plugins {
    kotlin("jvm")
}

dependencies {
    // The domain module must not have any Android dependencies.
    implementation("io.insert-koin:koin-core:4.1.1")
}
