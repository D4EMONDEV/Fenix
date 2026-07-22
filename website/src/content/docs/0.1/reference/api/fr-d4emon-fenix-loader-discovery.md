---
title: "loader.discovery"
description: "Types in fr.d4emon.fenix.loader.discovery"
sidebar:
  order: 91
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.loader.discovery</code></p>

| Type | What it is |
|---|---|
| [`DiscoveryResult`](#discoveryresult) | What a scan of the mods directory turned up. |
| [`ModCandidate`](#modcandidate) | A mod that was found on disk, before anything has decided whether it can actually load. |
| [`ModDiscoverer`](#moddiscoverer) | Finds mods on disk. |

## DiscoveryResult

What a scan of the mods directory turned up.

Problems are returned rather than thrown so that one unreadable jar does
not hide the other four. The caller reports them together, then decides
whether to continue.

### `boolean equals(Object o)`

### `boolean hasProblems()`

{@return whether any jar failed to be read}

### `int hashCode()`

### `List<ModCandidate> mods()`

### `List<String> problems()`

### `String toString()`

## ModCandidate

A mod that was found on disk, before anything has decided whether it can
actually load.

Called a candidate rather than a mod because resolution still gets to
reject it: its dependencies may be missing, it may not run on this side, or
another jar may claim the same id.

### `boolean equals(Object o)`

### `String fileName()`

{@return the jar's file name, which is what a player recognises}

### `int hashCode()`

### `String id()`

{@return the mod's id}

### `ModMetadata metadata()`

### `boolean nested()`

### `Path path()`

### `String toString()`

### `Version version()`

{@return the mod's version}

## ModDiscoverer

Finds mods on disk.

A scan looks at the top level of the mods directory only, and considers
every `.jar` file there. Renaming a jar to `.jar.disabled` is
therefore enough to keep it out of a launch, which players already expect
from other loaders.

One bad jar never hides the others: every failure becomes an entry in
`DiscoveryResult#problems()` and the scan carries on. A jar with no
`fenix.mod.json` at all is reported too, because a file in the mods
directory that is not a Fenix mod is almost always a mod for a different
loader — silence would leave the player wondering why nothing happened.

### `static DiscoveryResult scan(Path modsDirectory)`

Scans a mods directory.

Candidates are returned in file-name order, compared case-insensitively
so the result does not depend on the file system. Load order is decided
later, by resolution — this order only makes logs and error lists stable.

### `static DiscoveryResult scan(Path modsDirectory, Path unpackDirectory)`

Scans a mods directory, unpacking any jars the mods carry inside them.

