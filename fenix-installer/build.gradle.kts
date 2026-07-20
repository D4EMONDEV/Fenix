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
