plugins {
    id("fenix.publish-conventions")
}

description = "Installs a Fenix profile — version manifest and launcher entry — into a .minecraft directory."

dependencies {
    // The installer runs standalone, outside the game, so it bundles its own JSON
    // support rather than borrowing the copy on the vanilla classpath.
    implementation(libs.gson)
}

// The loader jars travel inside the installer jar, so the installer is one
// self-contained file with nothing to download.
tasks.processResources {
    filesMatching("fenix-installer.properties") {
        expand(mapOf(
            "version" to project.version.toString(),
            "minecraft_version" to providers.gradleProperty("minecraft_version").get(),
        ))
    }
    from(project(":fenix-loader").tasks.named("jar")) { into("fenix-payload") }
    from(project(":fenix-api-core").tasks.named("jar")) { into("fenix-payload") }
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
