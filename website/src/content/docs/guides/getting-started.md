---
title: Getting started
description: Set up a Fenix mod project and run it.
sidebar:
  order: 1
---

:::caution
Fenix is not released yet. This page describes the intended workflow; follow the
[roadmap](https://github.com/D4EMONDEV/Fenix/blob/main/docs/roadmap.md) for what
actually works today.
:::

## Requirements

- **JDK 25** — Minecraft 26.2 runs on Java 25
- **Minecraft 26.2**

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

The plugin puts Minecraft and the Fenix API on the compile classpath and adds
the Fenix repository, so that is the entire build file.

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

See the [metadata reference](/reference/mod-metadata/) for every field.

## Running

```bash
./gradlew runClient
./gradlew runServer
```
