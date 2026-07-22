---
title: "loader.access"
description: "Types in fr.d4emon.fenix.loader.access"
sidebar:
  order: 91
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.loader.access</code></p>

| Type | What it is |
|---|---|
| [`AccessTransformer`](#accesstransformer) | Raises the declared access of what a mod asked to reach. |
| [`AccessWidener`](#accesswidener) | What a mod has asked to reach inside Minecraft. |

## AccessTransformer

Raises the declared access of what a mod asked to reach.

Runs before Mixin, because a mixin that targets a widened member has to
see it already widened — and because Mixin's own transformer is registered
after this one, which is what decides the order.

Making a nested type nameable takes two edits, not one. The type's own
access flags are the obvious half; the other is the `InnerClasses`
entry, which both the outer and the nested class carry and which is what
`javac` actually reads when deciding whether you may write the name
down. Widening only the first leaves a class that is public at runtime and
still refuses to compile.

### `byte[] transform(String className, byte[] bytes)`

## AccessWidener

What a mod has asked to reach inside Minecraft.

Mixin covers most of this already: `@Accessor` and `@Invoker`
reach a private field or method without touching its declaration. What they
cannot do is make a type <em>nameable</em> — and some of vanilla's doors are
shut that way. Creating a `MenuType` needs a private constructor whose
parameter is a private interface, so there is nothing a mod can write down at
all, in any package.

Hence widening: the declared access of named members is raised to public
before anything loads them. Fabric and Forge both carry the same mechanism,
under different names, for the same reason.

Declarations are written in `fenix.mod.json`:

```java
"accessible": [
 "class net.minecraft.world.inventory.MenuType$MenuSupplier",
 "method net.minecraft.world.inventory.MenuType <init>"
]
```

Members are named without a descriptor, so every overload of a name is
widened together. Naming one overload out of several is a precision nobody
has wanted and a signature that would rot on the next Minecraft update.

### `void add(List<String> declarations, String source)`

Reads a mod's declarations.

### `boolean isEmpty()`

{@return whether anything was declared at all}

Most mods declare nothing, and the transformer is worth skipping
entirely rather than running over every class the game loads.

### `boolean touches(String internalName)`

{@return whether this class is named by any declaration}

Includes classes named only as the owner of a member, and classes that
merely <em>contain</em> a widened nested class: a nested type is nameable
only if the outer class says so too, in its `InnerClasses` entry.

### `boolean widensClass(String internalName)`

{@return whether this class should be made public}

### `boolean widensField(String owner, String name)`

{@return whether this field should be made public}

### `boolean widensMethod(String owner, String name)`

{@return whether this method should be made public}

