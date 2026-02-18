pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AppliCash"
include(":app")
include(":feature:home")
include(":data:login")
include(":data:expense")
include(":core:ui")
include(":core:security")
include(":feature:login")
include(":domain:login")
include(":feature:expense")
include(":feature:splashscreen")
include(":data:profil")
include(":domain:profil")
include(":feature:profil")
