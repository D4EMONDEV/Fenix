---
title: "loader.log"
description: "Types in fr.d4emon.fenix.loader.log"
sidebar:
  order: 91
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.loader.log</code></p>

| Type | What it is |
|---|---|
| [`ConsoleLogger`](#consolelogger) | The fallback logging backend: plain lines on the standard streams. |

## ConsoleLogger

The fallback logging backend: plain lines on the standard streams.

Used when nothing better is available — once the loader runs inside the
real game, an slf4j-backed logger takes over so mod output lands in the
game's log files. The format is `[name/LEVEL] message`.

`trace` and `debug` are silent unless the `fenix.debug`
system property is set.

### `void debug(String message, Object[] arguments)`

### `void error(String message, Object[] arguments)`

### `void info(String message, Object[] arguments)`

### `void trace(String message, Object[] arguments)`

### `void warn(String message, Object[] arguments)`

