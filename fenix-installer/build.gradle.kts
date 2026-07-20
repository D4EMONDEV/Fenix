plugins {
    id("fenix.publish-conventions")
}

description = "Installs a Fenix profile — version manifest and launcher entry — into a .minecraft directory."

dependencies {
    // The installer runs standalone, outside the game, so it bundles its own JSON
    // support rather than borrowing the copy on the vanilla classpath.
    implementation(libs.gson)
}
