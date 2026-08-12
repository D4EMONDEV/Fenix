---
title: Fenix
description: Written by hand, for people writing mods.
order: 0
---

Fenix is a Minecraft mod loader and an API for writing mods against it. This is
its documentation: guides written by hand, not a class list generated from
source. A generated reference tells you a method exists; it does not tell you
which of two ways is the one that survives a datapack reload, or which mistake
fails silently.

Every code sample here is taken from code that compiles.

## Start here

| If you want to… | Read |
|---|---|
| Play with Fenix installed | [Install Fenix](/docs/@latest/play/install) |
| Write your first mod | [Getting started](/docs/@latest/guides/getting-started) |
| Add blocks, items, entities | [Content and registries](/docs/@latest/guides/content) |
| Stop writing JSON by hand | [Ember](/docs/@latest/guides/ember) |
| React to what the game does | [Events](/docs/@latest/guides/events) |
| Draw something, or read the keyboard | [Client-side code](/docs/@latest/guides/client) |
| Edit the game itself | [Mixins and access](/docs/@latest/guides/mixins) |

Or take a whole project from the [generator](/generate) and read the code it
gives you.

## What Fenix is made of

**The loader** starts the game, finds mods, resolves what depends on what, and
runs Mixin. It touches almost no Minecraft class, so it carries no game version
and one build runs several.

**The API** is what a mod calls once the game is up: lifecycle, registries,
events, resources, networking, commands, configuration. It is compiled against
Minecraft, so a release belongs to one game version —
see [Minecraft versions](/docs/@latest/reference/game-versions).

**Ember** writes the resource and data files a mod needs from the Java that
already declares them, so a block's model and its loot table cannot drift from
the block.

**The Gradle plugin** downloads the game, wires the rest up, and adds
`runClient`. A mod's build file names the Minecraft version and nothing else.

## Two rules worth knowing early

**Register in `onRegister`, listen in `onInit`.** The first runs before the
game's registries are frozen and is the only moment content can be added. The
second runs after, when a server exists and a config file can be read.

**`src/main` cannot see the client.** It compiles against a Minecraft with the
client half removed, so reaching for a renderer there is a compile error rather
than a crash on somebody else's dedicated server. Client code goes in
`src/client`.

## Naming

`ModContent`, `ModItems`, `ModBlocks` appear throughout these guides. They are
conventions, not API — classes you write to keep declarations in one place.
Fenix only provides the `Registrar` they use.
