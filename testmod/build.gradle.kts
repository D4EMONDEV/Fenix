plugins {
    id("fr.d4emon.fenix.dev")
}

description = "In-repo mod used to exercise the loader by hand while developing it."

fenix {
    minecraft = "26.2"
}

dependencies {
    // Copied into run/mods, and compiled against.
    fenixMod("fr.d4emon.fenix:fenix-api-event:0.1.0")

    // For the title mixin. Provided by the loader at run time.
    compileOnly(libs.mixin)
}
