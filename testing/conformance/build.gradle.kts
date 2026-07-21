plugins {
    id("fenix.java-conventions")
    // Library mode, purely to have the plugin download and cache Minecraft:
    // the checks below hand the game jar to the loader as a file, they do not
    // compile against it.
    id("fr.d4emon.fenix.dev")
}

description = "Checks that can only be proven by loading real Minecraft classes through the loader."

// Rule of thumb, see CONTRIBUTING.md: anything you could only verify by
// launching the game by hand belongs here as an automated check instead.

fenix {
    library = true
}

dependencies {
    implementation(project(":fenix-loader"))
    implementation(project(":fenix-api"))

    // The mixin conformance test compiles a real mixin fixture and drives the
    // library directly. ASM is used to synthesise targets and to read back the
    // bytecode Mixin produced.
    testImplementation(libs.mixin)
    testImplementation(libs.bundles.asm)
}

// The event checks need two files, not classpath entries: the game to load
// through the loader, and the event module's jar as a mod would ship it.
// The sibling has to be evaluated before its jar task can be referenced.
evaluationDependsOn(":fenix-api-event")

val clientJar = fenix.clientJar
val eventJar = project(":fenix-api-event").tasks.named<Jar>("jar").flatMap { it.archiveFile }

tasks.test {
    // Mixin bootstraps once per JVM and never resets, so each test class that
    // touches it gets a fresh process.
    forkEvery = 1

    inputs.file(eventJar).withPropertyName("eventJar")
    doFirst {
        systemProperty("fenix.test.clientJar", clientJar.get().asFile.absolutePath)
        systemProperty("fenix.test.eventJar", eventJar.get().asFile.absolutePath)
    }
}
