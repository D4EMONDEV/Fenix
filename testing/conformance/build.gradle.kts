plugins {
    id("fenix.java-conventions")
}

description = "Checks that can only be proven by loading real Minecraft classes through the loader."

// Rule of thumb, see CONTRIBUTING.md: anything you could only verify by
// launching the game by hand belongs here as an automated check instead.

dependencies {
    implementation(project(":fenix-loader"))
    implementation(project(":fenix-api"))

    // The mixin conformance test compiles a real mixin fixture and drives the
    // library directly. ASM is used to synthesise the target class.
    testImplementation(libs.mixin)
    testImplementation(libs.bundles.asm)
}

tasks.test {
    // Mixin bootstraps once per JVM and never resets, so each test class that
    // touches it gets a fresh process.
    forkEvery = 1
}
