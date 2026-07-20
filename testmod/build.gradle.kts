plugins {
    id("fenix.java-conventions")
    // TODO: switch to `id("fr.d4emon.fenix.dev")` once the plugin can supply the
    // Minecraft dependency and the runClient task. Until then this is a plain
    // Java module so the build stays green.
}

description = "In-repo mod used to exercise the loader by hand while developing it."

dependencies {
    compileOnly(project(":fenix-api"))
    annotationProcessor(project(":fenix-processor"))
}
