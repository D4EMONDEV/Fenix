---
title: Compared to other loaders
description: What Fenix does differently from Fabric, Forge and NeoForge — and
  what they do better.
slug: 0.1/why/comparison
---

Fabric, Forge and NeoForge are older, larger and far better tested than Fenix.
This page is about what is *different*, not about who wins.

## At a glance

| | Fenix | Fabric | Forge | NeoForge |
|---|---|---|---|---|
| Mods found by | compiling | scanning | scanning | scanning |
| Remapping | <span class="no">none</span> | at build & launch | at build | at build |
| Sidedness enforced by | <span class="yes">the compiler</span> | source sets | `@OnlyIn` | `@OnlyIn` |
| Data generation | <span class="yes">built in</span> | built in | built in | built in |
| Config | <span class="yes">built in</span> | <span class="no">third party</span> | built in | built in |
| Registry mismatch | <span class="yes">named refusal</span> | <span class="part">partial</span> | refusal | refusal |
| API install | <span class="yes">one jar</span> | one jar | bundled | bundled |
| Maturity | <span class="no">pre-1.0</span> | <span class="yes">years</span> | <span class="yes">a decade</span> | <span class="yes">years</span> |

## Where Fenix is genuinely different

### Sidedness is a compile error

Every loader has the same rule: client-only classes must not load on a server.
Forge and NeoForge express it with `@OnlyIn` annotations and trust you. Fabric
splits source sets, which is real enforcement and opt-in.

Fenix compiles common code against Minecraft with the client half **removed**:

```java title="src/main/java — common"
Minecraft.getInstance();
// error: package net.minecraft.client does not exist
```

The failure this prevents is the worst kind: it works on your machine, because
you develop on a client, and breaks on somebody else's dedicated server.

### No mappings at all

Loaders that predate 26.1 carry a remapping pipeline for an obfuscated game.
Fenix targets a game that has shipped unobfuscated since 26.1, so there is no
remapping, no mappings download, and no mixin refmaps to go stale.

This is not cleverness — it is arriving late enough that the problem was gone.

### Config without a third-party library

Fabric leaves configuration to community libraries, so every mod picks a
different one and a modpack ends up with four. Fenix has one, typed, backed by
records.

### The registrar absorbs vanilla's bookkeeping

Vanilla does work *around* registering its own content that a mod bypasses:
block-state network ids, `Item.BY_BLOCK`, block entity validity. Skip any of it
and the crash arrives later, inside vanilla, naming nothing useful.

Fenix does that work for your content. Other loaders do some of it; Fenix has
each case pinned by a check that boots the real game and fails if it regresses.

## Where the others are ahead

**Everything else.** Fabric has thousands of mods, Forge has a decade of them,
and both have solved problems Fenix has not met yet. There is no ecosystem here,
no mod you want to play with, and no release.

Fenix also targets exactly one Minecraft version. The others follow the game
across versions, which is most of the work and none of the fun.

**Choose Fenix** if you are writing a mod for 26.2 and want the compiler to
catch what other loaders find at runtime. **Choose the others** to play.
