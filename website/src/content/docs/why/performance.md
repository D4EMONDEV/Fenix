---
title: Speed
description: Where Fenix spends time, and where it refuses to.
---

Fenix is not fast because it is optimised. It is fast because a game that ships
unobfuscated, and a loader that decides things while your code compiles, leave
much less to do at startup.

<ul class="figures">
  <li><strong>0.4s</strong><span>to load 2 000 classes</span></li>
  <li><strong>0</strong><span>classes scanned at startup</span></li>
  <li><strong>0</strong><span>remapping steps</span></li>
</ul>

## Nothing is discovered at runtime

Other loaders find your mod by scanning: every class in every jar is opened and
read looking for an annotation, before the game starts. That cost grows with
every mod installed, and it is paid on every launch.

Fenix's annotation processor writes the answer into your jar while it compiles:

```json title="fenix.index.json"
{ "schema": 1, "mods": { "example-mod": "com.example.ExampleMod" } }
```

The loader reads one small file per mod. Startup stops caring how large the mods
are, and a mod class the processor could not validate is a `javac` error rather
than a mod that quietly never loads.

## No mappings, anywhere

Minecraft has shipped unobfuscated since 26.1. Loaders built for the years
before that carry a remapping pipeline: mod jars are rewritten at install or at
launch, mappings are downloaded and cached, and mixin refmaps translate names at
runtime.

Fenix has none of it. You write `BlockBehaviour.Properties`, the class file says
`BlockBehaviour.Properties`, and nothing translates anything. That removes a
build step, a cache, a download, and an entire category of "works in dev, breaks
in production".

## A classloader that keeps its files open

An early build closed each jar after reading it, which is tidy and, on Windows,
catastrophic: reopening the archive per class turned a launch into **163
seconds**. Keeping the handles open brought the same work to **0.4 seconds** —
a 400× difference from one decision about file handles.

The cost is that jars stay locked while the game runs, which is correct: a mod
jar replaced under a running game is not a feature.

## Dispatch that allocates nothing

The event bus takes no lock and allocates nothing per fire. Registering rebuilds
a sorted array behind a `volatile`, so listeners can be added or removed during a
dispatch and simply take effect from the next one.

Events fire every tick, twenty times a second, for the life of a session. A
`synchronized` block there is a promise to be slow for hours.

## What Fenix will not do to be fast

**Silently remap ids.** When a client and a server disagree on registries, the
join is refused with the mod named. Remapping to paper over it trades a
confusing disconnect for a world that corrupts slowly.

**Skip vanilla's bookkeeping.** Registering a block redoes the passes vanilla
runs over its own — state ids, item mapping, block entity validity. Skipping
them is faster and produces crashes far from their cause.
