/*
 * Standalone build.
 *
 * The plugin has to exist before the main build can apply it, which is exactly
 * what a composite build is for: `settings.gradle.kts` at the repository root
 * pulls this in via `includeBuild`.
 */

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "fenix-gradle-plugin"
