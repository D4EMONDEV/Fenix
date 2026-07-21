plugins {
    id("fenix.publish-conventions")
}

description = "Every Fenix API module in a single dependency."

dependencies {
    // Discovered rather than listed: adding a new `fenix-api-*` module needs no
    // edit here, and no module can be forgotten.
    //
    // Modules with no sources yet are skipped. They are reserved shapes for
    // phases not written, and shipping them would put empty jars in every
    // mod's run/mods for the loader to discover, resolve and log.
    rootProject.subprojects
        .filter { it.name.startsWith("fenix-api-") }
        .filter { it.file("src/main/java").walkTopDown().any { f -> f.extension == "java" } }
        .forEach { api(project(it.path)) }
}
