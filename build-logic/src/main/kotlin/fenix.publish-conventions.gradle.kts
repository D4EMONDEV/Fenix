import org.gradle.api.plugins.JavaPluginExtension

/**
 * Applied to modules that ship to a Maven repository. The published artifact id
 * is the Gradle project name, which `settings.gradle.kts` already sets to the
 * final artifact name (for example `fenix-api-event`).
 */

plugins {
    id("fenix.java-conventions")
    id("maven-publish")
}

extensions.configure<JavaPluginExtension> {
    withJavadocJar()
}

publishing {
    repositories {
        // A plain Maven repository laid out on disk, which the publish workflow
        // deploys to GitHub Pages. Every module writes into the same root-level
        // directory so the whole thing deploys as one site.
        maven {
            name = "pages"
            url = rootProject.layout.buildDirectory.dir("fenix-maven-repo").get().asFile.toURI()
        }
    }
    publications.register<MavenPublication>("maven") {
        from(components["java"])

        pom {
            name = project.name
            description = provider { project.description ?: "Part of the Fenix mod loader." }
            url = "https://github.com/D4EMONDEV/Fenix"

            licenses {
                license {
                    name = "Apache-2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                }
            }
            developers {
                developer {
                    id = "d4emon"
                    name = "D4EMON"
                }
            }
            scm {
                url = "https://github.com/D4EMONDEV/Fenix"
                connection = "scm:git:https://github.com/D4EMONDEV/Fenix.git"
                developerConnection = "scm:git:ssh://git@github.com/D4EMONDEV/Fenix.git"
            }
        }
    }
}
