# 0002 — Discover mods at compile time, not at launch

**Status:** Accepted — 2026-07-20

## Context

A loader has to find the entry points of every installed mod. The usual options:

1. **Class names in the mod's JSON.** Fabric's `entrypoints`. The declaration
   sits far from the code it names, and a rename or a typo produces a mod that
   silently never loads.
2. **`META-INF/services`.** Standard, but the same split-declaration problem
   with a filename nobody remembers, and it still fails only at launch.
3. **Scanning the classpath for an annotation at startup.** Costs real startup
   time, growing with the number of mods.

## Decision

An annotation processor — `fenix-processor` — reads `@Mod` while the mod
compiles and writes an index into the jar. The loader reads that index.

The processor has zero dependencies, because it runs inside the mod author's
compiler.

## Consequences

- Startup does no scanning. It reads one small file per jar.
- The declaration is the annotation, on the class, where it can be renamed
  safely by any refactoring tool.
- The processor rejects at compile time what the alternatives only reveal at
  launch: an abstract class, a non-static inner class, a missing public no-arg
  constructor, a class that does not implement what it claims.
- Mod authors must have the processor on their annotation processor path. The
  Gradle plugin wires it automatically; anyone not using the plugin declares it
  themselves.
- The index format is now a compatibility surface between the processor and the
  loader, and the two must ship together.
