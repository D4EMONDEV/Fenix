plugins {
    id("fr.d4emon.fenix.dev")
}

description = "In-repo mod used to exercise the loader by hand while developing it."

fenix {
    minecraft = "26.2"
}

dependencies {
    // The whole API arrives by default; only the mixin library is extra here.
    // Provided by the loader at run time.
    compileOnly(libs.mixin)
}
