---
title: "api.log"
description: "Types in fr.d4emon.fenix.api.log"
sidebar:
  order: 11
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.api.log</code></p>

| Type | What it is |
|---|---|
| [`FenixLogger`](#fenixlogger) | A logger scoped to a single mod. |

## FenixLogger

A logger scoped to a single mod.

Obtained from `Fenix#logger()`. Every mod gets its own instance, so
its output is attributed to it without the mod having to name itself in every
message.

Messages use `{}` as a placeholder and are only formatted if the
level is enabled:

```java
logger.info("Loaded {} blocks in {} ms", count, elapsed);
```

If the last argument is a `Throwable` and has no matching
placeholder, its stack trace is logged after the message.

### `void debug(String message, Object[] arguments)`

Logs a message useful when diagnosing a problem.

### `void error(String message, Object[] arguments)`

Logs a failure.

### `void info(String message, Object[] arguments)`

Logs a message worth seeing during a normal run.

### `void trace(String message, Object[] arguments)`

Logs a message useful only when tracing execution step by step.

### `void warn(String message, Object[] arguments)`

Logs something unexpected that did not stop the mod from working.

