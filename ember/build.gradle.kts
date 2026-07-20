plugins {
    id("fenix.publish-conventions")
}

description = "Ember — generates Minecraft assets and data from plain Java instead of hand-written JSON."

dependencies {
    api(project(":fenix-api-core"))

    // Ember runs inside a real game process so the registries it reads are live;
    // Gson therefore comes from the vanilla classpath.
    compileOnly(libs.gson)
}
