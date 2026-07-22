---
title: "loader.classloader"
description: "Types in fr.d4emon.fenix.loader.classloader"
sidebar:
  order: 91
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.loader.classloader</code></p>

| Type | What it is |
|---|---|
| [`ClassTransformationException`](#classtransformationexception) | Thrown when a `ClassTransformer` fails, so the launch stops. |
| [`ClassTransformer`](#classtransformer) | Rewrites a class on its way into the JVM. |
| [`FenixClassLoader`](#fenixclassloader) | The classloader the game and every mod run inside. |
| [`FenixClassLoader.ClassGenerator`](#fenixclassloader-classgenerator) | Produces the bytes of a class no jar contains — Mixin's runtime-generated synthetic classes. |

## ClassTransformationException

Thrown when a `ClassTransformer` fails, so the launch stops.

Deliberately never swallowed by the classloader's fallback path: letting
the parent supply its copy after a failed transformation would silently run
the <em>untransformed</em> class, which is a far worse failure than a crash —
a mixin that did not apply, with no error anywhere.

## ClassTransformer

Rewrites a class on its way into the JVM.

Transformers only ever see classes the `FenixClassLoader` defines
itself — game and mod classes. Anything the parent loads, including the
loader and the API, is out of reach by construction.

A class is transformed exactly once, at definition. A transformer
registered after a class was defined never sees it, so anything that wants to
touch game classes has to be registered before the first game class loads —
in practice, during `onPreLaunch`.

### `byte[] transform(String className, byte[] classBytes)`

Transforms one class.

Transformers run in registration order, each receiving the previous
one's output.

## FenixClassLoader

The classloader the game and every mod run inside.

<strong>Child-first:</strong> a class is looked up in the game and mod
jars before the application classpath. That inversion is what makes
transformation possible at all — if the parent were asked first, it would
define game classes from the original jar and this loader would never see
them.

Several package prefixes are exceptions, always taken from the parent:

<ul>
<li>`fr.d4emon.fenix.loader.` — the loader itself;</li>
<li>`fr.d4emon.fenix.api.` — the contracts the loader shares with
   mods. If a mod jar carried its own copy and this loader defined it, the
   JVM would hold two `Class` objects with the same name, and every
   cast across the boundary would fail. The parent's copy is the only
   one that can exist.</li>
<li>`org.objectweb.asm.` and `org.spongepowered.asm.` — the
   transformation stack. A transformed game class holds references to Mixin
   runtime types like `CallbackInfo`; those must resolve to the same
   copy the transformer used, so exactly one copy can exist, on the
   parent. The one exception is `org.spongepowered.asm.synthetic.`,
   which Mixin generates at runtime against game classes and which
   therefore has to be defined here (see `ClassGenerator`).</li>
</ul>

(`java.` is short-circuited to the parent as well, as everywhere:
the JVM refuses user-defined classes in it.)

A parent-only class that the parent does not have is a
`ClassNotFoundException` even if a mod jar contains it — falling back
to the child copy would quietly recreate the split it exists to prevent.

Resources follow the same child-first rule, so a mod's
`assets/` shadow the classpath's on lookup, and
`#getResources(String)` lists child results first.

<strong>Performance contract:</strong> each jar added with
`#addPath(Path)` is opened exactly once and stays open until
`#close()`; class bytes are read from that open, indexed
`JarFile`. The game defines tens of thousands of classes at startup —
anything per-class that reopens a jar (in particular the JDK's
`jar:` URL machinery, cached or not) turns launch time from seconds
into minutes. Keeping the jars open ourselves is also what lets
`#close()` actually release the file locks, which Windows needs before
a player can update their mods folder.

### `void addPath(Path path)`

Adds a jar — or, in a development environment, a directory of classes —
to the child scope.

A jar is opened here, once, and stays open until `#close()`.

### `void addTransformer(ClassTransformer transformer)`

Registers a transformer for every class defined from now on.

Classes defined before registration stay as they are; there is no
retroactive transformation. See `ClassTransformer` for the timing
this imposes.

### `void close()`

### `Class<?> findClass(String name)`

### `URL getResource(String name)`

### `InputStream getResourceAsStream(String name)`

### `Enumeration<URL> getResources(String name)`

### `Class<?> loadClass(String name, boolean resolve)`

### `byte[] readClassBytes(String binaryName)`

Reads a class's bytes without transforming them, child scope before parent.

This is what the Mixin service reads mixin classes and target-class
hierarchies through: it wants the original bytecode, never the
post-transformation result.

### `void setClassGenerator(FenixClassLoader.ClassGenerator generator)`

Installs the generator for classes no jar contains.

There is one, Mixin's, so a second call is a wiring bug and is refused.

## FenixClassLoader.ClassGenerator

Produces the bytes of a class no jar contains — Mixin's runtime-generated
synthetic classes. Returns `null` when it cannot generate the name,
which becomes a `ClassNotFoundException`.

### `byte[] generate(String binaryName)`

Generates one class.

