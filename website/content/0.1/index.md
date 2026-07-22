---
title: Documentation
description: What Fenix is, and where to start depending on why you are here.
order: 0
---

Fenix is a mod loader for **Minecraft 26.2**. It launches the game, loads mods
into it, and gives those mods an API that absorbs the vanilla bookkeeping they
would otherwise skip and crash on.

It is pre-1.0 and honest about that: it works, it is tested against the real
game, and the API still changes without notice.

## Start where you are

| You want to | Go to |
|---|---|
| Play with Fenix mods | [Install Fenix](/docs/0.1/play/install) |
| Add mods to your game | [Adding mods](/docs/0.1/play/mods) |
| Write a mod | [Getting started](/docs/0.1/guides/getting-started) |
| Generate assets and data | [Ember](/docs/0.1/guides/ember) |
| Look something up | [`fenix.mod.json`](/docs/0.1/reference/mod-metadata) |
| Know how it compares | [Compared to other loaders](/docs/0.1/why/comparison) |

## The three pieces

**The loader** starts the game and starts your mods inside it: discovery,
dependency resolution, class transformation and Mixin.

**The installer** is a double-click application that adds a Fenix profile to the
Minecraft Launcher. It carries its own Java.

**Ember** generates a mod's assets and data from Java — models, loot tables,
recipes, tags, translations, sounds and ore placement.

## What a mod's build file looks like

```kotlin
plugins { id("fr.d4emon.fenix.dev") version "0.1.3" }

fenix { minecraft = "26.2" }
```

That is the whole thing. The plugin downloads the game, compiles the mod against
it, and gives you `runClient` and `runServer`.

:::caution[Before 1.0]
Anything on these pages can change. Versions are published per module, so a mod
that uses only events does not look out of date when the registry changes — see
[`fenix.mod.json`](/docs/0.1/reference/mod-metadata) for how to depend on them.
:::
