plugins {
    id("fenix.publish-conventions")
}

description = "Every Fenix API module in a single dependency."

dependencies {
    // Discovered rather than listed: adding a new `fenix-api-*` module needs no
    // edit here, and no module can be forgotten.
    rootProject.subprojects
        .filter { it.name.startsWith("fenix-api-") }
        .forEach { api(project(it.path)) }
}
