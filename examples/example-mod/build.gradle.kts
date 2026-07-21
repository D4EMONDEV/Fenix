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

// No dependencies block: the plugin puts the whole Fenix API on the compile
// classpath and into run/mods, so what this mod is written against is what is
// there when it runs. A mod that wants a smaller set says so:
//
//     fenix { api = false }
//     dependencies { fenixMod("fr.d4emon.fenix:fenix-api-event:0.1.0") }
