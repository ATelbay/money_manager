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
include(":core:auth")
include(":core:database")
include(":core:firestore")
include(":core:datastore")
include(":core:ui")
include(":core:common")
include(":core:model")
include(":core:ai")
include(":core:remoteconfig")
include(":core:parser")
include(":core:crypto")

// Domain
include(":domain:auth")
include(":domain:transactions")
include(":domain:categories")
include(":domain:accounts")
include(":domain:statistics")
include(":domain:import")
include(":domain:exchangerate")
include(":domain:sync")

// Data
include(":data:auth")
include(":data:sync")
include(":data:transactions")
include(":data:categories")
include(":data:accounts")
include(":data:exchangerate")

// Presentation
include(":presentation:auth")
include(":presentation:transactions")
include(":presentation:categories")
include(":presentation:accounts")
include(":presentation:statistics")
include(":presentation:import")
include(":presentation:settings")
include(":presentation:onboarding")
