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
val vineflowerVersion = libs.versions.vineflower.get()

// The plugin carries every game version Fenix has a release for, so a mod that
// asks for an older one gets the API built for it rather than the newest. The
// table is `platforms.json` at the repository root; it ships in the jar and
// `fr.d4emon.fenix.gradle.Platforms` reads it.
val platformsFile = file("../platforms.json")

// Two files now state the versions for the line being developed: gradle.properties,
// which the modules are actually built with, and platforms.json, which is what
// mod authors are handed. They must agree, and nothing about editing one makes
// you edit the other — so the build refuses to produce a plugin where they
// disagree. Without this the failure surfaces as a mod resolving a version that
// was never published, far from the file that was left behind.
val checkPlatforms = tasks.register("checkPlatforms") {
    val properties = rootProperties
    val json = platformsFile
    inputs.file(json)
    inputs.property("versions", properties.stringPropertyNames().sorted()
        .joinToString(",") { "$it=${properties.getProperty(it)}" })
    // A file, so the check is up-to-date-able rather than rerun on every build.
    val stamp = layout.buildDirectory.file("platforms-checked.txt")
    outputs.file(stamp)
    doLast {
        @Suppress("UNCHECKED_CAST")
        val parsed = groovy.json.JsonSlurper().parse(json) as Map<String, Any>
        val platforms = parsed["platforms"] as List<Map<String, Any>>
        val line = properties.getProperty("minecraft_version")
        val entry = platforms.firstOrNull { it["minecraft"] == line }
            ?: throw GradleException(
                "platforms.json has no entry for Minecraft $line, the version this" +
                    " repository builds against. Add one, or the plugin will refuse" +
                    " to build a mod for the very version it was compiled from."
            )
        val expected = mapOf(
            "loader" to properties.getProperty("version_loader"),
            "api" to properties.getProperty("version_api"),
            "ember" to properties.getProperty("version_ember"),
            "processor" to properties.getProperty("version_processor"),
        )
        val drifted = expected.filter { (key, value) -> entry[key] != value }
        if (drifted.isNotEmpty()) {
            throw GradleException(
                "platforms.json disagrees with gradle.properties for Minecraft $line: " +
                    drifted.entries.joinToString(", ") { (key, value) ->
                        "$key is ${entry[key]} there and $value here"
                    } + ". Both describe the same release, so both have to be bumped."
            )
        }
        // The website tells visitors which plugin version to apply, and reads
        // it from here. It is this project's own version, so it drifts the
        // moment a release bumps one file and not the other.
        val declaredPlugin = parsed["plugin"]
        if (declaredPlugin != version.toString()) {
            throw GradleException(
                "platforms.json says the plugin is $declaredPlugin, but this build produces" +
                    " $version. That number is what the website tells people to apply."
            )
        }
        if (platforms.first()["minecraft"] != line) {
            throw GradleException(
                "platforms.json lists ${platforms.first()["minecraft"]} first, but this" +
                    " repository builds $line. The first entry is the default a mod gets" +
                    " when it names no version, so it has to be the current line."
            )
        }
        stamp.get().asFile.writeText("$line ok\n")
    }
}

tasks.processResources {
    dependsOn(checkPlatforms)
    from(platformsFile)
    // Declared as inputs so a version bump actually re-expands the file rather
    // than reusing a stale, cached result.
    inputs.property("vineflowerVersion", vineflowerVersion)
    filesMatching("fenix-plugin.properties") {
        expand("vineflower_version" to vineflowerVersion)
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
