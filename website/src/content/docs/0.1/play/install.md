---
title: Install Fenix
description: Install Fenix into the Minecraft Launcher.
tableOfContents: false
slug: 0.1/play/install
---

:::caution[Early days]
Fenix is pre-1.0. It works, and the API will still change without warning.
:::

## Players

Download the latest **fenix-installer-…-windows.zip** from
[the releases page](https://github.com/D4EMONDEV/Fenix/releases), unzip it, and
run **Fenix Installer**.

It adds a Fenix profile to the Minecraft Launcher. Open the launcher, pick the
profile, and press Play. Mods go in `.minecraft/mods`, as usual.

The installer carries its own Java runtime, so nothing else needs installing.

### Before you start

Install Minecraft **26.2** from the launcher and run it once. The installer
builds its profile on top of the version the launcher has already fetched, and
will say so plainly if that version is missing.

### From source

```bash
git clone https://github.com/D4EMONDEV/Fenix
cd Fenix
./gradlew :fenix-installer:distInstaller
```

The application lands in `fenix-installer/build/distributions/`.

Given arguments it stays a command-line tool, which is what you want on a
server with no screen:

```bash
"Fenix Installer" --dir /srv/minecraft --minecraft 26.2
```

## Mod authors

You do not need the installer to develop — the Gradle plugin downloads and
launches the game itself.

```kotlin title="settings.gradle.kts"
pluginManagement {
    repositories {
        maven("https://d4emondev.github.io/Fenix/")
        gradlePluginPortal()
    }
}
```

```kotlin title="build.gradle.kts"
plugins { id("fr.d4emon.fenix.dev") version "0.1.0" }

fenix { minecraft = "26.2" }
```

Then `./gradlew runClient`. The [getting started guide](/0.1/guides/getting-started/)
takes it from there.

## What gets installed

| Where | What |
|---|---|
| `versions/fenix-<version>-<mc>/` | A version manifest that inherits from vanilla |
| `libraries/fr/d4emon/fenix/` | The loader and the API core |
| `libraries/net/fabricmc/`, `libraries/org/ow2/asm/` | Mixin and ASM |
| `launcher_profiles.json` | One profile, named Fenix |

Nothing in `versions/26.2/` is touched. Vanilla keeps working, and removing the
Fenix profile removes Fenix.
