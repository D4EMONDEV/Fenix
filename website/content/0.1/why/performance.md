---
title: Speed, and what it costs
description: What Fenix does quickly, what it refuses to do at all, and what has not been measured.
order: 2
---

A loader is not usually the slow part of a modded game — the mods are. What a
loader controls is startup, and how much work it makes every mod do afterwards.

## What is not done at all

The fastest work is work that does not happen.

**No remapping.** No name translation at load, no refmaps to resolve, no
remapped copy of the game to produce or cache.

**No classpath scanning.** Entry points come from a file the annotation
processor wrote at compile time. Nothing walks a jar looking for annotations.

**No reflection in the hot path.** A mod is instantiated once, at startup, and
called through an interface after that.

## What is done in parallel

Discovery reads every jar independently — open it, parse the manifest, unpack
any jars inside it. At a hundred mods that used to be a hundred file reads
waiting on each other for no reason.

Results are collected per jar and flattened afterwards, so the order stays the
sorted one. A log has to read in a stable order for anyone to follow it.

## Where the API spends its budget

**Events** dispatch through a lock-free array. Registering or removing a listener
copies it; firing does not allocate, and an event nobody listens to costs one
null check.

**Registration** is deferred. A mod declares content as fields, and Fenix
registers all of it at the one moment the game's registries are open. That is
about correctness first, but it also means nothing is done twice.

**Registry sync** sends a digest per registry rather than every id — a few
hundred bytes when a player joins, instead of a list that grows with the
modpack.

## What has not been measured

This page claims no number, because none has been taken.

The honest position is that Fenix does less work than a loader carrying a
mapping layer, and that "less work" has not been turned into a benchmark against
Fabric or NeoForge on the same modpack. Until it has, read the above as a
description of the design rather than a result.

:::note[If you benchmark it]
Startup time on a large modpack is the number worth having, and it would be
welcome as an issue on the repository — including if it is unflattering.
:::
