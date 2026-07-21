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

dependencies {
    // A mod-on-mod dependency: compiled against, and copied into run/mods so it
    // is actually there at run time.
    fenixMod("fr.d4emon.fenix:fenix-api-event:0.1.0")
    fenixMod("fr.d4emon.fenix:fenix-api-registry:0.1.0")
    fenixMod("fr.d4emon.fenix:fenix-api-resource:0.1.0")
}
