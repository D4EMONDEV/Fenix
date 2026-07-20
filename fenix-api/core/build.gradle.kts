plugins {
    id("fenix.api-module")
}

description = "Core Fenix contracts: the @Mod annotation, the Fenix context, lifecycle hooks and logging."

// Unlike the other slices this module is not itself a mod: the loader needs
// these types to discover and instantiate mods, so they live on the parent
// classpath under `fr.d4emon.fenix.api`. See docs/architecture.md.
