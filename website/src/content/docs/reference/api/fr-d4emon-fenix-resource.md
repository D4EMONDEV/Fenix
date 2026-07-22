---
title: "resource"
description: "Types in fr.d4emon.fenix.resource"
sidebar:
  order: 70
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.resource</code></p>

| Type | What it is |
|---|---|
| [`ModPackSource`](#modpacksource) | Hands every mod jar to the game as a resource pack. |

## ModPackSource

Hands every mod jar to the game as a resource pack.

Minecraft only reads assets and data from packs. A mod jar is not one — it
has no `pack.mcmeta` — so without this a mod's models, textures and
translations sit in the jar and are never looked at. That is the difference
between a registered block and a <em>visible</em> one.

The pack is therefore built in code rather than read from the jar, which
also means mod authors never have to write a `pack.mcmeta` or keep its
format version current.

Packs are added at the top and forced on: a mod's own resources are not
something a player should have to enable, and a resource pack they *do*
enable still sits above and can override them.

### `void loadPacks(Consumer<Pack> consumer)`

