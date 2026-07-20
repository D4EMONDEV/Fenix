plugins {
    base
}

description = "Fenix — a modern Minecraft mod loader"

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
