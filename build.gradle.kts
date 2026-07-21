plugins {
    base
}

description = "Fenix — a modern Minecraft mod loader"

/**
 * Resolve the Fenix coordinates to the in-repo projects.
 *
 * The dev plugin refers to Fenix by Maven coordinate — the form an external mod
 * uses — but a regular multi-project build does not substitute those for its own
 * projects the way a composite build would. Without this, `example-mod` would
 * try to download `fr.d4emon.fenix:fenix-api` from a repository instead of
 * building the sibling project, and a fresh clone could not build until
 * `installFenix` had run. These rules make the coordinates mean the projects.
 */
subprojects {
    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("fr.d4emon.fenix:fenix-loader")).using(project(":fenix-loader"))
            substitute(module("fr.d4emon.fenix:fenix-api")).using(project(":fenix-api"))
            substitute(module("fr.d4emon.fenix:fenix-processor")).using(project(":fenix-processor"))
        }
    }
}

/**
 * Publishes every Fenix artifact to the local Maven repository so that a mod
 * project outside this repository can resolve them with `mavenLocal()`.
 *
 * Includes the Gradle plugin, which lives in a separate composite build.
 */
tasks.register("installFenix") {
    group = "fenix"
    description = "Publishes the loader, the API, Ember and the Gradle plugin to ~/.m2"

    dependsOn(provider {
        subprojects
            .filter { it.plugins.hasPlugin("maven-publish") }
            .map { "${it.path}:publishToMavenLocal" }
    })
    dependsOn(gradle.includedBuild("fenix-gradle-plugin").task(":publishToMavenLocal"))
}

/**
 * Assembles the whole Fenix Maven repository under `build/fenix-maven-repo`,
 * which the publish workflow deploys to GitHub Pages. This is the public
 * counterpart of `installFenix` — same artifacts, a shareable repository
 * instead of the developer's `~/.m2`.
 */
tasks.register("publishFenixRepo") {
    group = "fenix"
    description = "Builds the public Fenix Maven repository into build/fenix-maven-repo"

    dependsOn(provider {
        subprojects
            .filter { it.plugins.hasPlugin("maven-publish") }
            .map { "${it.path}:publishAllPublicationsToPagesRepository" }
    })
    dependsOn(gradle.includedBuild("fenix-gradle-plugin").task(":publishAllPublicationsToPagesRepository"))
}
