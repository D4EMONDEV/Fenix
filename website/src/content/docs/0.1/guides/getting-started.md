---
title: Getting started
description: Set up a Fenix mod project and run it.
sidebar:
  order: 1
slug: 0.1/guides/getting-started
---

:::caution
Fenix is pre-1.0 and the API will change. This workflow works today; the
[roadmap](https://github.com/D4EMONDEV/Fenix/blob/main/docs/roadmap.md) lists
what is still missing — networking, config and commands, mainly.
:::

## Requirements

* **JDK 25** — Minecraft 26.2 runs on Java 25
* **Minecraft 26.2**

## A new mod project

```kotlin title="settings.gradle.kts"
pluginManagement {
    repositories {
        maven("https://d4emondev.github.io/Fenix/")
        gradlePluginPortal()
    }
}
```

```kotlin title="build.gradle.kts"
plugins {
    id("fr.d4emon.fenix.dev") version "0.1.0"
}

fenix {
    minecraft = "26.2"
}
```

The plugin puts Minecraft and the Fenix API on the compile classpath *and* into
`run/mods`, and adds the Fenix repository — so that really is the entire build
file. Set `fenix { api = false }` and name modules with `fenixMod(…)` to ship
against fewer.

## The mod class

```java title="src/main/java/com/example/ExampleMod.java"
@Mod("example-mod")
public final class ExampleMod implements FenixMod {

    @Override
    public void onInit(Fenix fenix) {
        fenix.logger().info("Hello from {}", fenix.mod().name());
    }
}
```

Nothing in your metadata points at this class. The annotation processor finds it
while your mod compiles.

## Metadata

```json title="src/main/resources/fenix.mod.json"
{
  "schema": 1,
  "id": "example-mod",
  "version": "1.0.0",
  "name": "Example Mod",
  "depends": {
    "fenix": ">=0.1.0",
    "minecraft": "~26.2"
  }
}
```

See the [metadata reference](/0.1/reference/mod-metadata/) for every field.

## Running

```bash
./gradlew runClient
./gradlew runServer
```

## Adding content

```java title="ModBlocks.java"
public static final Holder<Block> RUBY_BLOCK = ModContent.REGISTRAR
        .newBlock("ruby_block")
        .strength(3f)
        .requiresTool()
        .withItem()          // also registers the item that places it
        .register();
```

Registered with one call from `onRegister`. A `Holder` stands in until then, so
content can live in `static final` fields.

## The creative menu

Until content is in a tab, only `/give` reaches it.

```java
CreativeTabs.addTo(CreativeTabs.BUILDING_BLOCKS, ModBlocks.RUBY_BLOCK);
```

A tab of the mod's own works the same way, and belongs in `ModContent` since
blocks and items both go in it:

```java
public static final ResourceKey<CreativeModeTab> TAB =
        REGISTRAR.creativeTab("example_mod", ModItems.RUBY);

CreativeTabs.addTo(TAB, ModBlocks.RUBY_BLOCK, ModItems.RUBY);
```

Vanilla's tab strip is two rows of seven and vanilla fills all fourteen, so mod
tabs land on a second page. Arrows appear at the top right of the panel, Page Up
and Page Down do the same, and both stay hidden while there is only one page.

Search, the inventory, saved hotbars and operator blocks follow the player to
every page, leaving ten slots a page for mod tabs.

Name the tab from the key itself so the two cannot drift apart:

```java
add(ModContent.TAB, "Example Mod");
```

## Reacting to the game

```java
BlockEvents.BREAK.register(event ->
        isProtected(event.pos()) ? Flow.CANCEL : Flow.CONTINUE);
```

`BlockEvents` is the server's, where cancelling actually holds.
`ClientBlockEvents` only makes a refusal feel immediate — it is never the
enforcement point.

## Generating resources

```java title="ModLanguage.java"
@Generator
public final class ModLanguage extends EmberLanguageProvider {
    @Override
    protected void translations() {
        add(ModBlocks.RUBY_BLOCK, "Ruby Block");
    }
}
```

`./gradlew ember` writes models, translations, loot tables, recipes and tags
into `src/main/generated`. Textures are the one thing you still draw yourself.
