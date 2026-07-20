plugins {
    id("fenix.java-conventions")
}

description = "A minimal fake game the loader can boot, so loader behaviour is testable without Minecraft."

dependencies {
    // For FenixHooks — the harness stands in for the mixin layer, so it calls
    // the loader's hooks directly where real Minecraft gets them injected.
    // compileOnly: at run time the loader is on the parent classpath already.
    compileOnly(project(":fenix-loader"))
}

// ---------------------------------------------------------------------------
// runDemo — the whole pipeline, no Minecraft:
// loader on the parent classpath, this jar as the game, testmod in mods/.
// ---------------------------------------------------------------------------

// What the launcher process itself runs with. Gson is compileOnly in the
// loader (the real game provides it), so the demo supplies it here.
val demoRuntime = configurations.create("demoRuntime") {
    isCanBeConsumed = false
}

dependencies {
    demoRuntime(project(":fenix-loader"))
    demoRuntime(libs.gson)
}

val stageDemoMods = tasks.register<Sync>("stageDemoMods") {
    description = "Copies the demo mods into the demo game directory"
    from(project(":testmod").tasks.named("jar"))
    into(layout.buildDirectory.dir("demo/mods"))
}

tasks.register<JavaExec>("runDemo") {
    group = "fenix"
    description = "Boots the fake game through the Fenix loader, with testmod installed"
    dependsOn(stageDemoMods)

    classpath = demoRuntime
    mainClass = "fr.d4emon.fenix.loader.launch.Launch"

    val harnessJar = tasks.jar.flatMap { it.archiveFile }
    inputs.file(harnessJar)
    val demoDir = layout.buildDirectory.dir("demo")

    argumentProviders.add {
        listOf(
            "--fenix.gameJar", harnessJar.get().asFile.absolutePath,
            "--fenix.gameMain", "fr.d4emon.fenix.testing.harness.FakeGame",
            "--fenix.gameDir", demoDir.get().asFile.absolutePath,
            "--fenix.side", "client",
        )
    }
}
