---
title: fenix.mod.json
description: Every field of the Fenix mod metadata file.
order: 1
---

Every mod ships one at the root of its jar. It describes the mod; it does not
point at code — that is what `@Mod` and the compile-time index are for.

## Fields

| Field | Required | Meaning |
|---|---|---|
| `schema` | yes | Metadata format version. Currently `1`. |
| `id` | yes | Unique id. Lowercase, `a-z0-9-`. |
| `version` | yes | Semantic version of this mod. |
| `name` | no | Human-readable name. Defaults to `id`. |
| `description` | no | One or two sentences. |
| `authors` | no | List of names. |
| `license` | no | SPDX identifier. |
| `contact` | no | Free-form map; `homepage` and `issues` are conventional. |
| `side` | no | `both` (default), `client`, or `server`. |
| `depends` | no | What must be present. Also orders this mod after it. |
| `breaks` | no | What this mod refuses to run alongside. |
| `after` | no | What this mod loads after, when present, without needing it. |
| `mixins` | no | Mixin config files to load. |
| `accessible` | no | Vanilla members to raise to public. |

## Version constraints

| Constraint | Matches |
|---|---|
| `1.2.3` | exactly that version |
| `>=1.2.0` | that version or newer; `>`, `<=` and `<` work too |
| `^1.2.0` | compatible updates — `>=1.2.0 <2.0.0` |
| `~1.2.0` | patch updates — `>=1.2.0 <1.3.0` |
| `*` | any version |

:::note[The caret tightens below 1.0.0]
A project that has not reached its first release breaks things on minor bumps,
so `^0.2.0` means `>=0.2.0 <0.3.0`. Fenix is a `0.x` project today.
:::

A version missing its minor or patch is filled in with zeros, so `~26.2` behaves
as `~26.2.0`. `minecraft` and `fenix` are valid ids in `depends`.

## Depending on the API

One line is enough:

```json
"depends": {
  "fenix": ">=0.1.0",
  "minecraft": "~26.2",
  "fenix-api": ">=0.1.0"
}
```

`fenix-api` is a mod in its own right — the bundle jar — and it declares every
module it carries, so this also orders your mod after all of them.

Naming individual modules is allowed and says something narrower. It buys
little, because the modules travel inside the bundle and arrive together, and it
costs something real: the list is written once and then goes quietly stale. The
example mod declared three modules while using six, for weeks, and nothing
complained — `depends` asserts presence, and all six were present anyway.

## Ordering without requiring

```json
"after": { "somemod": "*" }
```

That is what a compatibility patch needs: it has to run after the mod it
patches, and still load when that mod is absent. Saying it with `depends` was
the only way before, and it turned every optional integration into a hard
requirement.

## Refusing a combination

```json
"breaks": { "oldmod": "<2.0.0" }
```

The launch is refused, naming both mods and the constraint. Without it the
incompatibility surfaces as a crash inside one of the two, which names neither
and blames whichever happened to be on the stack.

## Reaching what vanilla keeps shut

```json
"accessible": [
  "class net.minecraft.world.inventory.MenuType$MenuSupplier",
  "method net.minecraft.world.inventory.MenuType <init>",
  "field net.minecraft.client.Minecraft instance"
]
```

Names are dotted, without descriptors. The loader applies them before anything
loads, and the Gradle plugin applies the same declarations to the copy of
Minecraft you compile against, so `javac` and the game cannot disagree.

Reach for it only when a mixin cannot do the job: `@Accessor` and `@Invoker`
already cover a private field or method. Widening is for the cases where a type
cannot be *named* at all.
