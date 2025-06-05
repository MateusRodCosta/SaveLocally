pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.autonomousapps.build-health") version "2.17.0"

    id("org.jetbrains.kotlin.jvm") version "2.1.21" apply false

    id("com.android.application") version "8.10.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.21" apply false
}

rootProject.name = "Save Locally"
include(":app")
