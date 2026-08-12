---
title: Configuration
description: Settings as a record, checked by the compiler and by you.
order: 11
---

A config is a record. The field names are the JSON keys, the types are the
parsing, and every setting read anywhere is a field the compiler knows about —
so a renamed setting is a compile error rather than a `null` at run time.

```java title="src/main/java/com/example/mymod/ModConfig.java"
package com.example.mymod;

import net.minecraft.world.Difficulty;

public record ModConfig(boolean spawnWisps, int maxWisps, Difficulty floor) {

    /** Written the first time the mod runs. */
    public static final ModConfig DEFAULTS = new ModConfig(true, 20, Difficulty.EASY);

    /** @throws IllegalArgumentException if the file holds a value that cannot work */
    public ModConfig {
        if (maxWisps < 1) {
            throw new IllegalArgumentException("maxWisps must be at least 1");
        }
    }
}
```

```java title="in your mod class"
private Config<ModConfig> config;

@Override
public void onInit(Fenix fenix) {
    config = Config.of(fenix, ModConfig.DEFAULTS);
    if (config.get().spawnWisps()) {
        // …
    }
}
```

`Config.of` reads the file, fills in anything missing from the defaults, writes
it back, and hands you the record. Read it in `onInit`: the config directory
exists by then, and reading once at startup beats reading on every use.

## Where the file goes

`run/config/<mod-id>/config.json` in development, and the same path under the
game directory in a real installation. `config.file()` gives you the path if you
need to point a player at it.

A second file is a second call:

```java
Config<Balance> balance = Config.of(fenix, "balance", Balance.DEFAULTS);
```

## What a field may be

| Type | In JSON |
|---|---|
| `boolean` | `true` / `false` |
| any number type | a number |
| `String` | a string |
| an `enum` | the constant's name, as written in Java |
| a nested `record` | an object |
| `List<…>` of any of the above | an array |

An enum is the reason to prefer one over a string: a typo in the file is caught
at load, with the field named, rather than becoming a silent fallback.

## Rejecting bad values

The compact constructor is where a value that parses but makes no sense is
refused. It runs on load, so the failure names the file and the field — not the
distant place that would eventually have divided by it.

Prefer clamping to throwing when a wrong value is merely awkward, and throwing
when it would corrupt something. A `maxWisps` of `0` is a config the player
should be told about; a `0` silently treated as `1` is a config they will
believe works.

## Reloading

`config.reload()` re-reads from disk. Nothing calls it for you — wire it to a
command if your mod wants live reloading:

```java
literal("mymod").then(literal("reload").requires(operator()).executes(run(context -> {
    config.reload();
    context.getSource().sendSuccess(() -> Component.literal("Reloaded"), true);
})));
```

Note the shape this forces: anything holding a copy of a setting instead of
asking `config.get()` each time will not see the new value. Read through the
`Config`, not around it.
