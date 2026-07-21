import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.language.jvm.tasks.ProcessResources

/**
 * Baseline for every Java module in the repository: toolchain, compiler flags,
 * tests, and `fenix.mod.json` version stamping.
 */

plugins {
    id("java-library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

/*
 * A module's version, and whether the game version belongs in it.
 *
 * Derived from the project name rather than declared per module, so adding a
 * module means adding one line to gradle.properties and nothing else. A module
 * with no line of its own falls back to the repository version.
 *
 * `+mc<version>` is semver build metadata, and it goes on anything compiled
 * against Minecraft: those artifacts are only usable with the game they were
 * built for, and a coordinate that does not say so invites someone to find out
 * at run time. The loader, the processor and the build tooling carry no such
 * tie and stay plain.
 */
run {
    val key = "version_" + project.name.removePrefix("fenix-").replace('-', '_')
    val declared = providers.gradleProperty(key).orNull
        ?: providers.gradleProperty("version").get()

    val gameTied = project.name.startsWith("fenix-api") || project.name == "ember"
    version = if (gameTied) {
        declared + "+mc" + providers.gradleProperty("minecraft_version").get()
    } else {
        declared
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.findVersion("java").get().requiredVersion)
    }
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // `-parameters` keeps parameter names for reflection-free diagnostics.
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-serial", "-Xlint:-processing", "-parameters"))
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

/**
 * Mod metadata declares its version as `${version}`; substitute the real one at
 * build time so a module's manifest can never drift from its Gradle version.
 */
tasks.named<ProcessResources>("processResources") {
    val tokens = mapOf(
        "version" to project.version.toString(),
        "minecraft_version" to providers.gradleProperty("minecraft_version").get(),
    )
    inputs.properties(tokens)
    filesMatching("fenix.mod.json") { expand(tokens) }
}

dependencies {
    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}
