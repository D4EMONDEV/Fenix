---
title: "loader.resolve"
description: "Types in fr.d4emon.fenix.loader.resolve"
sidebar:
  order: 91
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.loader.resolve</code></p>

| Type | What it is |
|---|---|
| [`ModResolver`](#modresolver) | Turns discovered mods into a load order, or explains why it cannot. |
| [`ResolutionException`](#resolutionexception) | Thrown when the discovered mods cannot be turned into a load order. |
| [`ResolutionResult`](#resolutionresult) | A successful resolution. |

## ModResolver

Turns discovered mods into a load order, or explains why it cannot.

Resolution runs in four steps, and the first three collect problems
instead of stopping, so a single `ResolutionException` names everything
at once:

<ol>
<li>Mods declaring a side this process is not on are set aside. That is
   normal — a shared modpack carries client-only mods onto the server — so
   it is never an error, but a mod <em>depending</em> on one of them is.</li>
<li>Ids must be unique, and must not claim one of the built-in ids.</li>
<li>Every dependency must be present in a version inside the declared
   range.</li>
<li>Mods are ordered so dependencies always come first. Ties are broken
   alphabetically by id, which makes the order a function of the mod set
   alone — the same mods load the same way on every machine, whatever order
   the file system listed them in.</li>
</ol>

### `static ResolutionResult resolve(Collection<ModCandidate> candidates, Side side, Map<String,Version> builtins)`

Resolves a load order.

## ResolutionException

Thrown when the discovered mods cannot be turned into a load order.

Resolution never gives up at the first problem: it checks everything, then
throws once with the complete list. A player fixing a modpack gets one
message naming every missing dependency, instead of a launch-crash loop that
reveals them one at a time.

### `List<String> problems()`

{@return every problem, one entry each}

## ResolutionResult

A successful resolution.

### `boolean equals(Object o)`

### `int hashCode()`

### `List<ModCandidate> loadOrder()`

### `List<ModCandidate> skipped()`

### `String toString()`

