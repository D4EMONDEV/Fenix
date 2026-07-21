/*
 * Fenix — a modern Minecraft mod loader.
 *
 * This repository is a monorepo: the loader, the API modules, the build
 * tooling and the samples live here and are versioned together.
 */

pluginManagement {
    // Convention plugins used to build Fenix itself. Never published.
    includeBuild("build-logic")

    // The public Gradle plugin mod authors apply (`fr.d4emon.fenix.dev`).
    // Included as a composite build so the samples in this repository
    // consume the very plugin we ship, without a publish round-trip.
    includeBuild("fenix-gradle-plugin")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    // PREFER_SETTINGS, not FAIL_ON_PROJECT_REPOS: the dev plugin adds its own
    // repositories (for external mods that have none in settings), and these
    // settings repos simply take precedence in-repo rather than erroring.
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }        // sponge-mixin
        maven("https://libraries.minecraft.net/") { name = "MinecraftLibraries" }
    }
}

rootProject.name = "fenix"

// ---------------------------------------------------------------------------
// Loader & tooling
// ---------------------------------------------------------------------------
include(":fenix-loader")     // classloading, mod discovery, mixin bootstrap
include(":fenix-processor")  // annotation processor -> compile-time mod index
include(":fenix-installer")  // writes a Fenix profile into .minecraft
include(":ember")            // asset & data generation

// ---------------------------------------------------------------------------
// API — small independent modules
//
// A mod depends only on the modules it actually uses. `fenix-api` is an
// aggregate that pulls every module in with a single dependency line.
// Directory `fenix-api/<name>` maps to project `:fenix-api-<name>` so that
// the published artifact id needs no extra configuration.
// ---------------------------------------------------------------------------
listOf("core", "event", "registry", "resource", "network", "command", "config").forEach { name ->
    val path = ":fenix-api-$name"
    include(path)
    project(path).projectDir = settingsDir.resolve("fenix-api/$name")
}

include(":fenix-api")
project(":fenix-api").projectDir = settingsDir.resolve("fenix-api/bundle")

// ---------------------------------------------------------------------------
// Testing & samples
// ---------------------------------------------------------------------------
include(":test-harness")
project(":test-harness").projectDir = settingsDir.resolve("testing/harness")

include(":conformance")
project(":conformance").projectDir = settingsDir.resolve("testing/conformance")

include(":demo-mod")
project(":demo-mod").projectDir = settingsDir.resolve("testing/demo-mod")

include(":testmod")

include(":example-mod")
project(":example-mod").projectDir = settingsDir.resolve("examples/example-mod")
