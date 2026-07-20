plugins {
    id("fenix.java-conventions")
}

description = "A minimal fake game the loader can boot, so loader behaviour is testable without Minecraft."

dependencies {
    compileOnly(project(":fenix-api-core"))
}
