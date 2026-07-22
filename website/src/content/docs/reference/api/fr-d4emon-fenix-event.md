---
title: "event"
description: "Types in fr.d4emon.fenix.event"
sidebar:
  order: 30
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.event</code></p>

| Type | What it is |
|---|---|
| [`BlockEvents`](#blockevents) | What players do to blocks, decided by the server. |
| [`BlockEvents.Break`](#blockevents-break) | A player about to break a block. |
| [`BlockEvents.Use`](#blockevents-use) | A player about to right-click a block. |
| [`CancellableEvent`](#cancellableevent) | An event whose listeners can stop the thing from happening. |
| [`CancellableEvent.Listener`](#cancellableevent-listener) | A listener on a `CancellableEvent`. |
| [`EntityEvents`](#entityevents) | Entities entering and leaving the world. |
| [`EntityEvents.Died`](#entityevents-died) | A living entity that has died. |
| [`EntityEvents.Spawning`](#entityevents-spawning) | An entity about to be added to a level. |
| [`Event`](#event) | Something that happened, which listeners are told about. |
| [`Event.Listener`](#event-listener) | A listener on an `Event`. |
| [`Flow`](#flow) | What a listener wants to happen next. |
| [`LevelEvents`](#levelevents) | Worlds being loaded and written out. |
| [`LevelEvents.Of`](#levelevents-of) | A level, in whatever way the event means it. |
| [`PlayerEvents`](#playerevents) | Players arriving, leaving and dying. |
| [`PlayerEvents.Died`](#playerevents-died) | A player who has died. |
| [`PlayerEvents.Joined`](#playerevents-joined) | A player who has just arrived and can be sent things. |
| [`PlayerEvents.Left`](#playerevents-left) | A player on their way out. |
| [`PlayerEvents.Respawned`](#playerevents-respawned) | A player who has just respawned. |
| [`Priority`](#priority) | Conventional listener priorities. |
| [`ServerEvents`](#serverevents) | The server's lifecycle and heartbeat. |
| [`ServerEvents.Started`](#serverevents-started) | A server that has just come up. |
| [`ServerEvents.Tick`](#serverevents-tick) | One server tick. |
| [`Subscription`](#subscription) | A registered listener, which can be taken back off the event. |

## BlockEvents

What players do to blocks, decided by the server.

These are the events to use for anything that must actually hold — block
protection, claims, permissions. The server has the final say, so cancelling
here really stops the action, for every player, including someone using a
modified client.

Their client-side counterparts in
`fr.d4emon.fenix.event.client.ClientBlockEvents` exist to give instant
feedback and avoid the visual rubber-band of a block breaking and coming
back — never as the enforcement point.

**Constants**

| Name | What it is |
|---|---|
| `BREAK` | Fires before a block is broken. |
| `USE` | Fires before a block is used. |

## BlockEvents.Break

A player about to break a block.

### `boolean equals(Object o)`

### `int hashCode()`

### `ServerLevel level()`

### `ServerPlayer player()`

### `BlockPos pos()`

### `String toString()`

## BlockEvents.Use

A player about to right-click a block.

### `boolean equals(Object o)`

### `InteractionHand hand()`

### `int hashCode()`

### `BlockHitResult hit()`

### `Level level()`

### `ServerPlayer player()`

### `String toString()`

## CancellableEvent

An event whose listeners can stop the thing from happening.

Same shape as `Event`, except a listener returns a `Flow`:

```java
public record BlockBreak(Player player, BlockPos pos) { }

public static final CancellableEvent<BlockBreak> BREAK = CancellableEvent.create();
```

```java
BlockEvents.BREAK.register(break -> isProtected(break.pos()) ? Flow.CANCEL : Flow.CONTINUE);
```

The first listener to cancel ends the dispatch — the remaining listeners
are not called, because once the action is off the table there is nothing
left for them to decide. A listener that only wants to <em>observe</em>
every occurrence should sit on a plain `Event`, or register at
`Priority#HIGHEST` to run before anyone can cancel.

Firing returns the outcome, which is what the caller acts on:

```java
if (BlockEvents.BREAK.fire(new BlockBreak(player, pos)).isCancelled()) {
   callback.cancel();
}
```

### `static CancellableEvent<C> create()`

Creates an event.

### `Flow fire(C context)`

Asks every listener, in priority order, stopping at the first refusal.

### `boolean hasListeners()`

{@return whether anything is listening}

For skipping expensive work needed only to build the context.

### `Subscription register(CancellableEvent.Listener<C> listener)`

Registers a listener at `Priority#NORMAL`.

### `Subscription register(int priority, CancellableEvent.Listener<C> listener)`

Registers a listener.

## CancellableEvent.Listener

A listener on a `CancellableEvent`.

### `Flow on(C context)`

Called when the event fires.

## EntityEvents

Entities entering and leaving the world.

Server-side: the client is told about entities that already exist, and a
mod that acted on the client's copy would be acting on a shadow.

**Constants**

| Name | What it is |
|---|---|
| `SPAWNING` | Fires before an entity joins a level; cancelling keeps it out. |
| `DIED` | Fires when anything living dies, before its drops are decided. |

## EntityEvents.Died

A living entity that has died. Players included.

### `DamageSource cause()`

### `LivingEntity entity()`

### `boolean equals(Object o)`

### `int hashCode()`

### `String toString()`

## EntityEvents.Spawning

An entity about to be added to a level.

Cancelling stops it being added at all — which is how a mod refuses a
spawn rather than removing the entity a tick later, after it has already
been seen.

### `Entity entity()`

### `boolean equals(Object o)`

### `int hashCode()`

### `ServerLevel level()`

### `String toString()`

## Event

Something that happened, which listeners are told about.

An event carries a single <em>context</em> — normally a record holding
everything the listener needs. That is what keeps declaring an event to two
lines instead of a hand-written functional interface and a combiner per
event:

```java
public record ClientTick(Minecraft client) { }

public static final Event<ClientTick> TICK_END = Event.create();
```

and listening to one line:

```java
ClientEvents.TICK_END.register(tick -> doSomething(tick.client()));
```

Adding a parameter later is a change to the record, not a break in every
listener's signature.

This event <strong>cannot be cancelled</strong> — every listener always
runs. That is a promise in the type: if an event can be stopped, it is a
`CancellableEvent` and its listeners must say so by returning a
`Flow`. There is no way to "return CANCEL" here and quietly have
nothing happen.

Safe to register, unsubscribe and fire from any thread. Listeners run on
whichever thread fired the event — usually the game thread, so treat them as
game code.

### `static Event<C> create()`

Creates an event.

### `void fire(C context)`

Tells every listener, in priority order.

### `boolean hasListeners()`

{@return whether anything is listening}

For skipping expensive work needed only to build the context.

### `Subscription register(Event.Listener<C> listener)`

Registers a listener at `Priority#NORMAL`.

### `Subscription register(int priority, Event.Listener<C> listener)`

Registers a listener.

## Event.Listener

A listener on an `Event`.

### `void on(C context)`

Called when the event fires.

## Flow

What a listener wants to happen next.

Returned by listeners of a `CancellableEvent`. A listener that
cancels stops the game's own action <em>and</em> the remaining listeners:
once something has decided the action must not happen, asking the rest is
meaningless.

**Constants**

| Name | What it is |
|---|---|
| `CONTINUE` | Let the action proceed, and let the remaining listeners see the event. |
| `CANCEL` | Stop the action, and stop dispatching to further listeners. |

### `boolean isCancelled()`

{@return whether this is {@link #CANCEL}}

### `static Flow valueOf(String name)`

### `static Flow[] values()`

## LevelEvents

Worlds being loaded and written out.

A server has several — the overworld, the nether, the end, and any a mod
or datapack adds — so these fire once per level rather than once per server.

**Constants**

| Name | What it is |
|---|---|
| `LOADED` | Fires when a level is ready to be used. |
| `SAVING` | Fires as a level is written to disk. |

## LevelEvents.Of

A level, in whatever way the event means it.

### `boolean equals(Object o)`

### `int hashCode()`

### `ServerLevel level()`

### `String toString()`

## PlayerEvents

Players arriving, leaving and dying.

Server-side, all of them. A client knows when <em>it</em> joined and
nothing about anybody else's session, so anything that has to be true for
every player belongs here.

**Constants**

| Name | What it is |
|---|---|
| `JOINED` | Fires once the player is in the world and their connection can carry payloads. |
| `LEFT` | Fires while the player is still readable. |
| `DIED` | Fires when a player dies, before their items are scattered. |
| `RESPAWNED` | Fires when a player comes back, on a new player object. |

## PlayerEvents.Died

A player who has died.

### `DamageSource cause()`

### `boolean equals(Object o)`

### `int hashCode()`

### `ServerPlayer player()`

### `String toString()`

## PlayerEvents.Joined

A player who has just arrived and can be sent things.

### `boolean equals(Object o)`

### `int hashCode()`

### `ServerPlayer player()`

### `String toString()`

## PlayerEvents.Left

A player on their way out.

Fired while they are still on the server, so their inventory and
position can still be read — a moment later there is nothing to read.

### `boolean equals(Object o)`

### `int hashCode()`

### `ServerPlayer player()`

### `String toString()`

## PlayerEvents.Respawned

A player who has just respawned.

The player is a <em>new</em> object: respawning replaces it rather
than resetting it, which is why anything a mod attached to the old one is
gone and has to be put back here.

### `boolean endPortal()`

### `boolean equals(Object o)`

### `int hashCode()`

### `ServerPlayer player()`

### `String toString()`

## Priority

Conventional listener priorities.

A <strong>higher</strong> value runs earlier, which is the way round most
people guess. Any `int` works; these constants are spaced a thousand
apart so a mod can slot itself between two of them without inventing a scale.

Listeners registered at the same priority run in registration order, so a
mod that registers twice keeps its own ordering.

**Constants**

| Name | What it is |
|---|---|
| `HIGHEST` | Runs before everything else. |
| `HIGH` | Runs early. |
| `NORMAL` | The default. |
| `LOW` | Runs late. |
| `LOWEST` | Runs after everything else. |

## ServerEvents

The server's lifecycle and heartbeat.

These fire on a dedicated server and on the integrated server inside a
single-player game — the server is where the world actually lives, so
anything authoritative belongs here rather than in
`fr.d4emon.fenix.event.client.ClientEvents`.

**Constants**

| Name | What it is |
|---|---|
| `STARTED` | Fires once per server, on its first tick — the world is loaded and the server is running. |
| `TICK_START` | Fires before each server tick. |
| `TICK_END` | Fires after each server tick. |

## ServerEvents.Started

A server that has just come up.

### `boolean equals(Object o)`

### `int hashCode()`

### `MinecraftServer server()`

### `String toString()`

## ServerEvents.Tick

One server tick.

### `boolean equals(Object o)`

### `int hashCode()`

### `MinecraftServer server()`

### `String toString()`

## Subscription

A registered listener, which can be taken back off the event.

Every `register` call returns one. Being able to unsubscribe is
deliberate: a listener that is only relevant while a screen is open, a world
is loaded, or a feature is enabled should not have to guard itself on every
single dispatch forever.

It extends `AutoCloseable` with no checked exception, so it works in
try-with-resources and as a method reference. Closing twice is harmless.

### `void close()`

Removes the listener from its event. Idempotent.

