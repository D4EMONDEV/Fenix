---
title: Generating resources
description: Ember writes a mod's assets and data from Java, into its source tree.
order: 2
---

A block with no model is a purple cube. A block with no loot table drops
nothing, silently. A block with no translation shows its raw key. None of those
fail a build, and all of them are found by a player.

Ember writes those files from Java, so they cannot drift from the content they
describe.

## How it runs

```bash
./gradlew ember
```

Output lands in `src/main/generated`, which is part of the build and ships in
the jar. It is committed, so a change to a generator shows up in a diff like any
other change.

## The providers

| Provider | Writes |
|---|---|
| `EmberModelProvider` | block and item models, blockstates |
| `EmberLanguageProvider` | `lang/en_us.json` |
| `EmberLootTableProvider` | what a block drops |
| `EmberRecipeProvider` | crafting and smelting |
| `EmberTagsProvider` | block and item tags |
| `EmberSoundProvider` | `sounds.json` |
| `EmberOreProvider` | ore generation: the configured and placed features |

## A generator

```java
@Generator
public final class ModModels extends EmberModelProvider {

    @Override
    protected void models() {
        cubeAll(ModBlocks.RUBY_BLOCK);
        flatItem(ModItems.RUBY);
    }
}
```

`@Generator` is how Ember finds it — the same compile-time index the loader uses
for `@Mod`. A generator that does not compile is not a generator that quietly
never runs.

## Ore generation

An ore needs two files: a *configured feature* saying what to place, and a
*placed feature* saying where.

```java
@Generator
public final class ModOres extends EmberOreProvider {

    @Override
    protected void ores() {
        ore("ruby_ore", ModBlocks.RUBY_ORE, ModBlocks.DEEPSLATE_RUBY_ORE)
                .veinSize(6)
                .veinsPerChunk(4)
                .between(-48, 48)
                .discardOnAirExposure(0.5f)
                .write();
    }
}
```

Two blocks, not one: vanilla's ores each have a deepslate variant because the two
replace different blocks, and an ore that skips it shows stone-textured lumps
below y=0.

:::caution[Neither file does anything alone]
A placed feature that no biome refers to is never run. Saying which biomes want
it is code — see the world generation section of
[Getting started](/docs/0.1/guides/getting-started).
:::

## What Ember cannot generate

Textures and `.ogg` files, and the small `particles/<name>.json` that lists a
particle's sprites. Those are still written by hand.
