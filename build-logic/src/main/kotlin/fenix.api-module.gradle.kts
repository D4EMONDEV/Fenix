/**
 * A slice of the Fenix API.
 *
 * Each slice is an independent, publishable module so a mod can depend on just
 * the pieces it uses. `:fenix-api` aggregates them for anyone who would rather
 * pull everything in one line.
 */

plugins {
    id("fenix.publish-conventions")
}

// Every slice builds on the core contracts (`@Mod`, `Fenix`, lifecycle types).
if (path != ":fenix-api-core") {
    dependencies {
        "api"(project(":fenix-api-core"))
    }
}
