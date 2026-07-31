---
title: Fenix API
description: The API reference and practical guides for writing Fenix mods.
order: 0
---

Fenix API is the code a mod uses after the game has started: lifecycle,
registries, events, resources, networking, configuration and client helpers.
The API bundle is versioned independently from this website: this documentation
is for **Fenix API 0.3.0**, targeting **Minecraft 26.2**.

## Start here

| If you need to… | Read… |
|---|---|
| Create a minimal mod project | [Getting started](/docs/@latest/guides/getting-started) |
| Understand how registrations are organised | [Content and registries](/docs/@latest/guides/content) |
| Generate assets or data | [Ember](/docs/@latest/guides/ember) |
| Browse exact classes and methods | [API reference](/docs/@latest/api/index) |

## What belongs to Fenix API

- `fr.d4emon.fenix.api` is the mod lifecycle and context.
- `fr.d4emon.fenix.registry` registers game content safely.
- `fr.d4emon.fenix.event`, `network`, `config` and `resource` cover common mod
  integrations.
- `fr.d4emon.fenix.ember` generates resource and data files from Java.

Your own mod classes do not belong to Fenix API. Names such as `ModContent`,
`ModItems` or `MyBlocks` are conventions you create to keep your code organised;
the API only provides the building blocks those classes use.

:::note[Minecraft compatibility]
Fenix begins at Minecraft **26.2** and is never designed or backported for an
older game version. A Fenix release targets one exact Minecraft version. When a
new game release is supported, it is published as a new Fenix release.
:::
