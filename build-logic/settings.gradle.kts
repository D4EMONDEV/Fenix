dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    // Reuse the main build's catalogue so versions are declared exactly once.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
