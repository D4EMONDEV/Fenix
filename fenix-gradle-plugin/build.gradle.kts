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
version = rootProperties.getProperty("version")

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
    implementation(libs.vineflower)
}

// TODO: declare the plugin once its implementation class exists.
//
// gradlePlugin {
//     plugins {
//         create("fenixDev") {
//             id = "fr.d4emon.fenix.dev"
//             implementationClass = "fr.d4emon.fenix.gradle.FenixDevPlugin"
//             displayName = "Fenix development plugin"
//             description = "Downloads Minecraft, wires the Fenix loader, and adds runClient / runServer / genSources / ember."
//         }
//     }
// }
