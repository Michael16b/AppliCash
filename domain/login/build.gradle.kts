plugins {
    kotlin("jvm")
}

dependencies {
    // Le module de domaine ne doit avoir aucune dépendance Android.
    implementation("io.insert-koin:koin-core:4.1.1")
}
