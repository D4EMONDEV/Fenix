plugins {
    id("fenix.api-module")
    // Library mode: Minecraft on the compile classpath, nothing else.
    id("fr.d4emon.fenix.dev")
}

description = "Content registration: blocks, items, and the vanilla bookkeeping that has to happen around them."

fenix {
    library = true
}
