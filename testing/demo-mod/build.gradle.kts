plugins {
    id("fenix.java-conventions")
}

description = "A Minecraft-free mod, so the fake-game smoke test can exercise mod loading."

dependencies {
    compileOnly(project(":fenix-api-core"))
    annotationProcessor(project(":fenix-processor"))
}
