---
title: Adding mods
description: Where mods go, and what to do when one refuses to load.
slug: 0.1/play/mods
---

Drop mod jars into `.minecraft/mods`. Fenix reads the folder on start; there is
nothing to enable.

## What belongs there

| File | What it is |
|---|---|
| `example-mod-1.0.0.jar` | A mod |
| `fenix-api-0.1.0+mc26.2.jar` | The API most mods need, as **one** file |

The API carries its modules inside itself, so installing or updating it is a
single file even as it grows. You do not need to match module versions by hand.

## When a mod does not load

Fenix says why, by name, before the game window opens. It never drops a jar
silently — a mod that is present and quiet is the failure that costs an evening.

**`contains no fenix.mod.json`** — that jar is for another loader. A Fabric or
Forge mod will not run on Fenix, and no amount of renaming changes that.

**`requires fenix >=x, but y is installed`** — update Fenix, or use an older
build of the mod.

**`requires minecraft ~26.2`** — the mod was built for another game version.
Anything built against Minecraft says so in its file name: `+mc26.2`.

**Two mods with one id** — you have the same mod twice, probably two versions.
Delete one.

## When the server refuses you

> This server and your game do not have the same mods — missing: example-mod

The server checks on join and names what is wrong rather than letting you in to
break slowly. Install what it names, or ask whoever runs it.

This happens because block and item ids are assigned in order: one missing mod
shifts everything after it, and the results are wrong blocks and confusing
crashes that mention nothing useful.

## Removing Fenix

Delete the Fenix profile from the launcher. Nothing in `versions/26.2/` was ever
touched, so vanilla is exactly where you left it.
