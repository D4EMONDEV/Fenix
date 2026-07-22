---
title: "event.client"
description: "Types in fr.d4emon.fenix.event.client"
sidebar:
  order: 31
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.event.client</code></p>

| Type | What it is |
|---|---|
| [`ClientBlockEvents`](#clientblockevents) | What the player is trying to do to a block, on their own machine. |
| [`ClientBlockEvents.Attack`](#clientblockevents-attack) | The player starting to break a block. |
| [`ClientBlockEvents.Use`](#clientblockevents-use) | The player right-clicking a block. |
| [`ClientEvents`](#clientevents) | The client's heartbeat. |
| [`ClientEvents.Tick`](#clientevents-tick) | One client tick. |

## ClientBlockEvents

What the player is trying to do to a block, on their own machine.

These fire before the client tells the server anything, so cancelling
gives instant feedback and avoids the block visually breaking and snapping
back when the server refuses.

<strong>Never the enforcement point.</strong> A modified client can skip
these entirely. Anything that must actually hold belongs in
`fr.d4emon.fenix.event.BlockEvents`, on the server; use these as well
only to make the refusal feel immediate.

**Constants**

| Name | What it is |
|---|---|
| `ATTACK` | Fires before the client starts breaking a block. |
| `USE` | Fires before the client uses a block. |

## ClientBlockEvents.Attack

The player starting to break a block.

### `boolean equals(Object o)`

### `Direction face()`

### `int hashCode()`

### `LocalPlayer player()`

### `BlockPos pos()`

### `String toString()`

## ClientBlockEvents.Use

The player right-clicking a block.

### `boolean equals(Object o)`

### `InteractionHand hand()`

### `int hashCode()`

### `BlockHitResult hit()`

### `LocalPlayer player()`

### `String toString()`

## ClientEvents

The client's heartbeat.

Client only — this package is never loaded on a dedicated server. For
anything authoritative, use `fr.d4emon.fenix.event.ServerEvents`
instead; a client tick is for rendering, input and local state.

**Constants**

| Name | What it is |
|---|---|
| `TICK_START` | Fires before each client tick. |
| `TICK_END` | Fires after each client tick. |

## ClientEvents.Tick

One client tick.

### `Minecraft client()`

### `boolean equals(Object o)`

### `int hashCode()`

### `String toString()`

