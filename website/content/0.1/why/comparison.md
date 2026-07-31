---
title: Compared to other loaders
description: Where Fenix does something else on purpose — and where it simply has less.
order: 1
---

Fabric, Forge and NeoForge are mature, and Fenix is not. This page is about the
places where Fenix does something **different on purpose**, and it ends with the
places where it is simply behind.

## No remapping, anywhere

Minecraft has shipped unobfuscated since 26.1. Every loader built before that
carries a mapping layer: names to translate, refmaps to generate, a remapping
step in the build, and a decompiler view that does not match what you type.

Fenix has none of it. A mixin targets `net.minecraft.client.Minecraft` by that
name, the jar you compile against is the jar that runs, and a stack trace in a
crash report reads like your source.

| | Fenix | Fabric | NeoForge |
|---|---|---|---|
| Mappings to install | none | yes | yes |
| Refmap generation | none | yes | yes |
| Remap step in the build | none | yes | yes |

## Mods are found when they compile

An annotation processor writes an index into the jar naming each `@Mod` class.
The loader reads that file; it never scans classes looking for entry points.

Two consequences, and the second is the one that matters day to day:

- Startup does no classpath scanning at all.
- A misspelled entry class is a **compile error**, not a mod that silently does
  nothing.

## The API absorbs vanilla's bookkeeping

Registering a block in Minecraft is not one call. Vanilla does a pass over its
own blocks in a static initialiser — block state network ids, the item mapping,
and more — long finished by the time a mod runs. Skip any of it and the failure
surfaces far from its cause: a player kicked over an id the server cannot find,
or a creative tab dying on a stack size.

Fenix's registrar does all of it, and a conformance suite proves it by booting
real Minecraft and inspecting the result.

## Creative tabs that page

Vanilla's tab strip holds exactly fourteen, and vanilla fills all fourteen. Every
loader has to do something; Fenix adds pages, with arrows at the top right of the
panel and on Page Up/Page Down. Search, inventory, hotbars and op blocks travel
to every page — losing search to reach a mod's blocks is what makes paging feel
bad elsewhere.

## Access widening from the manifest

Some of vanilla's doors are shut in a way no mixin can open. `MenuType`'s
constructor is private and its parameter is a *private interface*, so there is
nothing a mod can write down, in any package.

A mod declares what it needs once, in `fenix.mod.json`. The loader raises those
members before anything loads them, **and** the Gradle plugin applies the same
declarations to the copy of Minecraft the mod compiles against. Both read the one
file the mod already ships, so what `javac` allows and what the game allows
cannot drift apart.

## Every claim is checked against the game

Twenty-two conformance checks boot real Minecraft through the loader and inspect
what happened. Each one was verified to fail when the thing it covers is
sabotaged — a check that cannot fail proves nothing.

That is why this page can be specific. It is also why the next one is honest.

## Where Fenix is behind

- **Ecosystem.** Fabric and NeoForge have thousands of mods. Fenix has an
  example mod.
- **Fluids.** Not one registration but four — the fluid, its flowing form, the
  block and the bucket — plus a renderer. A wrapper covering only the first
  would be worse than none.
- **Custom recipes.** The registries are there; a `Recipe` implementation worth
  handing to a mod is not.
- **A modern starting point.** Fenix support begins at 26.2 and is never
  backported to older versions. Each release targets one exact Minecraft
  version; support for a newer game version is a new Fenix release.
- **Pre-1.0.** The API changes without notice.
