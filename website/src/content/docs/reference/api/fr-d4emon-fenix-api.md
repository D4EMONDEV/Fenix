---
title: "api"
description: "Types in fr.d4emon.fenix.api"
sidebar:
  order: 10
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.api</code></p>

| Type | What it is |
|---|---|
| [`Fenix`](#fenix) | A mod's view of the loader, handed to it at every `FenixMod` lifecycle phase. |
| [`FenixMod`](#fenixmod) | The lifecycle of a mod. |
| [`Mod`](#mod) | Marks the entry point of a mod. |
| [`ModInfo`](#modinfo) | What one mod tells the rest of the world about itself. |
| [`Side`](#side) | Which side of the game the code is currently running on. |
| [`Version`](#version) | A semantic version, as defined by Semantic Versioning 2.0.0. |
| [`VersionRange`](#versionrange) | A constraint on a `Version`, written the way package managers write one. |

## Fenix

A mod's view of the loader, handed to it at every `FenixMod` lifecycle
phase.

There is no way to obtain one statically, and that is the point: because
the context is passed in, it can know <em>which</em> mod it belongs to. That
is what makes `#logger()` attribute output automatically and
`#configDir()` resolve per mod, neither of which a singleton could do.

Instances are supplied by the loader. Mods implement this only in tests.

### `Path configDir()`

{@return the directory this mod should keep its configuration in}

Resolved per mod and created on demand, so two mods cannot collide.

### `Optional<ModInfo> findMod(String id)`

Looks up a loaded mod.

### `Path gameDir()`

{@return the game directory, holding the world saves, resource packs and mods}

### `boolean isLoaded(String id)`

Checks whether a mod is loaded.

For optional integration with another mod. A hard requirement belongs
in `depends` instead, so the player is told what is missing before
the game starts rather than after something silently did nothing.

### `Version loaderVersion()`

{@return the version of the loader itself}

### `FenixLogger logger()`

{@return a logger that attributes its output to this mod}

### `ModInfo mod()`

{@return the mod this context belongs to}

### `Collection<ModInfo> mods()`

{@return every loaded mod, in the order they were initialised}

Dependencies always come before the mods that declare them.

### `Side side()`

{@return the side this process is running on}

## FenixMod

The lifecycle of a mod.

Every method has a default implementation, so a mod overrides only the
phases it cares about. The phases always run in the order below, and a mod is
never called before anything it declares in `depends`.

Each method receives its own `Fenix` context rather than reaching for
a global one. That is what lets every mod have its own logger and its own view
of what is loaded.

### `void onInit(Fenix fenix)`

Runs once the game is up and its content is final.

The right place for everything that is not registration: listening for
events, reading configuration, wiring gameplay.

### `void onPreLaunch(Fenix fenix)`

Runs before any game class is loaded.

The only phase where the game can still be influenced before it exists:
class transformers and network payload types have to be registered here,
because the classes that read them have not been initialised yet.

Touching game classes from this method defeats its purpose — loading one
freezes it before transformation.

### `void onRegister(Fenix fenix)`

Runs while the game's registries are being populated, before they freeze.

Blocks, items and anything else that lives in a registry are added here.
This window exists because the game finalises registry contents in one pass
and refuses additions afterwards.

## Mod

Marks the entry point of a mod.

The annotated class must implement `FenixMod`, be concrete, and have a
public no-argument constructor. Fenix checks all three while the mod compiles,
so a mistake is a compiler error with a line number rather than a mod that
silently never loads.

The id must match the `id` of the jar's `fenix.mod.json`; the
loader refuses to start a mod where the two disagree.

```java
@Mod("example-mod")
public final class ExampleMod implements FenixMod {

   @Override
   public void onInit(Fenix fenix) {
       fenix.logger().info("Hello from {}", fenix.mod().name());
   }
}
```

Retention is `RetentionPolicy#CLASS`: the annotation is read at
compile time and the result is written into the jar, so nothing scans for it
at startup. It stays in the bytecode only so that tooling can inspect a jar
without recompiling it.

### `String value()`

{@return the id of the mod this class belongs to}

## ModInfo

What one mod tells the rest of the world about itself.

This is the public view of a mod, handed to other mods through
`Fenix#mods()`. It deliberately omits everything only the loader needs —
dependency constraints, mixin configurations, the jar it came from — so that
loading internals can change without breaking mods.

### `List<String> authors()`

### `String description()`

### `boolean equals(Object o)`

### `int hashCode()`

### `String id()`

### `static boolean isValidId(String id)`

Checks whether a string is a usable mod id, without throwing.

Exposed so that metadata parsing and the annotation processor apply the
same rule as this record, and can report a failure in their own terms.

### `String license()`

### `String name()`

### `String toString()`

### `Version version()`

## Side

Which side of the game the code is currently running on.

This is a fact about the running process, so it is always exactly one of
the two. It is not the same thing as the `side` field of
`fenix.mod.json`, which says where a mod is <em>allowed</em> to run and
therefore also accepts `both`.

A side check is not enough on its own to keep client-only code off a
server. Class loading resolves every type a method mentions, so the guarded
code has to live in a <em>separate method</em> — an `if` in the same
method still fails.

**Constants**

| Name | What it is |
|---|---|
| `CLIENT` | The game client, with a window, rendering and input. |
| `SERVER` | A dedicated server, with no client classes available at all. |

### `boolean isClient()`

{@return whether this is {@link #CLIENT}}

### `boolean isServer()`

{@return whether this is {@link #SERVER}}

### `static Side valueOf(String name)`

### `static Side[] values()`

## Version

A semantic version, as defined by <a href="https://semver.org">Semantic Versioning 2.0.0</a>.

Parsing is deliberately more permissive than the specification in one way:
a missing minor or patch component defaults to zero, so `26.2` parses
and equals `26.2.0`. Minecraft numbers its releases that way and there
is nothing to gain from refusing them.

<strong>Ordering ignores build metadata</strong>, as the specification
requires, while `#equals(Object)` does not. So `1.0.0+a` and
`1.0.0+b` compare equal but are not equal — the one place this type
knowingly departs from the usual `Comparable` advice.

### `String build()`

### `int compareTo(Version other)`

### `boolean equals(Object o)`

### `int hashCode()`

### `boolean isPreRelease()`

{@return whether this version carries pre-release identifiers}

A pre-release always sorts below the release it leads to, so
`1.0.0-rc1` is older than `1.0.0`.

### `int major()`

### `int minor()`

### `static Version parse(String text)`

Parses a version.

### `int patch()`

### `String preRelease()`

### `String toString()`

## VersionRange

A constraint on a `Version`, written the way package managers write one.

<table border="1">
<caption>Accepted syntax</caption>
<tr><th>Written</th><th>Means</th></tr>
<tr><td>`*`</td><td>any version</td></tr>
<tr><td>`1.2.3`</td><td>exactly that version</td></tr>
<tr><td>`>=1.2.0`</td><td>that version or newer; `>`, `<=` and `<` also work</td></tr>
<tr><td>`^1.2.0`</td><td>compatible updates, so `>=1.2.0 <2.0.0`</td></tr>
<tr><td>`~1.2.0`</td><td>patch updates, so `>=1.2.0 <1.3.0`</td></tr>
</table>

Below `1.0.0` the caret tightens, because a project that has not
reached its first release breaks things on minor bumps: `^0.2.0` means
`>=0.2.0 <0.3.0`, and `^0.0.3` means `>=0.0.3 <0.0.4`. This
matters right now — Fenix itself is a `0.x` project.

Bounds are compared exactly as `Version` orders versions, so a
pre-release falls inside a range that spans it. `^1.0.0` therefore
accepts `1.5.0-rc.1`. That is simpler than the exclusion rules package
managers layer on top, and predictable, which matters more here.

**Constants**

| Name | What it is |
|---|---|
| `ANY` | Matches every version. |

### `static VersionRange atLeast(Version version)`

{@return a range matching the given version and anything newer}

### `static VersionRange atMost(Version version)`

{@return a range matching the given version and anything older}

### `static VersionRange compatibleWith(Version version)`

{@return a range of updates that should not break a consumer of the given version}

The upper bound follows the leftmost non-zero component, so the range
tightens below `1.0.0`. See the type documentation.

### `boolean contains(Version version)`

Checks a version against this constraint.

### `boolean equals(Object o)`

### `static VersionRange exactly(Version version)`

{@return a range matching only the given version}

### `static VersionRange greaterThan(Version version)`

{@return a range matching anything newer than the given version}

### `int hashCode()`

### `static VersionRange lessThan(Version version)`

{@return a range matching anything older than the given version}

### `Optional<Version> lower()`

### `boolean lowerInclusive()`

### `static VersionRange parse(String text)`

Parses a constraint.

### `static VersionRange patchesOf(Version version)`

{@return a range of patch updates to the given version}

### `String toString()`

Renders the range in a canonical form.

Deliberately not the text it was parsed from: `>=0.2.0 <0.3.0`
tells a player what is actually required, where `^0.2.0` assumes
they know the notation.

### `Optional<Version> upper()`

### `boolean upperInclusive()`

