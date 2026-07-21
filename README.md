# Fenix

A modern Minecraft mod loader, and the toolchain around it.

> **Status: anyone can build a Fenix mod.** The loader, the API and the Gradle
> plugin are published to a [public Maven repository](docs/publishing.md), so a
> mod's whole build file is `id("fr.d4emon.fenix.dev")`. The plugin fetches
> Minecraft, compiles the mod against it, and `runClient` / `runServer` launch
> through the loader with Mixin wired in. See [the roadmap](docs/roadmap.md).

Fenix targets **Minecraft 26.2** on **Java 25**. Since Minecraft 26.1 the game
ships unobfuscated, so there is no mapping or remapping step anywhere in this
project: mods compile against real Minecraft names.

## Design principles

Fenix is not a Fabric or NeoForge clone. Where a loader has to do the same job,
it does it the way it would be designed today.

- **Compile time over runtime.** Mods are indexed by an annotation processor
  while they compile, so the loader reads a manifest instead of scanning the
  classpath at startup. A missing no-arg constructor is a `javac` error, not a
  crash five seconds into launch.
- **No global singletons.** A mod is handed a `Fenix` context object. There is
  no `Fenix.getInstance()`, which is what lets each mod get its own logger and
  its own view of the game.
- **Small modules.** If all you use is the event bus, you depend on
  `fenix-api-event` and nothing else. `fenix-api` exists for people who would
  rather pull everything in one line.
- **Honest lifecycle.** Typed methods with typed arguments, not string keys
  pointing at class names in a JSON file.

## Repository map

| Path                   | What it is                                                          |
|------------------------|---------------------------------------------------------------------|
| `fenix-loader/`        | The loader: classloading, mod discovery, resolution, mixin bootstrap |
| `fenix-api/`           | The mod-facing API, split into small independent modules             |
| `fenix-processor/`     | Annotation processor producing the compile-time mod index            |
| `fenix-installer/`     | Writes a Fenix profile into a `.minecraft` directory                 |
| `fenix-gradle-plugin/` | The `fr.d4emon.fenix.dev` plugin mod authors apply                   |
| `ember/`               | Generates assets and data from plain Java instead of hand-written JSON |
| `testing/harness/`     | A fake game, so the loader is testable without Minecraft             |
| `testing/conformance/` | Checks that require loading real Minecraft classes                   |
| `testmod/`             | Mod used to exercise the loader by hand                              |
| `examples/`            | Samples that double as documentation                                 |
| `website/`             | The Fenix site                                                       |
| `docs/`                | Developer documentation and architecture decisions                   |
| `build-logic/`         | Convention plugins for building Fenix itself                         |

The API modules are `fenix-api-{core,event,registry,resource,network,command,config}`,
and `fenix-api` aggregates them.

## Building

Requires **JDK 25**. The Gradle wrapper is checked in.

```bash
./gradlew build                              # compile and test everything
./gradlew :test-harness:runDemo              # boot the fake game through the loader
./gradlew :fenix-installer:installToLauncher # write the Fenix profile into .minecraft
./gradlew installFenix                       # publish every artifact to ~/.m2
./gradlew projects                           # list the modules
```

## Documentation

- [Getting started](docs/getting-started.md) — writing a mod against Fenix
- [Architecture](docs/architecture.md) — how the pieces fit, and the rules that keep them apart
- [Mod metadata](docs/mod-metadata.md) — the `fenix.mod.json` format
- [Roadmap](docs/roadmap.md)
- [Contributing](CONTRIBUTING.md)

## License

[Apache-2.0](LICENSE).

Fenix never redistributes the Minecraft jar; it is downloaded from Mojang at
build time and stays in your Gradle cache.
