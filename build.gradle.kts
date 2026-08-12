plugins {
    base
}

description = "Fenix — a modern Minecraft mod loader"

/**
 * Resolve the Fenix coordinates to the in-repo projects.
 *
 * The dev plugin refers to Fenix by Maven coordinate — the form an external mod
 * uses — but a regular multi-project build does not substitute those for its own
 * projects the way a composite build would. Without this, `example-mod` would
 * try to download `fr.d4emon.fenix:fenix-api` from a repository instead of
 * building the sibling project, and a fresh clone could not build until
 * `installFenix` had run. These rules make the coordinates mean the projects.
 */
subprojects {
    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("fr.d4emon.fenix:fenix-loader")).using(project(":fenix-loader"))
            substitute(module("fr.d4emon.fenix:fenix-api")).using(project(":fenix-api"))
            substitute(module("fr.d4emon.fenix:fenix-processor")).using(project(":fenix-processor"))
            substitute(module("fr.d4emon.fenix:ember")).using(project(":ember"))
            listOf("core", "event", "registry", "resource", "network", "command", "config").forEach {
                substitute(module("fr.d4emon.fenix:fenix-api-$it")).using(project(":fenix-api-$it"))
            }
        }
    }
}

/**
 * Publishes every Fenix artifact to the local Maven repository so that a mod
 * project outside this repository can resolve them with `mavenLocal()`.
 *
 * Includes the Gradle plugin, which lives in a separate composite build.
 */
tasks.register("installFenix") {
    group = "fenix"
    description = "Publishes the loader, the API, Ember and the Gradle plugin to ~/.m2"

    dependsOn(provider {
        subprojects
            .filter { it.plugins.hasPlugin("maven-publish") }
            .map { "${it.path}:publishToMavenLocal" }
    })
    dependsOn(gradle.includedBuild("fenix-gradle-plugin").task(":publishToMavenLocal"))
}

/**
 * Builds one browsable Javadoc site covering everything a mod author writes
 * against.
 *
 * Every module already publishes a `-javadoc` jar, which is what an IDE reads
 * and what nobody browses: reading it means downloading it, and answering "what
 * can the registry do" means knowing to look in `fenix-api-registry` first. One
 * site indexes the lot.
 *
 * Both halves of each module are included. The client half is where
 * `KeyBindings`, `MenuScreens` and `EntityRendering` live — the classes most
 * likely to be looked up, and the ones a per-module split hides.
 */
val apiDocs = tasks.register<Javadoc>("apiDocs") {
    group = "fenix"
    description = "Builds the combined API documentation into build/docs/api"

    setDestinationDir(layout.buildDirectory.dir("docs/api").get().asFile)
    title = "Fenix API"

    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        addStringOption("Xdoclint:all,-missing", "-quiet")
        // Minecraft publishes no Javadoc to link against, so every
        // net.minecraft type stays a plain name. Nothing to be done about it.
        links("https://docs.oracle.com/en/java/javase/25/docs/api/")
    }
}

subprojects {
    plugins.withId("java") {
        // Documented: what a mod is written against. The installer, the
        // processor and the test harness are tools, not API.
        if (!name.startsWith("fenix-api-") && name != "ember" && name != "fenix-loader") {
            return@withId
        }
        val sourceSets = extensions.getByType<SourceSetContainer>()
        apiDocs.configure {
            sourceSets.matching { it.name == "main" || it.name == "client" }.all {
                source(allJava)
                // Compiled output as well as the compile classpath: the mixins
                // are excluded from the sources below but still imported by the
                // classes that are documented, and reading them as class files
                // is what lets those imports resolve.
                classpath += compileClasspath + output.classesDirs
            }
            // Mixins are how Fenix is built, not what a mod calls. Leaving them
            // out of the site also avoids a real conflict: only a module
            // declaring `accessible` compiles against a widened Minecraft, and
            // merging every module's classpath into one puts a widened jar and
            // a plain one side by side — so a mixin naming a widened type fails
            // to resolve, depending on which jar came first.
            exclude("**/mixin/**")
        }
    }
}

/**
 * Assembles the whole Fenix Maven repository under `build/fenix-maven-repo`,
 * which the publish workflow deploys to GitHub Pages. This is the public
 * counterpart of `installFenix` — same artifacts, a shareable repository
 * instead of the developer's `~/.m2`.
 */
tasks.register("publishFenixRepo") {
    group = "fenix"
    description = "Builds the public Fenix Maven repository into build/fenix-maven-repo"

    dependsOn(provider {
        subprojects
            .filter { it.plugins.hasPlugin("maven-publish") }
            .map { "${it.path}:publishAllPublicationsToPagesRepository" }
    })
    dependsOn(gradle.includedBuild("fenix-gradle-plugin").task(":publishAllPublicationsToPagesRepository"))
}
