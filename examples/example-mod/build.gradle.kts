plugins {
    id("fr.d4emon.fenix.dev")
}

// This is the whole build file a Fenix mod needs. The plugin puts Minecraft on
// the compile classpath, wires the API and the annotation processor, and adds
// runClient. Inside this repository the Fenix coordinates resolve to the
// sibling projects; an external mod gets them from a Maven repository.

group = "fr.d4emon.fenix.example"
version = "1.0.0"

description = "Sample mod: the smallest thing a Fenix mod author has to write."

fenix {
    minecraft = "26.2"
}
