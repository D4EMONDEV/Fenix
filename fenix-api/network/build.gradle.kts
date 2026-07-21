plugins {
    id("fenix.api-module")
    // Library mode: Minecraft on the compile classpath, nothing else.
    id("fr.d4emon.fenix.dev")
}

description = "Typed custom payloads between client and server, in both directions."

fenix {
    library = true
}

dependencies {
    // The mixins that carry payloads in and out.
    compileOnly(libs.mixin)
}
