plugins {
    id("fenix.publish-conventions")
}

description = "Every Fenix API module in a single dependency."

/**
 * The modules this release is made of.
 *
 * Discovered rather than listed: adding a new `fenix-api-*` module needs no
 * edit here, and no module can be forgotten.
 *
 * Modules with no sources yet are skipped. They are shapes reserved for phases
 * not written, and shipping them would put empty jars in every mod's `run/mods`
 * for the loader to discover, resolve and log.
 */
val apiModules = rootProject.subprojects
    .filter { it.name.startsWith("fenix-api-") }
    .filter { it.file("src/main/java").walkTopDown().any { f -> f.extension == "java" } }

dependencies {
    apiModules.forEach { api(project(it.path)) }
}

/**
 * Carries the modules inside the jar, not merely beside it.
 *
 * A player installing the API should drop in one file, not four — and not five
 * once the next module lands, nor keep their versions in step by hand. The
 * loader unpacks anything under `META-INF/jars/` and treats each as the mod it
 * is, so the modules stay independently versioned and independently
 * publishable while the thing you install stays one file.
 *
 * `fenix-api-core` is deliberately included even though it is not a mod: it is
 * what the others compile against, and a jar of it inside is how it reaches the
 * classloader without also needing a place in `mods`.
 */
tasks.jar {
    apiModules.forEach { module ->
        from(module.tasks.named<Jar>("jar")) {
            into("META-INF/jars")
        }
    }
}
