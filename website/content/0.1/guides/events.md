---
title: Events
description: Listen to what the game does, and sometimes stop it.
order: 4
---

An event is how a mod reacts to the game without editing it. Fenix fires them
from mixins it owns, so your mod stays ordinary Java: no injection points, no
method names that a game update can rename underneath you.

## Listening

Every event is a static field. Registering returns a `Subscription` you can keep
if you ever want to stop listening; most mods drop it.

```java title="src/main/java/com/example/mymod/MyMod.java"
import fr.d4emon.fenix.event.PlayerEvents;

@Override
public void onInit(Fenix fenix) {
    PlayerEvents.JOINED.register(joined ->
            joined.player().sendSystemMessage(Component.literal("Welcome.")));
}
```

The listener takes one argument: a record carrying everything the event knows.
That is the whole shape. A record rather than a long parameter list, because a
new field can be added to it without breaking every mod that already listens.

Register in `onInit`, not `onRegister`. `onRegister` is for content, runs before
the registries are frozen, and is not a moment when a server exists.

## The events

### Players

| Event | Fires |
|---|---|
| `PlayerEvents.JOINED` | A player finished connecting. |
| `PlayerEvents.LEFT` | A player disconnected. |
| `PlayerEvents.DIED` | A player died. Carries the `DamageSource`. |
| `PlayerEvents.RESPAWNED` | A player respawned. `endPortal()` distinguishes returning from the End from dying. |

### The world

| Event | Fires |
|---|---|
| `ServerEvents.STARTED` | The server is up and every level exists. |
| `ServerEvents.TICK_START` / `TICK_END` | Every server tick, twenty times a second. |
| `LevelEvents.LOADED` | A level is ready to be used. |
| `LevelEvents.SAVING` | A level is about to be written to disk — where to flush anything of your own. |
| `EntityEvents.DIED` | A living entity died. |
| `LootEvents.LOADING` | A loot table was read, and can be replaced before anything uses it. |

### Cancellable

Two events are `CancellableEvent` rather than `Event`. A listener returns a
`Flow` saying whether the game should carry on:

```java
import fr.d4emon.fenix.event.BlockEvents;
import fr.d4emon.fenix.event.Flow;

BlockEvents.BREAK.register(broken -> {
    if (broken.level().getBlockState(broken.pos()).is(Blocks.BEDROCK)) {
        return Flow.CANCEL;
    }
    return Flow.CONTINUE;
});
```

| Event | Cancelling it means |
|---|---|
| `BlockEvents.BREAK` | The block is not broken. |
| `BlockEvents.USE` | The right-click does nothing. |
| `EntityEvents.SPAWNING` | The entity is not added to the world. |

Cancelling stops the game's own handling **and** every listener after yours. That
is why the order matters, and why the next section exists.

## Order

Listeners run in priority order, highest first, and in registration order within
a priority.

```java
import fr.d4emon.fenix.event.Priority;

BlockEvents.BREAK.register(Priority.HIGH, broken -> { /* … */ });
```

`Priority` is five named ints — `HIGHEST`, `HIGH`, `NORMAL`, `LOW`, `LOWEST` —
and the method takes an `int`, so a mod that needs to sit between two of them
can. Default is `NORMAL`.

Use a high priority to *veto* something early, and a low one to *observe* what
survived. A mod that protects a region wants `HIGH`; a mod that counts blocks
broken wants `LOW`, so it does not count the ones a protection mod cancelled.

## Client events

Client events live in `fr.d4emon.fenix.event.client` and may only be touched
from the client half of a mod. `src/main` compiles against a Minecraft with the
client removed, so naming one there is a compile error — which is the point. See
[Client-side code](/docs/@latest/guides/client).

| Event | Fires |
|---|---|
| `ClientEvents.CONNECTED` / `DISCONNECTED` | This client joined or left a world. |
| `ClientEvents.TICK_START` / `TICK_END` | Every client tick. |
| `ItemTooltipEvents.BUILD` | A tooltip is being assembled, and lines can be added. |
| `HudRenderEvents.RENDER` | The HUD is being drawn. |
| `ClientBlockEvents.ATTACK` / `USE` | This client hit or right-clicked a block. Both cancellable. |

## Your own events

`Event.create()` and `CancellableEvent.create()` are public. A library mod that
wants other mods to react to it declares one the same way Fenix does:

```java
public final class ForgeEvents {

    /** Fires when a ritual completes. */
    public record Completed(ServerLevel level, BlockPos pos) { }

    public static final Event<Completed> COMPLETED = Event.create();

    private ForgeEvents() {
    }
}
```

and fires it with `COMPLETED.fire(new Completed(level, pos))`.

`hasListeners()` is there for the case where building the context is expensive —
gathering a list, walking a chunk — and nothing is listening:

```java
if (COMPLETED.hasListeners()) {
    COMPLETED.fire(new Completed(level, expensiveSurvey()));
}
```

### A listener that throws

It is skipped, the failure is logged with the listener named, and the event
carries on to the rest. An event is fired from inside Minecraft, so letting the
exception out would send it up the game's own call stack — a crash report
naming a vanilla method, and every listener registered after the broken one
never running.

For a `CancellableEvent`, a listener that threw counts as `CONTINUE`. It decided
nothing, and cancelling on its behalf would let one broken mod silently veto
everything the event guards.

`Error` is deliberately not caught. An `OutOfMemoryError` is not a listener
misbehaving, and carrying on around it helps nobody.

This is containment, not forgiveness: the log line is at `ERROR` with the full
stack trace, because a mod that silently stops working is worse than one that
crashes.

### Client block events

`ClientBlockEvents.ATTACK` and `USE` are the client's own view of hitting and
right-clicking a block, both cancellable. They fire before the client sends
anything to the server, so cancelling one stops the packet as well as the local
prediction. They are not a substitute for `BlockEvents` — a client can be lied
to, and anything that matters belongs on the server.
