---
title: "ember"
description: "Types in fr.d4emon.fenix.ember"
sidebar:
  order: 80
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.ember</code></p>

| Type | What it is |
|---|---|
| [`EmberGenerator`](#embergenerator) | Something that writes part of a mod's resource files. |
| [`EmberLanguageProvider`](#emberlanguageprovider) | Writes `en_us.json`. |
| [`EmberLootTableProvider`](#emberloottableprovider) | Writes block loot tables — what a block drops when broken. |
| [`EmberModelProvider`](#embermodelprovider) | Writes models, blockstates and item model definitions. |
| [`EmberOreProvider`](#emberoreprovider) | Writes the two files an ore needs to generate. |
| [`EmberOreProvider.Ore`](#emberoreprovider-ore) | Collects an ore's numbers, then writes both files. |
| [`EmberOutput`](#emberoutput) | Where generated files go, and the small amount of shared machinery for writing them. |
| [`EmberProvider`](#emberprovider) | The base every provider shares. |
| [`EmberRecipeProvider`](#emberrecipeprovider) | Writes crafting recipes. |
| [`EmberRecipeProvider.Shaped`](#emberrecipeprovider-shaped) | A recipe where the arrangement matters. |
| [`EmberRecipeProvider.Shapeless`](#emberrecipeprovider-shapeless) | A recipe where only the ingredients matter. |
| [`EmberRunner`](#emberrunner) | Runs the generators. |
| [`EmberSoundProvider`](#embersoundprovider) | Writes `sounds.json`. |
| [`EmberTagsProvider`](#embertagsprovider) | Writes tags — the groups the game and other mods reason about, like "things a pickaxe mines" or "planks". |
| [`EmberTagsProvider.BlockTagsProvider`](#embertagsprovider-blocktagsprovider) | Tags of blocks. |
| [`EmberTagsProvider.ItemTagsProvider`](#embertagsprovider-itemtagsprovider) | Tags of items. |
| [`EmberTagsProvider.Tag`](#embertagsprovider-tag) | Collects the contents of one tag. |
| [`Generator`](#generator) | Marks a class that describes a mod's generated assets and data. |

## EmberGenerator

Something that writes part of a mod's resource files.

Normally implemented by extending one of the providers —
`EmberLanguageProvider`, `EmberModelProvider` and the rest —
rather than directly. Implement this only for output none of them covers.

### `void generate(EmberOutput output)`

Writes this generator's files.

## EmberLanguageProvider

Writes `en_us.json`.

```java
@Generator
public final class ModLanguage extends EmberLanguageProvider {
   @Override
   protected void translations() {
       add(ModBlocks.RUBY_BLOCK, "Ruby Block");
       add(ModItems.RUBY, "Ruby");
   }
}
```

Entries are sorted, so the file does not reshuffle itself between runs and
a diff shows only what actually changed.

### `void add(Holder<?> content, String english)`

Names a block or an item.

One method for both: generics erase, so a pair of overloads taking
`Holder<Block>` and `Holder<Item>` would be the same method.
The kind is checked when this runs, at build time.

### `void add(ResourceKey<CreativeModeTab> tab, String english)`

Names a creative tab.

The key comes from the tab itself rather than being written out, so a
renamed tab cannot leave its translation behind — which in game shows up
as a tab titled `itemGroup.your-mod.something`.

### `void add(String key, String english)`

Adds any translation.

### `void run()`

### `void translations()`

Describes the translations.

## EmberLootTableProvider

Writes block loot tables — what a block drops when broken.

```java
@Generator
public final class ModLootTables extends EmberLootTableProvider {
   @Override
   protected void lootTables() {
       dropsSelf(ModBlocks.RUBY_BLOCK);
       drops(ModBlocks.RUBY_ORE, ModItems.RUBY);
   }
}
```

A block with no loot table drops nothing at all, silently — which is the
single most common surprise when adding a block by hand.

### `void drops(Holder<Block> block, Holder<Item> drop)`

The block drops something else, the way ore drops its material.

### `void dropsSelf(Holder<Block> block)`

The block drops itself — the usual case for a decorative block.

### `void lootTables()`

Describes the loot tables.

### `void run()`

## EmberModelProvider

Writes models, blockstates and item model definitions.

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

The methods describe an intent — "a cube with one texture" — rather than a
file. Which files that becomes is Minecraft's business and moves between
versions: 26.x wants a model definition under `items/` separate from
the model itself, and a block's item points straight at the block model with
no item model of its own.

### `void cubeAll(Holder<Block> block)`

A solid cube with the same texture on every face.

Expects a texture at `assets/<mod>/textures/block/<name>.png`,
the one thing that cannot be generated.

### `void flatItem(Holder<Item> item)`

A flat item drawn from one texture, like most crafting materials.

Expects a texture at `assets/<mod>/textures/item/<name>.png`.

### `void models()`

Describes the models.

### `void run()`

## EmberOreProvider

Writes the two files an ore needs to generate.

```java
@Generator
public final class ModOres extends EmberOreProvider {
   @Override
   protected void ores() {
       ore("ruby_ore", ModBlocks.RUBY_ORE, ModBlocks.DEEPSLATE_RUBY_ORE)
               .veinSize(9)
               .veinsPerChunk(8)
               .between(-64, 64)
               .write();
   }
}
```

Two files, because the game splits the question in two: a
<em>configured feature</em> says what to place — which block, in veins of
what size, replacing what — and a <em>placed feature</em> says where, how
often and between which heights. Neither does anything on its own.

Nor do the two together: a placed feature that no biome refers to is never
run. Say which biomes want it with `BiomeModifications.addFeature`.

The stone and deepslate blocks are separate on purpose. Vanilla's ores
each have a deepslate variant because the two replace different blocks, and
an ore that skips it appears as stone-textured lumps below y=0.

### `EmberOreProvider.Ore ore(String name, Holder<Block> stone, Holder<Block> deepslate)`

Starts describing an ore.

### `EmberOreProvider.Ore ore(String name, Holder<Block> stone)`

Starts describing an ore with no deepslate variant.

Right for an ore that only occurs above y=0. Below it, the stone block
simply never replaces deepslate and the ore stops.

### `void ores()`

Describes the ores.

### `void run()`

## EmberOreProvider.Ore

Collects an ore's numbers, then writes both files.

### `EmberOreProvider.Ore between(int min, int max)`

The heights it generates between, inclusive.

### `EmberOreProvider.Ore discardOnAirExposure(float chance)`

How often a block touching air is dropped from the vein.

What keeps ore out of cave walls. Vanilla uses 0.5 for the ores
that would otherwise be visible from every cave, and 0 for the rest.

### `EmberOreProvider.Ore veinSize(int blocks)`

How many blocks a vein holds at most.

### `EmberOreProvider.Ore veinsPerChunk(int count)`

How many veins are attempted per chunk.

Attempted, not placed: a vein whose height lands in the air or in
an existing cave places less than its size, or nothing.

### `void write()`

Writes both files.

## EmberOutput

Where generated files go, and the small amount of shared machinery for
writing them.

JSON is written by hand rather than through a serialiser. These files have
a handful of fixed shapes, and writing them directly keeps the output stable
and diffable — a generated file that reorders itself between runs is a
generated file nobody can review.

### `void asset(String path, String json)`

Writes a client resource, under this mod's namespace.

### `void data(String path, String json)`

Writes server data, under this mod's namespace.

### `void data(String namespace, String path, String json)`

Writes server data under any namespace.

Needed for tags: adding to `minecraft:mineable/pickaxe` means
writing a file in <em>Minecraft's</em> namespace, which the game then
merges with vanilla's. A tag file belongs to the tag, not to the mod
contributing to it.

### `static Identifier idOf(Object content)`

{@return the registered id of a block or item}

### `String modId()`

{@return the mod everything is being written for}

## EmberProvider

The base every provider shares.

A provider is created by Ember through its no-argument constructor and
handed its output afterwards, which is why `#output()` is only valid
once generation has started.

### `void generate(EmberOutput target)`

### `String modId()`

{@return the mod being generated for}

### `EmberOutput output()`

{@return where this provider's files go}

### `void run()`

Does the work. Each provider turns this into something domain-shaped.

## EmberRecipeProvider

Writes crafting recipes.

```java
@Generator
public final class ModRecipes extends EmberRecipeProvider {
   @Override
   protected void recipes() {
       shaped(ModBlocks.RUBY_BLOCK)
               .pattern("###", "###", "###")
               .define('#', ModItems.RUBY)
               .save();

       shapeless(ModItems.RUBY, 9).ingredient(ModBlocks.RUBY_BLOCK).save();
   }
}
```

### `void recipes()`

Describes the recipes.

### `void run()`

### `EmberRecipeProvider.Shaped shaped(Holder<?> result)`

A recipe where the arrangement matters.

### `EmberRecipeProvider.Shaped shaped(Holder<?> result, int count)`

A recipe where the arrangement matters.

### `EmberRecipeProvider.Shapeless shapeless(Holder<?> result)`

A recipe where only the ingredients matter.

### `EmberRecipeProvider.Shapeless shapeless(Holder<?> result, int count)`

A recipe where only the ingredients matter.

## EmberRecipeProvider.Shaped

A recipe where the arrangement matters.

### `EmberRecipeProvider.Shaped define(char key, Holder<?> ingredient)`

What a character in the pattern stands for.

### `EmberRecipeProvider.Shaped define(char key, String id)`

What a character stands for, by id, for vanilla content.

### `EmberRecipeProvider.Shaped named(String recipeName)`

Names the file, when the result's own name would collide.

### `EmberRecipeProvider.Shaped pattern(String[] rows)`

The grid, one string per row.

### `void save()`

Writes the recipe.

## EmberRecipeProvider.Shapeless

A recipe where only the ingredients matter.

### `EmberRecipeProvider.Shapeless ingredient(Holder<?> ingredient)`

Adds an ingredient.

### `EmberRecipeProvider.Shapeless ingredient(String id)`

Adds an ingredient by id, for vanilla content.

### `EmberRecipeProvider.Shapeless named(String recipeName)`

Names the file, when the result's own name would collide.

### `void save()`

Writes the recipe.

## EmberRunner

Runs the generators. This is the game's main class for a generation run.

It boots a real game, headlessly, before generating anything — which is
the whole trick. By the time a generator runs, the registries are populated
and the mod's own content is registered, so a generator refers to a block by
the object it registered rather than by repeating its name.

Generators are found the same way mods are: from the index the annotation
processor wrote into each jar. Nothing here is configured by naming a class
in a build file.

It has to run as the game, from a jar the Fenix classloader loaded, or its
`net.minecraft` references would resolve against a second, untouched
copy of the game — which is why Ember ships as a mod.

### `static void main(String[] args)`

Generates every mod's resources.

## EmberSoundProvider

Writes `sounds.json`.

```java
@Generator
public final class ModSounds extends EmberSoundProvider {
   @Override
   protected void sounds() {
       add(ModContent.ANVIL_CRACK, "anvil_crack");
   }
}
```

Registering a sound event is only half of a sound. The other half is this
file, which says which ogg files the event actually plays; without it the
event exists, the code that plays it runs, and nothing is heard.

Entries are sorted, so the file does not reshuffle itself between runs and
a diff shows only what actually changed.

### `void add(Holder<SoundEvent> sound, String[] files)`

Says which files a sound event plays.

More than one file makes the game pick between them at random, which
is how vanilla keeps repeated sounds from grating.

### `void run()`

### `void sounds()`

Describes the sounds.

## EmberTagsProvider

Writes tags — the groups the game and other mods reason about, like
"things a pickaxe mines" or "planks".

Use one of the nested providers; the split exists because a block tag and
an item tag are different files even when they hold the same names.

A tag file belongs to the <em>tag's</em> namespace, not the mod's. Adding
to `minecraft:mineable/pickaxe` writes into Minecraft's data directory,
and the game merges every pack's copy — which is exactly how a mod joins a
vanilla tag without replacing it.

### `void run()`

### `EmberTagsProvider.Tag tag(String tag)`

Starts describing a tag.

### `void tags()`

Describes the tags.

## EmberTagsProvider.BlockTagsProvider

Tags of blocks.

## EmberTagsProvider.ItemTagsProvider

Tags of items.

## EmberTagsProvider.Tag

Collects the contents of one tag.

### `EmberTagsProvider.Tag add(Holder<?> content)`

Adds content to the tag.

### `EmberTagsProvider.Tag add(String id)`

Adds something by id, for vanilla content or another mod's.

## Generator

Marks a class that describes a mod's generated assets and data.

The annotated class must implement `EmberGenerator`, be concrete and
public, and have a public no-argument constructor — checked while the mod
compiles, like `@Mod`.

```java
@Generator
public final class ModAssets implements EmberGenerator {
   @Override
   public void collect(Ember ember) {
       ember.cubeAll(ModBlocks.RUBY_BLOCK);
       ember.name(ModBlocks.RUBY_BLOCK, "Ruby Block");
   }
}
```

Nothing in the build points at this class by name: the annotation
processor records it, exactly as it records the mod's entry point.

