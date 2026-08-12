---
title: Mixins and access
description: Editing the game itself, and the four rules that make it work.
order: 12
---

A mixin edits a Minecraft class as it loads. Fenix's own API is built out of
them, and your mod can write one for the cases the API has no event for.

Reach for one last. An injection binds to a method under the name and signature
it has today, and Minecraft renames things every release. An event survives an
update; an injection has to be looked at again. Everything the API already
covers — [events](/docs/@latest/guides/events), block interaction, loot — is a
mixin somebody already maintains.

## Declaring one

Three pieces: the dependency, the config file, and the metadata entry that loads
it.

```kotlin title="build.gradle.kts"
dependencies {
    // Provided by the loader at run time; this is only for compiling.
    compileOnly("net.fabricmc:sponge-mixin:0.17.3+mixin.0.8.7")
}
```

```json title="src/main/resources/my-mod.mixins.json"
{
  "required": true,
  "minVersion": "0.8.7",
  "package": "com.example.mymod.mixin",
  "compatibilityLevel": "JAVA_25",
  "injectors": { "defaultRequire": 1 },
  "mixins": ["MinecraftServerMixin"],
  "client": ["MinecraftMixin"],
  "server": []
}
```

```json title="src/main/resources/fenix.mod.json"
{
  "mixins": ["my-mod.mixins.json"]
}
```

`mixins` is loaded everywhere, `client` only on a client, `server` only on a
dedicated server. A client-only mixin listed under `mixins` fails to apply on a
server, and `"required": true` turns that into a refusal to start.

## The four rules

### The package belongs to Mixin

A config owns **every** class under the package it declares. Put a helper class
in `com.example.mymod.mixin` and it fails to load, with an error about a class
that is not a mixin. Helpers go anywhere else.

### Never cast to a mixin class

```java
// Throws IllegalClassLoadError at run time.
MinecraftServerMixin self = (MinecraftServerMixin) (Object) server;
```

A mixin class does not exist at run time — it is merged into its target and then
discarded. To reach the target, cast through `Object` to the *target* type:

```java
MinecraftServer server = (MinecraftServer) (Object) this;
```

To hand your own methods to other code, put them on an interface the target
implements — see [duck typing](#duck-typing) below.

### Added members are prefixed and `@Unique`

```java
@Unique
private boolean mymod$announced;
```

Two mods mixing into the same class would otherwise both add `announced`, and
one would silently win. The prefix is conventionally your mod id.

### `remap = false`

```java
@Inject(method = "tickServer", at = @At("HEAD"), remap = false)
```

Minecraft has shipped unobfuscated since 26.1. The name in `method` is the real
one and there is no mapping step that could translate it, so remapping is off.

## A worked example

```java title="src/main/java/com/example/mymod/mixin/MinecraftServerMixin.java"
package com.example.mymod.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Unique
    private boolean mymod$announced;

    @Inject(method = "tickServer", at = @At("HEAD"), remap = false)
    private void mymod$onFirstTick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (mymod$announced) {
            return;
        }
        mymod$announced = true;
        MinecraftServer server = (MinecraftServer) (Object) this;
        // …
    }
}
```

The injected method takes the target's parameters, then a `CallbackInfo`. Get
that wrong and the injection does not match — which, with `defaultRequire: 1`,
fails the launch rather than doing nothing.

<div class="admonition tip">
<p class="admonition-title">defaultRequire: 1</p>
<p>Leave it on. Without it, an injection that stops matching after a game update
silently never fires, and the feature it powered simply stops working with
nothing in the log. With it, the game refuses to start and names the mixin.</p>
</div>

## Reading and writing private state

`@Accessor` and `@Invoker` generate a getter or a call for something private,
without an injection:

```java
@Mixin(SomeClass.class)
public interface SomeClassAccessor {

    @Accessor("privateField")
    int getPrivateField();

    @Invoker("privateMethod")
    void callPrivateMethod();
}
```

Then `((SomeClassAccessor) instance).getPrivateField()`. The interface is the
mixin, so it lives in the mixin package — and because it is an interface rather
than a class, casting an instance to it is exactly what you are meant to do.

`@Mutable @Shadow @Final` lets a `final` field be reassigned. Use it sparingly:
a field the game assumed was constant is often constant for a reason.

## Duck typing {#duck-typing}

To attach your own method to a game class and call it from ordinary code, have a
mixin implement an interface:

```java title="com/example/mymod/Charged.java — NOT in the mixin package"
public interface Charged {
    int mymod$charge();
}
```

```java title="com/example/mymod/mixin/ItemStackMixin.java"
@Mixin(ItemStack.class)
public class ItemStackMixin implements Charged {

    @Unique
    private int mymod$charge;

    @Override
    public int mymod$charge() {
        return mymod$charge;
    }
}
```

```java
if (stack instanceof Charged charged) {
    int charge = charged.mymod$charge();
}
```

The interface **must not** live in the mixin package — that package belongs to
Mixin, and an interface there is treated as a mixin rather than as an interface
to implement.

For data attached to entities and block entities, reach for
[attachments](/docs/@latest/guides/attachments) first: they do this without a
mixin, and they save and load themselves.

## Access widening

Sometimes nothing needs to be injected — a field or method is simply not public.
Declare it in `fenix.mod.json` rather than writing an accessor:

```json title="src/main/resources/fenix.mod.json"
{
  "accessible": [
    "class net.minecraft.world.inventory.MenuType$MenuSupplier",
    "method net.minecraft.world.inventory.MenuType <init>",
    "field net.minecraft.world.level.block.Block someField"
  ]
}
```

Each entry is three words: the kind — `class`, `method` or `field` — then the
class, then the member. `<init>` is a constructor. A nested class is named with
`$`, as the JVM names it, not with a dot.

The loader widens those at run time, and the Gradle plugin widens the copy of
Minecraft you compile against — so what javac allows and what the loader allows
cannot disagree. That is the whole reason it is declared here rather than in a
separate file: one declaration, both sides.
