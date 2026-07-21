plugins {
    id("fenix.publish-conventions")
}

description = "Installs a Fenix profile — version manifest and launcher entry — into a .minecraft directory."

dependencies {
    // The installer runs standalone, outside the game, so it bundles its own JSON
    // support rather than borrowing the copy on the vanilla classpath.
    implementation(libs.gson)
}

// ---------------------------------------------------------------------------
// Payload: the loader's entire runtime — loader, api-core, Mixin and ASM —
// travels inside the installer jar, so the installer is one self-contained
// file with nothing to download. `payload.index` records each jar's Maven
// coordinates so the installer can lay them out and list them as libraries.
// ---------------------------------------------------------------------------
val payload = configurations.create("payload") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    // Everything on the loader's runtime classpath (Gson and slf4j are
    // compileOnly there — the game provides them — so they stay out).
    payload(project(":fenix-loader"))
}

val stagePayload = tasks.register("stagePayload") {
    val outDir = layout.buildDirectory.dir("generated/payload/fenix-payload")
    inputs.files(payload).withPropertyName("payload")
    outputs.dir(outDir)

    doLast {
        val dir = outDir.get().asFile
        dir.deleteRecursively()
        dir.mkdirs()
        val index = StringBuilder()
        payload.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            val id = artifact.moduleVersion.id
            val fileName = "${id.name}-${id.version}.jar"
            artifact.file.copyTo(dir.resolve(fileName), overwrite = true)
            index.append("${id.group}:${id.name}:${id.version}:$fileName\n")
        }
        dir.resolve("payload.index").writeText(index.toString())
    }
}

tasks.processResources {
    filesMatching("fenix-installer.properties") {
        expand(mapOf(
            "version" to project.version.toString(),
            "minecraft_version" to providers.gradleProperty("minecraft_version").get(),
        ))
    }
    // The staged dir's contents go under fenix-payload/ so they resolve as
    // /fenix-payload/<jar> and /fenix-payload/payload.index at runtime.
    from(stagePayload) { into("fenix-payload") }
}

tasks.jar {
    manifest {
        attributes("Main-Class" to "fr.d4emon.fenix.installer.InstallerMain")
    }
}

tasks.register<JavaExec>("installToLauncher") {
    group = "fenix"
    description = "Installs the Fenix profile into this machine's .minecraft"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "fr.d4emon.fenix.installer.InstallerMain"
}

// ---------------------------------------------------------------------------
// A native application, so installing Fenix is a double-click rather than a
// command line. jpackage bundles a Java runtime with it: Minecraft ships its
// own Java but does not put it on the PATH, so an installer that needed one
// would turn away exactly the people it exists for.
// ---------------------------------------------------------------------------

/** Where jpackage puts its input; it insists on a directory of its own. */
val stageApp = tasks.register<Sync>("stageInstallerApp") {
    from(tasks.jar)
    // And what it runs with: jpackage puts every jar it finds here on the
    // application's classpath, and the installer parses JSON.
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("jpackage-input"))
}

val appDir = layout.buildDirectory.dir("jpackage")

val packageApp = tasks.register<Exec>("packageInstaller") {
    group = "fenix"
    description = "Builds the native Fenix Installer application"
    dependsOn(stageApp)

    inputs.files(stageApp)
    outputs.dir(appDir)

    // The toolchain's jpackage, not whatever is on the PATH: the application
    // carries the runtime it is given, and that should be the one this project
    // is built and tested against.
    val launcher = javaToolchains.launcherFor(java.toolchain).get()
    val jpackage = launcher.metadata.installationPath.file("bin/jpackage").asFile

    val appName = "Fenix Installer"
    val input = layout.buildDirectory.dir("jpackage-input").get().asFile
    val out = appDir.get().asFile

    // jpackage writes its output read-only, and jpackage refuses to write into
    // a directory that already exists — so clearing it needs the write bit put
    // back first, or the second build on any machine fails.
    doFirst {
        out.walkBottomUp().forEach {
            it.setWritable(true)
            it.delete()
        }
    }

    commandLine(
        jpackage.absolutePath,
        // app-image, not exe or msi: those need the WiX toolset installed, and
        // a build that only works on a machine somebody prepared by hand is a
        // build that breaks the first time somebody else runs it. Producing the
        // application itself works everywhere; wrapping it in a signed system
        // installer is a release concern, and belongs in CI.
        "--type", "app-image",
        "--name", appName,
        "--app-version", project.version.toString().substringBefore('+'),
        "--vendor", "D4EMON",
        "--description", "Installs Fenix into the Minecraft Launcher",
        "--input", input.absolutePath,
        "--main-jar", tasks.jar.get().archiveFileName.get(),
        "--main-class", "fr.d4emon.fenix.installer.InstallerMain",
        "--dest", out.absolutePath,

        // Only what the installer actually uses. The default is every module
        // in the JDK, which triples the download for code that draws a window
        // and copies files. java.desktop is Swing; the rest is java.base.
        "--add-modules", "java.base,java.desktop",
        "--jlink-options", "--strip-debug --no-header-files --no-man-pages --compress=zip-6",
    )
}

tasks.register<Zip>("distInstaller") {
    group = "fenix"
    description = "Zips the native Fenix Installer for release"
    dependsOn(packageApp)
    from(appDir)
    archiveFileName = "fenix-installer-${project.version}-windows.zip"
    destinationDirectory = layout.buildDirectory.dir("distributions")
}
