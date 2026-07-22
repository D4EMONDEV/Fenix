---
title: "loader.launch"
description: "Types in fr.d4emon.fenix.loader.launch"
sidebar:
  order: 91
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.loader.launch</code></p>

| Type | What it is |
|---|---|
| [`FenixHooks`](#fenixhooks) | The game's way back into the loader. |
| [`FenixRuntime`](#fenixruntime) | The state behind every `Fenix` context, and the thing that fires the lifecycle. |
| [`FenixVersion`](#fenixversion) | The loader's own version, stamped into `fenix-loader.properties` at build time. |
| [`Launch`](#launch) | The loader's entry point: what the launcher starts instead of the game. |
| [`LaunchException`](#launchexception) | A fatal, already-explained launch failure. |
| [`LoadedMod`](#loadedmod) | A mod that made it all the way in: resolved, on the classpath, instantiated. |
| [`ModIndexReader`](#modindexreader) | Reads `fenix.index.json`, the file the annotation processor writes into a mod jar at compile time. |
| [`ModInstantiator`](#modinstantiator) | Turns resolved candidates into `LoadedMod`s by reading each jar's compile-time index and instantiating the classes it names — through the `FenixClassLoader`, so mod code lives in the child scope from its very first instruction. |

## FenixHooks

The game's way back into the loader.

The later lifecycle phases can only be fired from <em>inside</em> the game
— registration while the registries are still open, init once the game is up.
With real Minecraft the calls are injected by the loader's own mixins; the
test harness's fake game calls them directly, which is exactly the point of
having a fake game.

This class lives in the loader, so it is parent-only: game code compiled
against it resolves to the same class the loader holds, static state
included.

### `static Map<String,Path> modJars()`

{@return every loaded mod's jar, keyed by mod id, in load order}

For the parts of Fenix that have to treat a mod as a <em>file</em>
rather than as code — resource loading above all, which hands the jars to
the game as resource packs.

Empty when no game is running, rather than throwing: something asking
for mod files outside a launch should get "there are none", not a crash.

### `static void onGameInit()`

Fires `onInit` for every mod. The game calls this once it is up.
Repeats are ignored.

### `static void onGameRegister()`

Fires `onRegister` for every mod. The game calls this while its
registries are still open. Repeats are ignored.

## FenixRuntime

The state behind every `Fenix` context, and the thing that fires the
lifecycle.

Each phase runs at most once — the game may reach a hook twice (a second
`MinecraftServer` in the same process, say) but mods must not — and
always walks the mods in load order, so a mod can rely on its dependencies
having been through a phase before it enters it.

### `void fireInit()`

Fires `onInit`. Called from inside the game once it is up.

### `void firePreLaunch()`

Fires `onPreLaunch`. Called by `Launch` before any game class
is loaded.

### `void fireRegister()`

Fires `onRegister`. Called from inside the game, while its
registries are still open.

### `List<LoadedMod> mods()`

{@return every loaded mod, in load order}

## FenixVersion

The loader's own version, stamped into `fenix-loader.properties` at
build time. This is what satisfies a mod's `"fenix"` dependency.

### `static Version current()`

{@return the version of the running loader}

## Launch

The loader's entry point: what the launcher starts instead of the game.

A Fenix launcher profile inherits from the vanilla version, so the
launcher builds the vanilla classpath — game jar included — and starts this
class with the vanilla arguments. Fenix's own options are namespaced
`--fenix.*`; every other argument belongs to the game and passes
through untouched. The game directory is peeked from the vanilla
`--gameDir` so the loader and the game always agree on it.

The pipeline is discovery, resolution, classloading, instantiation,
`onPreLaunch`, then the game's own `main`. The later phases fire
from inside the game through `FenixHooks`.

### `static void main(String[] args)`

Launches the game through Fenix.

### `static void run(String[] args)`

The pipeline, separated from `#main` so failures stay throwable.

Public because `#main` is a dead end for anything that wants to
know how the launch went: it prints and calls `System#exit`. Fenix's
own conformance tests drive this instead, and so could anything embedding
the loader.

## LaunchException

A fatal, already-explained launch failure.

The message is written for the person reading the crash: it names the mod
or the file at fault and, where possible, what to do about it. `main`
prints it without a stack trace — the trace belongs to unexpected failures,
not diagnosed ones.

## LoadedMod

A mod that made it all the way in: resolved, on the classpath, instantiated.

### `List<FenixMod> entries()`

### `boolean equals(Object o)`

### `int hashCode()`

### `String id()`

{@return the mod's id}

### `ModMetadata metadata()`

### `Path path()`

### `String toString()`

## ModIndexReader

Reads `fenix.index.json`, the file the annotation processor writes into
a mod jar at compile time.

The index maps mod ids to the binary names of their `@Mod` classes.
A jar without one is a mod with no code — a resource pack in mod clothing —
which is perfectly legal.

**Constants**

| Name | What it is |
|---|---|
| `FILE_NAME` | The index file, at the root of the jar. |
| `CLIENT_FILE_NAME` | The client-only index, written from a mod's client source set. |
| `SUPPORTED_SCHEMA` | The index schema this loader understands. |

### `static Map<String,String> read(Reader reader, String source)`

Reads an index.

### `static Map<String,String> read(String json, String source)`

Convenience for tests and tools: reads an index from text.

### `static Map<String,String> readFromJar(Path jar)`

Reads an index from a jar.

### `static Map<String,String> readFromJar(Path jar, String file)`

Reads one of a jar's indexes.

## ModInstantiator

Turns resolved candidates into `LoadedMod`s by reading each jar's
compile-time index and instantiating the classes it names — through the
`FenixClassLoader`, so mod code lives in the child scope from its very
first instruction.

The annotation processor already rejected most ways these classes can be
wrong, so a failure here means the jar was assembled by hand or manipulated
after compilation; the errors still name the jar and the class, because
hand-assembled jars are exactly the ones that need good errors.

### `static List<LoadedMod> instantiate(FenixClassLoader loader, List<ModCandidate> loadOrder, Side side)`

Instantiates every mod's entry classes.

