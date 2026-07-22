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

    // The registry probe is a real mod: it compiles against Minecraft and the
    // registrar, then gets repackaged into a jar the loader discovers.
    testCompileOnly(files(fenix.clientJar))
    testCompileOnly(project(":fenix-api-registry"))
    testCompileOnly(project(":fenix-api-network"))
}

// Compiling against Minecraft, and booting its registries, both need
// Minecraft's own libraries — Guava, DataFixerUpper, Brigadier and the rest.
// The game jar itself stays off this classpath: it belongs to the loader's
// child scope, where Mixin can reach it, and putting it here as well would
// invite classes to resolve from the wrong copy.
//
// compileClasspath, not compileOnly: the latter is a bucket and cannot be
// resolved.
val gameLibraries = files(provider {
    configurations.compileClasspath.get().files.filter { !it.name.startsWith("client-") }
})

dependencies {
    testCompileOnly(gameLibraries)
    testRuntimeOnly(gameLibraries)
}

// The event checks need two files, not classpath entries: the game to load
// through the loader, and the event module's jar as a mod would ship it.
// The sibling has to be evaluated before its jar task can be referenced.
evaluationDependsOn(":fenix-api-event")
evaluationDependsOn(":fenix-api-registry")
evaluationDependsOn(":fenix-api-resource")
evaluationDependsOn(":fenix-api-network")
evaluationDependsOn(":fenix-api-command")

val clientJar = fenix.clientJar
val eventJar = project(":fenix-api-event").tasks.named<Jar>("jar").flatMap { it.archiveFile }
val registryJar = project(":fenix-api-registry").tasks.named<Jar>("jar").flatMap { it.archiveFile }
val resourceJar = project(":fenix-api-resource").tasks.named<Jar>("jar").flatMap { it.archiveFile }
val networkJar = project(":fenix-api-network").tasks.named<Jar>("jar").flatMap { it.archiveFile }
val commandJar = project(":fenix-api-command").tasks.named<Jar>("jar").flatMap { it.archiveFile }

tasks.test {
    // Mixin bootstraps once per JVM and never resets, so each test class that
    // touches it gets a fresh process.
    forkEvery = 1

    // The probe is a mod, not a test: it extends real Minecraft classes and is
    // repackaged into a jar the loader runs. Gradle would otherwise try to load
    // each one looking for tests, and fail on the missing superclass — the game
    // is deliberately not on the test runtime classpath.
    exclude("fr/d4emon/fenix/probe/**")

    // The worldgen check reads what Ember actually wrote and committed, and
    // parses it with Minecraft's own codecs. The files rather than the
    // generator, because the files are what ships.
    val worldgen = rootProject.layout.projectDirectory
        .dir("examples/example-mod/src/main/generated/data/example-mod/worldgen")
    inputs.dir(worldgen).withPropertyName("worldgenData")
    doFirst {
        systemProperty("fenix.test.worldgenDir", worldgen.asFile.absolutePath)
    }

    inputs.file(eventJar).withPropertyName("eventJar")
    inputs.file(registryJar).withPropertyName("registryJar")
    inputs.file(resourceJar).withPropertyName("resourceJar")
    inputs.file(networkJar).withPropertyName("networkJar")
    inputs.file(commandJar).withPropertyName("commandJar")
    doFirst {
        systemProperty("fenix.test.clientJar", clientJar.get().asFile.absolutePath)
        systemProperty("fenix.test.eventJar", eventJar.get().asFile.absolutePath)
        systemProperty("fenix.test.registryJar", registryJar.get().asFile.absolutePath)
        systemProperty("fenix.test.resourceJar", resourceJar.get().asFile.absolutePath)
        systemProperty("fenix.test.networkJar", networkJar.get().asFile.absolutePath)
        systemProperty("fenix.test.commandJar", commandJar.get().asFile.absolutePath)
    }
}
