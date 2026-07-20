plugins {
    id("fenix.publish-conventions")
}

description = "Annotation processor that writes the compile-time mod index the loader reads at startup."

// Deliberately dependency-free.
//
// This processor runs inside the mod author's compiler, so it must not drag
// anything onto their annotation processor path. Annotations are matched by
// their fully qualified name and the index is written as hand-rolled JSON.

dependencies {
    // Tests compile real mod sources against the actual API; the processor
    // itself stays dependency-free.
    testImplementation(project(":fenix-api-core"))
}
