---
title: Commands
description: Brigadier, without the parts nobody enjoys.
order: 10
---

Commands are Brigadier underneath, and Fenix does not hide that — it removes the
generic parameters and the permission plumbing that make a simple command read
like a type puzzle.

```java title="src/main/java/com/example/mymod/ModCommands.java"
package com.example.mymod;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import fr.d4emon.fenix.command.CommandEvents;
import net.minecraft.network.chat.Component;

import static fr.d4emon.fenix.command.Commands.argument;
import static fr.d4emon.fenix.command.Commands.literal;
import static fr.d4emon.fenix.command.Commands.operator;
import static fr.d4emon.fenix.command.Commands.run;

public final class ModCommands {

    private ModCommands() {
    }

    public static void register() {
        CommandEvents.REGISTER.register(registration -> registration.dispatcher().register(
                literal("wisp")
                        .requires(operator())
                        .then(argument("count", IntegerArgumentType.integer(1, 20))
                                .executes(run(context -> {
                                    int count = IntegerArgumentType.getInteger(context, "count");
                                    spawn(context.getSource(), count);
                                })))
                        .executes(run(context -> spawn(context.getSource(), 1)))));
    }
}
```

Call `register()` once from `onInit`.

## Why an event and not a call

`CommandEvents.REGISTER` fires when the server starts **and again on every
datapack reload**, because a reload rebuilds the dispatcher from nothing. A
command registered any other way disappears the first time somebody runs
`/reload`, and nothing says why.

Registering the listener once covers both. That is the only reason this is an
event.

## The helpers

| | |
|---|---|
| `literal("name")` | A fixed word in the command. |
| `argument("name", type)` | A value the player types. |
| `run(context -> …)` | Wraps a body that returns nothing. Brigadier wants an `int`; this returns success for you. |
| `operator()` | Requires permission level 2 — the usual bar for a cheat-like command. |
| `requires(permission)` | Requires a named permission, for a server running a permissions mod. |

`run` also lets the body throw `CommandSyntaxException`, so a command that
rejects its own input reads normally rather than having to return an error code.

## Sending a reply

```java
context.getSource().sendSuccess(() -> Component.literal("Done"), false);
```

The supplier is not decoration: Minecraft only builds the message if somebody
will see it. The `false` is whether to also tell every operator — `true` for
something that changed the world, `false` for a query.

To fail, throw:

```java
throw new SimpleCommandExceptionType(Component.literal("Too far away")).create();
```

## Argument types of your own

A command that takes one of *your* things — an ore, a spell, a machine — can
have its own argument type, with completion and validation, rather than taking a
string and parsing it.

Register the type alongside your content:

```java
public static final Holder<ArgumentTypeInfo<?, ?>> ORE =
        REGISTRAR.commandArgument("ore", OreArgument.class,
                SingletonArgumentInfo.contextFree(OreArgument::ore));
```

Registering it is what makes completion work on a **multiplayer** client: the
server sends the command tree to the client, and a type the client cannot
resolve degrades to a plain string with no suggestions. An unregistered type
works fine in single-player and quietly stops suggesting the moment somebody
plays on a server.
