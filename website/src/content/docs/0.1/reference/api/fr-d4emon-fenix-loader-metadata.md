---
title: "loader.metadata"
description: "Types in fr.d4emon.fenix.loader.metadata"
sidebar:
  order: 91
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.loader.metadata</code></p>

| Type | What it is |
|---|---|
| [`InvalidMetadataException`](#invalidmetadataexception) | Thrown when a `fenix.mod.json` cannot be understood. |
| [`ModDependency`](#moddependency) | One entry of a mod's `depends`. |
| [`ModMetadata`](#modmetadata) | Everything a `fenix.mod.json` declares. |
| [`ModMetadataReader`](#modmetadatareader) | Reads `fenix.mod.json`. |
| [`ModSide`](#modside) | Where a mod declares it is allowed to run, from the `side` field of `fenix.mod.json`. |

## InvalidMetadataException

Thrown when a `fenix.mod.json` cannot be understood.

Always carries the source it came from, because the person who has to fix
it is usually looking at a folder of jars and needs to know which one is at
fault before anything else.

Unchecked on purpose: discovery catches these at its own boundary so it can
report every broken mod at once, rather than stopping at the first.

### `String source()`

{@return where the offending metadata came from}

## ModDependency

One entry of a mod's `depends`.

A dependency does two jobs: it refuses to start when something required is
missing or too old, and it orders initialisation so a mod always runs after
what it depends on.

### `boolean equals(Object o)`

### `int hashCode()`

### `String id()`

### `boolean isSatisfiedBy(Version version)`

Checks a candidate version against this dependency.

### `VersionRange range()`

### `String toString()`

## ModMetadata

Everything a `fenix.mod.json` declares.

This is the loader's own view. Mods see `ModInfo`, which is a subset:
dependency constraints, mixin configurations and the declared side are of no
use to another mod, and keeping them out means loading can be reworked without
breaking anyone.

### `List<String> accessible()`

### `List<String> authors()`

### `Map<String,String> contact()`

### `List<ModDependency> depends()`

### `String description()`

### `boolean equals(Object o)`

### `int hashCode()`

### `String id()`

### `String license()`

### `List<String> mixins()`

### `String name()`

### `ModSide side()`

### `ModInfo toModInfo()`

{@return the subset of this metadata that other mods are allowed to see}

### `String toString()`

### `Version version()`

## ModMetadataReader

Reads `fenix.mod.json`.

The JSON tree is walked by hand rather than bound to the record
reflectively. It is more code, but every failure can then name the field and
the jar it came from — and a broken metadata file is something a mod author
has to fix from an error message alone.

Gson comes from the vanilla classpath, so Fenix does not ship a second copy
of it. That is also why parsing must not rely on Gson's reflective binding,
which would tie the metadata format to whichever version the game happens to
bundle.

**Constants**

| Name | What it is |
|---|---|
| `FILE_NAME` | The name of the metadata file, at the root of every mod jar. |
| `SUPPORTED_SCHEMA` | The metadata schema version this loader understands. |

### `static ModMetadata read(Reader reader, String source)`

Reads metadata from a stream.

### `static ModMetadata read(String json, String source)`

Reads metadata from text.

## ModSide

Where a mod declares it is allowed to run, from the `side` field of
`fenix.mod.json`.

Not to be confused with `Side`, which is where the process actually
<em>is</em> and therefore only ever has two values. The two meet at
`#includes(Side)`.

**Constants**

| Name | What it is |
|---|---|
| `CLIENT` | Loads on a client only, and is skipped on a dedicated server. |
| `SERVER` | Loads on a server only, including the server inside a single-player game. |
| `BOTH` | Loads everywhere. |

### `boolean includes(Side side)`

Checks whether a mod declaring this should load on the given side.

### `static ModSide parse(String text)`

Parses the `side` field.

### `static ModSide valueOf(String name)`

### `static ModSide[] values()`

