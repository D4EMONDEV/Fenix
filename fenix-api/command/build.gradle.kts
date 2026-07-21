plugins {
    id("fenix.api-module")
    // Library mode: Minecraft on the compile classpath, nothing else.
    id("fr.d4emon.fenix.dev")
}

description = "Brigadier commands, without the parts nobody enjoys."

fenix {
    library = true
}

dependencies {
    // Commands are announced through the event bus, like everything else.
    api(project(":fenix-api-event"))

    // The mixin that opens the command tree.
    compileOnly(libs.mixin)
}
