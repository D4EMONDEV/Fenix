---
title: Versioning and Minecraft support
description: Which number means what, and how Fenix follows Minecraft releases.
order: 2
---

# Versioning

Fenix publishes several things that change at different speeds, so they carry
different version numbers. Every one of them is declared in
[`gradle.properties`](https://github.com/D4EMONDEV/Fenix/blob/main/gradle.properties), which is the only place to look.

## The three lines

| | Property | What it means |
|---|---|---|
| **The loader** | `version_loader` | The platform contract. This is what a mod's `depends: { "fenix": ">=x" }` names, so it moves when the loader's promises to mods change — not when its internals do. |
| **The API set** | `version_api` | A release of the modules below. The bundle carries it and pins their versions, so "Fenix API 0.2.0" names one exact collection. |
| **One module** | `version_api_<module>` | That module's own surface. Bumped when its API changes and nothing else. |

The point of the last one is the point of splitting the API at all: a fix in the
registry should not make every mod that only uses events look out of date.

## Why some versions say `+mc26.2`

Anything compiled against Minecraft carries the game version as semver build
metadata:

```
fenix-api-registry-0.1.0+mc26.2.jar     built against Minecraft
fenix-loader-0.1.0.jar                  not
```

Those artifacts only work with the game they were built for. A coordinate that
does not say so invites somebody to find that out at run time, in a crash
report, rather than at resolution.

The loader, the annotation processor and the build tooling carry no such tie and
stay plain.

## What a mod writes

The game version, and nothing else. Everything else is looked up for that game
in the table the plugin carries — see [Minecraft versions](/docs/@latest/reference/game-versions).

```kotlin
plugins { id("fr.d4emon.fenix.dev") version "0.2.1" }
fenix { minecraft = "26.2" }
```

Any of those lookups can be overridden, one line each, which is what testing an
unreleased loader or a locally built Ember looks like:

```kotlin
fenix {
    minecraft = "26.2"
    loader = "0.1.2"
    api = "0.4.0"
    ember = "0.2.1"
}
```

`api` and `ember` are built against the game, so their real coordinates end in
`+mc26.2`. Write either form: a version without the suffix gets the one for
`minecraft` appended. `loader` carries no suffix, because almost nothing in the
loader touches a Minecraft class.

An override is taken exactly as written, so overriding one does not move the
others. That is the point, and also the risk — a loader and an API from
different releases are a pair nobody tested.

## Adding a module

Add one line to `gradle.properties`:

```properties
version_api_command=0.1.0
```

The convention plugin derives it from the project name — `fenix-api-command`
becomes `version_api_command` — so nothing else needs editing, and a module
without a line of its own falls back to the repository `version`.
