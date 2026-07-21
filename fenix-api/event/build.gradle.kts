plugins {
    id("fenix.api-module")
    // In library mode: Minecraft on the compile classpath, nothing else. The
    // event contexts carry game types, so this module needs the game — but it
    // is the API, so it cannot depend on the API.
    id("fr.d4emon.fenix.dev")
}

description = "Event bus plus the game, client and server lifecycle events built on it."

fenix {
    library = true
}

dependencies {
    // The mixins that fire these events. Provided by the loader at run time.
    compileOnly(libs.mixin)
}
