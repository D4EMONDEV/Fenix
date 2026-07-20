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
        mavenLocal()          // until Fenix is on a public repository
        gradlePluginPortal()
    }
}
```

`build.gradle.kts`:

```kotlin
plugins {
    id("fr.d4emon.fenix.dev") version "0.1.0-SNAPSHOT"
}

dependencies {
    // Everything, in one line:
    fenixMod("fr.d4emon.fenix:fenix-api:0.1.0-SNAPSHOT")

    // Or only what you use:
    // fenixMod("fr.d4emon.fenix:fenix-api-event:0.1.0-SNAPSHOT")
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
./gradlew ember        # regenerate assets and data
./gradlew genSources   # decompile Minecraft for navigation
```
