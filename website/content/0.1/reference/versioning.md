---
title: Versioning and Minecraft support
description: Which number means what, and how Fenix follows Minecraft releases.
order: 2
---

Fenix has different version numbers because the game, the loader and the API do
not change at the same speed. They are related, but they are not interchangeable.

## The current versions

| Name | Current value | What it identifies |
|---|---:|---|
| Documentation line | `0.1` | This set of guides and reference pages. |
| Fenix API | `0.2.0` | The current published API bundle. |
| Minecraft target | `26.2` | The exact game version current Fenix artifacts are built and tested against. |

The `0.1` shown in the documentation picker is therefore **not** an API
download version. It lets the site keep old guides available when a later Fenix
line changes its API.

## Compatibility policy

Fenix starts at **Minecraft 26.2**. It will **never** target, support or be
backported to a Minecraft version older than 26.2.

A Fenix release supports one exact Minecraft version. This matters because it
compiles against that game's classes and its game-facing artifacts include the
target in their version, for example `fenix-api-0.2.0+mc26.2.jar`.

That means “26.2+” describes the project's direction, not a promise that one
jar works with every future game version. A mod made for 26.2 should use the
matching Fenix build; a future Minecraft 26.3 release receives its own matching
Fenix build.

## Updating for a new Minecraft version

When Mojang releases a new Minecraft version, Fenix moves forward as a release
line:

1. Update the repository's `minecraft_version` in `gradle.properties`.
2. Update and test the loader, API and Gradle plugin against the new game.
3. Run the real-game conformance tests, then publish new game-bound artifacts
   carrying `+mc<game version>`.
4. Publish matching installer and Gradle-plugin versions, and copy the site
   content into a new documentation line when its instructions or APIs changed.

Old Fenix releases and their documentation remain available. A new release is
added beside them; it does not silently change a project's target Minecraft
version.

## What mod authors do

Use the Fenix Gradle plugin version that matches the game version you want to
target:

```kotlin
plugins { id("fr.d4emon.fenix.dev") version "0.1.4" }

fenix { minecraft = "26.2" }
```

When support for a new Minecraft version ships, update both values to the
versions announced by that Fenix release, test your mod, and publish a new mod
build. Do not point a 26.2 mod at a newer game version without that work.
