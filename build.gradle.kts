// build.gradle.kts (Project Level)

// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Most configurations are now moved to settings.gradle.kts or module-level build.gradle.kts files.

// It's good practice to include a clean task:
tasks.register("clean", Delete::class) {
    delete(project.layout.buildDirectory)
}
