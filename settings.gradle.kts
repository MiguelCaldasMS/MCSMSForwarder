// settings.gradle.kts

// Configure where Gradle finds the plugins themselves (the plugin binaries)
// This is still necessary even with version catalogs defining plugin *versions*.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Configure where Gradle finds the dependencies (libraries) your app modules use.
dependencyResolutionManagement {
    // Fail quickly if a module tries to declare its own repositories (enforces central config)
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        // Add other repositories if needed, e.g.:
        // maven("https://jitpack.io")
    }
}

rootProject.name = "MCSMSForwarder1" // From your original settings.gradle
include(":app")                  // From your original settings.gradle
// include(":another-module")   // If you add more modules later
