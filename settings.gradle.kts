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

// Core
include(":core:database")
include(":core:datastore")
include(":core:ui")
include(":core:common")
include(":core:model")
include(":core:ai")
include(":core:remoteconfig")
include(":core:parser")

// Domain
include(":domain:transactions")
include(":domain:categories")
include(":domain:accounts")
include(":domain:statistics")
include(":domain:import")

// Data
include(":data:transactions")
include(":data:categories")
include(":data:accounts")

// Presentation
include(":presentation:transactions")
include(":presentation:categories")
include(":presentation:accounts")
include(":presentation:statistics")
include(":presentation:import")
include(":presentation:settings")
include(":presentation:onboarding")
