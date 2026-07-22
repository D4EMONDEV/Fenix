---
title: "command"
description: "Types in fr.d4emon.fenix.command"
sidebar:
  order: 50
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.command</code></p>

| Type | What it is |
|---|---|
| [`CommandEvents`](#commandevents) | When to add commands. |
| [`CommandEvents.Registration`](#commandevents-registration) | What a listener is given. |
| [`Commands`](#commands) | Commands, without the parts of Brigadier nobody enjoys. |
| [`Commands.Body`](#commands-body) | A command body. |

## CommandEvents

When to add commands.

```java
CommandEvents.REGISTER.register(registration ->
       registration.dispatcher().register(literal("hello").executes(run(context -> {
           context.getSource().sendSuccess(() -> Component.literal("hi"), false);
       }))));
```

Fired once per command tree, which the server builds on start and rebuilds
whenever datapacks reload — so a listener runs more than once per session and
must add rather than accumulate.

**Constants**

| Name | What it is |
|---|---|
| `REGISTER` | The command tree is open for additions. |

## CommandEvents.Registration

What a listener is given.

### `CommandBuildContext context()`

### `CommandDispatcher<CommandSourceStack> dispatcher()`

### `boolean equals(Object o)`

### `int hashCode()`

### `Commands.CommandSelection selection()`

### `String toString()`

## Commands

Commands, without the parts of Brigadier nobody enjoys.

```java
CommandEvents.REGISTER.register(registration -> registration.dispatcher().register(
       literal("home")
               .requires(Commands.operator())
               .then(argument("name", StringArgumentType.word())
                       .executes(run(context -> teleport(context.getSource().getPlayerOrException(),
                               StringArgumentType.getString(context, "name")))))));
```

Fenix's own, not vanilla's: use this and you never need the other. It is a
shortcut over Brigadier, never a wall in front of it — every builder here is
Brigadier's own, so anything not covered is still reachable.

What it removes is the `return 1`. Brigadier's `executes` wants
an int nobody reads, and forgetting it is a compile error whose message says
nothing about commands. `#run` takes a body that returns nothing.

### `static RequiredArgumentBuilder<CommandSourceStack,T> argument(String name, ArgumentType<T> type)`

Starts an argument.

### `static LiteralArgumentBuilder<CommandSourceStack> literal(String name)`

Starts a literal, like the `home` in `/home`.

### `static PermissionProviderCheck<CommandSourceStack> operator()`

{@return a requirement that the caller may run operator commands}

The same permission `/gamemode` asks for. Minecraft 26.2 replaced
the old numeric levels with named permissions, so this is a name rather
than the `hasPermission(2)` that appears in every older mod and
means nothing on sight.

### `static PermissionProviderCheck<CommandSourceStack> requires(Permission permission)`

{@return a requirement that the caller holds a permission}

### `static Command<CommandSourceStack> run(Commands.Body body)`

Wraps a command body that returns nothing.

## Commands.Body

A command body.

Allowed to throw `CommandSyntaxException` because the methods
that read arguments back out do, and because Brigadier catches it and
shows the player what went wrong. Wrapping every one of those calls in a
try/catch would only turn a good message into a crash.

### `void run(CommandContext<CommandSourceStack> context)`

Runs the command.

