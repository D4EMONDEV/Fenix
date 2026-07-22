---
title: "config"
description: "Types in fr.d4emon.fenix.config"
sidebar:
  order: 60
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.config</code></p>

| Type | What it is |
|---|---|
| [`Config`](#config) | A mod's settings, as a record. |
| [`ConfigException`](#configexception) | A configuration file that cannot be read, naming the file and the field. |

## Config

A mod's settings, as a record.

```java
public record Settings(boolean spawnWisps, int maxWisps, Difficulty floor) {
}

private static final Config<Settings> CONFIG =
       Config.of(fenix, new Settings(true, 20, Difficulty.EASY));

if (CONFIG.get().spawnWisps()) { … }
```

The record is the schema, the defaults and the documentation at once: its
component names are the file's keys, its types decide what a value may be,
and the instance you pass is what a missing setting falls back to. There is
no separate spec to keep in step with it.

Validation belongs in the record's compact constructor, where it can be
written once and cannot be skipped:

```java
public record Settings(int maxWisps) {
   public Settings {
       if (maxWisps < 1) {
           throw new IllegalArgumentException("maxWisps must be at least 1");
       }
   }
}
```

That message reaches the player prefixed with the file and field, rather
than as a stack trace.

The file is rewritten after every load, so a setting added by an update
appears with its default rather than staying invisible until somebody reads
the changelog.

### `Path file()`

{@return where the file is, for a mod that wants to say so}

### `T get()`

{@return the settings as they were last read}

Safe to call from any thread and cheap enough to call in a loop: it
reads a field, and the record it returns cannot change underneath.

### `static Config<T> of(Fenix fenix, T defaults)`

Loads a mod's settings from `config.json`.

### `static Config<T> of(Fenix fenix, String name, T defaults)`

Loads a named settings file, for a mod with more than one.

### `void reload()`

Reads the file again, and writes it back complete.

Called once by `#of`; call it again after a command that lets a
player edit the file and reload without restarting.

## ConfigException

A configuration file that cannot be read, naming the file and the field.

Unchecked on purpose: a mod cannot sensibly recover from its own
configuration being wrong, and the useful thing is a message a player can act
on rather than a catch block that hides it.

