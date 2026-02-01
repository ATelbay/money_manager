pluginManagement {
    includeBuild("build-logic")
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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "MoneyManager"

include(":app")
include(":core:database")
include(":core:datastore")
include(":core:ui")
include(":core:common")
include(":core:model")
include(":feature:onboarding")
include(":feature:transactions")
include(":feature:categories")
include(":feature:statistics")
include(":feature:accounts")
include(":feature:settings")
