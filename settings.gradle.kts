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

rootProject.name = "Maro"

include(":app")

// Shared modules
include(":core:domain")
include(":core:data")
include(":core:database")
include(":core:presentation")
include(":core:design-system")

// Feature modules: each feature is split into domain / data / presentation layers.
// Features never depend on each other; cross-feature navigation goes through callbacks wired in :app.
listOf("auth", "chatlist", "chat", "profile").forEach { feature ->
    include(":feature:$feature:domain")
    include(":feature:$feature:data")
    include(":feature:$feature:presentation")
}
