---
title: Install Fenix
description: Adding Fenix to the Minecraft Launcher, or to a server.
order: 1
---

## Before you start

Install Minecraft **26.2** from the launcher and run it once. The installer
builds its profile on top of the version the launcher has already downloaded,
and will say so plainly if it is missing.

## Players

Download the latest **fenix-installer-…-windows.zip** from
[the releases page](https://github.com/D4EMONDEV/Fenix/releases), unzip it, and
run **Fenix Installer**.

It adds a Fenix profile to the Minecraft Launcher. Open the launcher, pick the
profile, press Play. Mods go in `.minecraft/mods`, as usual.

The installer carries its own Java — there is nothing else to install.

### What it writes

| Where | What |
|---|---|
| `versions/fenix-<version>-<mc>/` | A version manifest that inherits from vanilla |
| `libraries/fr/d4emon/fenix/` | The loader and the API core |
| `libraries/net/fabricmc/`, `libraries/org/ow2/asm/` | Mixin and ASM |
| `launcher_profiles.json` | One profile, named Fenix |

Nothing in `versions/26.2/` is touched. Vanilla keeps working, and deleting the
Fenix profile deletes Fenix.

## Servers

With arguments the installer is a command-line tool, which is what you want on a
machine with no screen:

```bash
"Fenix Installer" --dir /srv/minecraft --minecraft 26.2
```

## Mod authors

You do not need the installer to develop. The Gradle plugin downloads and
launches the game itself:

```kotlin title="settings.gradle.kts"
pluginManagement {
    repositories {
        maven("https://d4emondev.github.io/Fenix/")
        gradlePluginPortal()
    }
}
```

```kotlin title="build.gradle.kts"
plugins { id("fr.d4emon.fenix.dev") version "0.1.5" }

fenix { minecraft = "26.2" }
```

Then `./gradlew runClient`. [Getting started](/docs/@latest/guides/getting-started)
takes it from there.

## Building the installer yourself

```bash
git clone https://github.com/D4EMONDEV/Fenix
cd Fenix
./gradlew :fenix-installer:distInstaller
```

The application lands in `fenix-installer/build/distributions/`.
