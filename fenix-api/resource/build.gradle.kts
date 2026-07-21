plugins {
    id("fenix.api-module")
    // Library mode: Minecraft on the compile classpath, nothing else.
    id("fr.d4emon.fenix.dev")
}

description = "Exposes a mod's assets and data to the game's resource and datapack repositories."

fenix {
    library = true
}

dependencies {
    // The mixin that injects the pack source. Provided by the loader at run time.
    compileOnly(libs.mixin)

    // FenixHooks.modJars(): which jars to hand the game as packs. The loader is
    // on the parent classpath at run time.
    compileOnly(project(":fenix-loader"))
}
