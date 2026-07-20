---
title: fenix.mod.json
description: Every field of the Fenix mod metadata file.
sidebar:
  order: 1
---

Every mod ships one at the root of its jar. It describes the mod; it does not
point at code — that is what `@Mod` and the compile-time index are for.

## Fields

| Field         | Required | Meaning                                                   |
|---------------|----------|-----------------------------------------------------------|
| `schema`      | yes      | Metadata format version. Currently `1`.                    |
| `id`          | yes      | Unique id. Lowercase, `a-z0-9-`.                           |
| `version`     | yes      | Semantic version of this mod.                              |
| `name`        | no       | Human-readable name. Defaults to `id`.                     |
| `description` | no       | One or two sentences.                                      |
| `authors`     | no       | List of names.                                             |
| `license`     | no       | SPDX identifier.                                           |
| `contact`     | no       | Free-form map; `homepage` and `issues` are conventional.   |
| `side`        | no       | `both` (default), `client`, or `server`.                   |
| `depends`     | no       | Map of mod id to version constraint.                       |
| `mixins`      | no       | Mixin config files to load.                                |

## Version constraints

| Constraint | Matches                                             |
|------------|-----------------------------------------------------|
| `1.2.3`    | exactly that version                                 |
| `>=1.2.0`  | that version or newer; `>`, `<=` and `<` work too    |
| `^1.2.0`   | compatible updates — `>=1.2.0 <2.0.0`                |
| `~1.2.0`   | patch updates — `>=1.2.0 <1.3.0`                     |
| `*`        | any version                                          |

:::note[The caret tightens below 1.0.0]
A project that has not reached its first release breaks things on minor bumps,
so `^0.2.0` means `>=0.2.0 <0.3.0` and `^0.0.3` means `>=0.0.3 <0.0.4`. Fenix is
a `0.x` project today.
:::

A version missing its minor or patch component is filled in with zeros, so
`~26.2` behaves as `~26.2.0`. A pre-release falls inside any range that spans
it, so `^1.0.0` accepts `1.5.0-rc.1`.

`minecraft` and `fenix` are valid ids in `depends`.

`depends` also drives **initialisation order**: a mod is always initialised
after everything it depends on.
