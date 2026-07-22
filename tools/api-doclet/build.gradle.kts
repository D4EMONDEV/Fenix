plugins {
    id("fenix.java-conventions")
}

description = "A javadoc doclet that writes the API reference as Starlight pages."

// Never published: this is build tooling, like build-logic. It also compiles
// against jdk.javadoc, which is a JDK module rather than a dependency.
