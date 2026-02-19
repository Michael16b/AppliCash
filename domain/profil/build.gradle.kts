plugins {
    kotlin("jvm")
}

dependencies {
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core.coroutines)
}

