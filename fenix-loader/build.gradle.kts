plugins {
    id("fenix.publish-conventions")
}

description = "The Fenix loader: classloading, mod discovery, dependency resolution and mixin bootstrap."

dependencies {
    // The loader instantiates mods through the core contracts, so anything that
    // embeds the loader sees them too.
    api(project(":fenix-api-core"))

    // Transformation pipeline.
    implementation(libs.mixin)
    implementation(libs.bundles.asm)

    // Both already sit on the vanilla classpath. Keeping them compile-only means
    // Fenix never ships a second copy in its version manifest.
    compileOnly(libs.gson)
    compileOnly(libs.slf4j.api)

    // Tests run without the game, so what the game provides has to be supplied.
    testImplementation(libs.gson)
}

// The loader is not a mod, so it has no fenix.mod.json; it learns its own
// version from this file instead.
tasks.processResources {
    filesMatching("fenix-loader.properties") {
        expand(mapOf("version" to project.version.toString()))
    }
}
