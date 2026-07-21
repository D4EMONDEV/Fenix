plugins {
    id("fenix.publish-conventions")
    // Library mode: Minecraft on the compile classpath, nothing else. Ember is
    // a piece of Fenix, and its own generator flag would be circular.
    id("fr.d4emon.fenix.dev")
}

description = "Ember — generates Minecraft assets and data from plain Java instead of hand-written JSON."

fenix {
    library = true
}

dependencies {
    api(project(":fenix-api-core"))
    // Content is passed as the Holder it was registered with.
    api(project(":fenix-api-registry"))

    // Ember runs inside a real game process, so Gson comes from the vanilla
    // classpath rather than being shipped again.
    compileOnly(libs.gson)
}
