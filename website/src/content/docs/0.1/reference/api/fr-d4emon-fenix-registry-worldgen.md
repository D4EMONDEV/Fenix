---
title: "registry.worldgen"
description: "Types in fr.d4emon.fenix.registry.worldgen"
sidebar:
  order: 21
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.registry.worldgen</code></p>

| Type | What it is |
|---|---|
| [`BiomeModifications`](#biomemodifications) | Adds features to biomes that already exist — an ore, a plant, a spring. |
| [`BiomeSelector`](#biomeselector) | Decides which biomes a modification applies to. |
| [`BiomeSelector.Context`](#biomeselector-context) | What a selector gets to look at. |
| [`BiomeSelectors`](#biomeselectors) | The selectors most modifications want. |

## BiomeModifications

Adds features to biomes that already exist — an ore, a plant, a spring.

```java
BiomeModifications.addFeature(BiomeSelectors.overworld(),
       GenerationStep.Decoration.UNDERGROUND_ORES,
       ResourceKey.create(Registries.PLACED_FEATURE, Identifier.parse("mymod:ruby_ore")));
```

The alternative is overriding whole biome files in a datapack, and that
does not compose: two mods each adding an ore to the plains would overwrite
one another, and the player would see whichever loaded last.

The feature itself is data — a `configured_feature` saying what to
place and a `placed_feature` saying where, both of which
`EmberOreProvider` generates. This only says which biomes get it.

Call it from `onRegister`. Modifications are applied each time
datapacks load, which is what makes them survive `/reload` and apply to
whatever world is opened next.

### `static void addFeature(BiomeSelector where, GenerationStep.Decoration step, ResourceKey<PlacedFeature> feature)`

Adds a placed feature to every biome a selector matches.

## BiomeSelector

Decides which biomes a modification applies to.

Called once per biome, after datapacks have loaded, so it sees whatever
biomes are actually in the world — including other mods' — rather than a list
written down in advance.

### `boolean test(BiomeSelector.Context biome)`

## BiomeSelector.Context

What a selector gets to look at.

### `boolean equals(Object o)`

### `boolean hasTag(TagKey<Biome> tag)`

{@return whether the biome carries a tag}

Tags are the right question to ask nearly always: `is_overworld` covers every overworld biome a datapack or another mod
adds, and naming biomes one by one does not.

### `int hashCode()`

### `Holder<Biome> holder()`

### `ResourceKey<Biome> key()`

### `String toString()`

## BiomeSelectors

The selectors most modifications want.

### `static BiomeSelector all()`

{@return a selector matching every biome}

### `static BiomeSelector end()`

{@return a selector matching every End biome}

### `static BiomeSelector nether()`

{@return a selector matching every Nether biome}

### `static BiomeSelector only(ResourceKey<Biome>[] keys)`

{@return a selector matching exactly the biomes named}

Precise, and brittle in proportion: a biome nobody has added yet is a
biome this will not match. Prefer `#tagged` where a tag says what
you mean.

### `static BiomeSelector overworld()`

{@return a selector matching every overworld biome}

By tag, so it covers biomes added by datapacks and by other mods. This
is what an ore usually wants.

### `static BiomeSelector tagged(TagKey<Biome> tag)`

{@return a selector matching biomes carrying a tag}

