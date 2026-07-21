# Getting started

> Fenix is scaffolding right now — the workflow below is the target, not
> something that runs today. Track progress in [roadmap.md](roadmap.md).

## Building this repository

Requires JDK 25.

```bash
./gradlew build          # compile and test everything
./gradlew installFenix   # publish every artifact to ~/.m2
```

`installFenix` is what you run after changing the loader, the API or the Gradle
plugin, so that a mod project outside this repository picks the change up.

## Writing a mod

`settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        maven("https://d4emondev.github.io/Fenix/")   // the Fenix plugin
        gradlePluginPortal()
    }
}
```

`build.gradle.kts` — the plugin adds the Fenix repository itself, so this is the
whole file:

```kotlin
plugins {
    id("fr.d4emon.fenix.dev") version "0.1.0"
}

fenix {
    minecraft = "26.2"
}
```

The plugin puts Minecraft and the whole Fenix API on the compile classpath. If
you would rather depend on individual API modules, add them explicitly:

```kotlin
dependencies {
    // Only what you use, instead of the whole API:
    fenixMod("fr.d4emon.fenix:fenix-api-event:0.1.0")
}
```

The mod class:

```java
@Mod("example-mod")
public final class ExampleMod implements FenixMod {

    @Override
    public void onInit(Fenix fenix) {
        fenix.logger().info("Hello from {}", fenix.mod().name());
    }
}
```

Plus a `fenix.mod.json` at the root of your resources — see
[mod-metadata.md](mod-metadata.md).

There is no entry in the metadata pointing at `ExampleMod`. The annotation
processor finds the class while it compiles and writes the index into your jar,
so a typo is a compile error rather than a mod that silently never loads.

## Running it

```bash
./gradlew runClient
./gradlew runServer
./gradlew genSources   # decompile Minecraft for navigation
```
