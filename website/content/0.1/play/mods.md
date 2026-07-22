---
title: Adding mods
description: Where mods go, what the loader refuses, and how to read what it says.
order: 2
---

## Where mods go

`.minecraft/mods`, the same as every other loader. Fenix looks at the top level
of that folder only, and considers every `.jar` in it.

Renaming a file to `.jar.disabled` keeps it out of a launch without deleting it.

## The API

Most mods need `fenix-api`. It is one file that carries every API module inside
it, so you install one jar rather than eight:

```
mods/
  fenix-api-0.1.3+mc26.2.jar
  some-mod-1.2.0.jar
```

## When something is wrong

Fenix refuses to start rather than starting badly, and says why. The report is
written to **`fenix-launch-error.txt`** next to the game, and shown in a window
if there is a screen:

```
Fenix cannot start because of 2 mod problems:
  - duplicate mod 'coollib': both a.jar and b.jar provide it — remove one of them
  - some-mod 1.2.0 requires fenix-api >=0.2.0, but fenix-api 0.1.3 is present
```

Two things it deliberately does **not** treat as errors:

- **Two mods carrying the same library inside them.** Neither author chose that
  and neither can fix it; the newer copy wins.
- **A mod for the wrong side.** A client-only mod on a server is skipped and
  named in the log, not a crash.

## Seeing what is loaded

In game, `/fenix mods` lists everything, in load order — which is the order that
matters when two mods disagree.

```
Fenix 0.1.1 — 9 mods loaded
  fenix-api-core 0.1.0+mc26.2
  fenix-api-event 0.1.0+mc26.2
  …
```

`/fenix mods <id>` shows one mod's authors, licence and description.

## Joining a server

The server states what it has when you join, and a mismatch is a disconnect that
names the mod you are missing. Fenix never silently remaps ids: a client let in
with shifted ids sees the wrong blocks, and nothing in that mentions the mod at
fault.
