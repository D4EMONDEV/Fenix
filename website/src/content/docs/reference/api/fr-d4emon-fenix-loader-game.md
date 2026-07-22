---
title: "loader.game"
description: "Types in fr.d4emon.fenix.loader.game"
sidebar:
  order: 91
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.loader.game</code></p>

| Type | What it is |
|---|---|
| [`GameLocator`](#gamelocator) | Finds Minecraft. |
| [`GameLocator.Game`](#gamelocator-game) | A located game. |

## GameLocator

Finds Minecraft.

When the vanilla launcher starts a Fenix profile, the game jar is already
on the application classpath — the profile inherits from the vanilla version,
so the launcher assembles the vanilla classpath and merely swaps the main
class for Fenix's. The loader therefore does not download or configure
anything; it only has to recognise which classpath entry is the game.

A jar is the game when it contains a known Minecraft main class. The
client jar also contains the dedicated server's classes, so the client main
is checked first — a client jar must never be mistaken for a server.

Since Minecraft 26.1 the jar is unobfuscated and self-describing: its
`version.json` carries the game version, which is what lets mods
declare `"minecraft": "~26.2"` and have it checked.

### `static Optional<GameLocator.Game> inspect(Path jar)`

Checks whether one jar is the game.

### `static Optional<GameLocator.Game> locate(String classpath)`

Scans a classpath for the game.

## GameLocator.Game

A located game.

### `boolean equals(Object o)`

### `int hashCode()`

### `Path jar()`

### `String mainClass()`

### `Side side()`

### `String toString()`

### `Optional<Version> version()`

