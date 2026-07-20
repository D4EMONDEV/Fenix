plugins {
    id("fenix.java-conventions")
}

description = "Checks that can only be proven by loading real Minecraft classes through the loader."

// Rule of thumb, see CONTRIBUTING.md: anything you could only verify by
// launching the game by hand belongs here as an automated check instead.

dependencies {
    implementation(project(":fenix-loader"))
    implementation(project(":fenix-api"))
}
