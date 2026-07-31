// Imported explicitly: inside a build script `java` resolves to the Java plugin
// extension, which shadows the package name.
import java.util.Properties

plugins {
    `java-gradle-plugin`
    `maven-publish`
}

// This build does not inherit the root `gradle.properties`, so read the
// coordinates from it directly and keep one source of truth.
val rootProperties = Properties().apply {
    file("../gradle.properties").inputStream().use { load(it) }
}

group = rootProperties.getProperty("group")
// Its own line, like every other module. It read the repository's umbrella
// version instead, which happened to agree and so hid that the declared key was
// never consulted — the same drift that had this plugin asking for three
// artifacts nobody had published.
version = rootProperties.getProperty("version_gradle_plugin")

description = "Gradle plugin mod authors apply to build and run a Fenix mod."

java {
    // Deliberately lower than the game's Java 25: this code runs inside the mod
    // author's Gradle daemon, which is often older than the JVM the game needs.
    // The toolchain used to compile and run the game is selected separately.
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-serial", "-parameters"))
}

dependencies {
    implementation(libs.gson)
    // Widening the compile-time copy of Minecraft is a bytecode edit.
    implementation(libs.asm)
    implementation(libs.vineflower)
}

val minecraftVersion = rootProperties.getProperty("minecraft_version")
// Every coordinate the plugin writes, read from the one file that decides them.
// Each module moves on its own, so none of these may be assumed equal to
// another or to this plugin's own version — they were, and every one of them
// pointed at an artifact that had never been published.
//
// Anything compiled against Minecraft carries the game version; the loader, the
// processor and the build tooling carry no such tie and stay plain. All of them
// are baked in so a mod's build file names none of them.
val loaderVersion = rootProperties.getProperty("version_loader")
val apiVersion = rootProperties.getProperty("version_api") + "+mc" + minecraftVersion
val emberVersion = rootProperties.getProperty("version_ember") + "+mc" + minecraftVersion
val processorVersion = rootProperties.getProperty("version_processor")
val vineflowerVersion = libs.versions.vineflower.get()

tasks.processResources {
    // Declared as inputs so a version bump actually re-expands the file rather
    // than reusing a stale, cached result.
    inputs.property("loaderVersion", loaderVersion)
    inputs.property("apiVersion", apiVersion)
    inputs.property("emberVersion", emberVersion)
    inputs.property("processorVersion", processorVersion)
    inputs.property("minecraftVersion", minecraftVersion)
    inputs.property("vineflowerVersion", vineflowerVersion)
    filesMatching("fenix-plugin.properties") {
        expand(
            "loader_version" to loaderVersion,
            "api_version" to apiVersion,
            "ember_version" to emberVersion,
            "processor_version" to processorVersion,
            "minecraft_version" to minecraftVersion,
            "vineflower_version" to vineflowerVersion,
        )
    }
}

gradlePlugin {
    plugins {
        create("fenixDev") {
            id = "fr.d4emon.fenix.dev"
            implementationClass = "fr.d4emon.fenix.gradle.FenixDevPlugin"
            displayName = "Fenix development plugin"
            description = "Downloads Minecraft, wires the Fenix loader and API, and adds runClient."
        }
    }
}

publishing {
    repositories {
        // The same on-disk repository the main build publishes into, so the
        // plugin ships alongside the loader and API for GitHub Pages.
        maven {
            name = "pages"
            url = file("../build/fenix-maven-repo").toURI()
        }
    }
}
