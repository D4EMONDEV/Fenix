plugins {
    id("fenix.java-conventions")
    // TODO: switch to `id("fr.d4emon.fenix.dev")` once the plugin exists — this
    // sample is what proves the published developer workflow actually works.
}

description = "Sample mod: the smallest thing a Fenix mod author has to write."

dependencies {
    compileOnly(project(":fenix-api"))
    annotationProcessor(project(":fenix-processor"))
}
