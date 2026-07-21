# Roadmap

Phases are ordered so that each one is verifiable on its own. Nothing after
phase 0 is implemented yet.

## Phase 0 — Scaffolding ✅

Monorepo layout, Gradle build with convention plugins, version catalogue,
module boundaries, repository documentation. `./gradlew build` is green.

## Phase 1 — Loader core ✅

The loader running against `testing/harness`, a fake game, so none of this needs
Minecraft to be tested. `./gradlew :test-harness:runDemo` shows it end to end.

- `fenix.mod.json` parsing and validation ✅
- Mod discovery from a mods directory ✅
- Semantic version constraint solving and dependency ordering ✅
- A child-first classloader with a transformation hook ✅
- The `@Mod` annotation, the `fenix-processor` index, and lifecycle dispatch ✅

## Phase 2 — Launching real Minecraft ✅

- `fenix-installer`: a version manifest and launcher profile written into
  `.minecraft` ✅ — the profile inherits from vanilla, so the launcher builds
  the classpath and Fenix only swaps the main class
- Locating the game jar and detecting the side ✅ — plus the game version read
  from the jar's `version.json`, which feeds the `minecraft` builtin
- A dry-run mode that proves the classpath is right without opening a window ✅
  — `--fenix.dryRun`, verified against the real 26.2 client jar

## Phase 3 — Mixin ✅

- A Fenix mixin service backed by the loader's classloader ✅
- `mixins` in mod metadata, and the shared `fr.d4emon.fenix.mixin.*` root ✅
- No refmaps: the game is unobfuscated ✅ — mixins target Minecraft by string,
  so nothing needs the game on its compile classpath
- Lifecycle mixins fire `onRegister`/`onInit` from inside the game, and
  `testmod`'s title mixin is the visible proof

## Phase 4 — The Gradle plugin ✅

`fr.d4emon.fenix.dev`, which is what makes Fenix usable by anyone else.

- Download the client from piston-meta, plus vanilla libraries ✅ — the client
  is cached and SHA-1 verified; libraries are ordinary Gradle dependencies
- `runClient` ✅ — launches through the loader with the mod in `run/mods`,
  reusing the vanilla launcher's assets
- `runServer` ✅ — un-bundles Mojang's server jar and launches it, server side
- `genSources` via Vineflower ✅ — decompiles Minecraft for navigation
- IDE run configurations ✅ — written for IntelliJ during Gradle sync

`examples/example-mod` is a complete Fenix mod whose whole build file is
`id("fr.d4emon.fenix.dev")`: it compiles against real Minecraft and runs
through Fenix.

Third parties can use it: the artifacts are published to a public Maven
repository (see below), so a mod's whole build file is one plugin line.

## Phase 5 — The API 🚧

`fenix-api-event`, then `registry`, then `resource`. Each one is a real mod that
proves the loader works.

- **Events** ✅ — `Event`/`CancellableEvent` carrying a context record,
  priorities, first-class unsubscription, lock-free dispatch. Client and server
  ticks, server lifecycle, and cancellable block break/use on both sides.
- **Registry** ✅ — a deferred `Registrar` with `Holder`s and fluent builders,
  absorbing the vanilla bookkeeping that a mod otherwise skips and crashes on.
  Blocks and items today.
- **Resources** ✅ — every mod jar is handed to the game as a resource pack, so
  its models, textures and translations are actually read.
- **Creative tabs** ✅ — content goes into vanilla's tabs, or into a tab of the
  mod's own. Vanilla's strip holds exactly fourteen and vanilla fills all
  fourteen, so Fenix adds pages, with arrows at the top right of the panel and
  on Page Up/Page Down. Search, inventory, hotbars and op blocks travel to
  every page.

Still missing, and wanted: more events (player, entity, world), and more
registries (entities, block entities, sounds).

## Phase 6 — Ember ✅

Assets and data generated from Java, as a set of providers:
`EmberModelProvider`, `EmberLanguageProvider`, `EmberLootTableProvider`,
`EmberRecipeProvider` and `EmberTagsProvider.BlockTagsProvider`/`.ItemTagsProvider`.
Run with `gradlew ember`; output lands in `src/main/generated`.

Textures are the one thing not generated.

## Phase 7 — Networking

Typed custom payloads, then registry sync — detection and a clear refusal
first, never live remapping.

## Phase 8 — Commands and config

Brigadier registration, and typed configuration backed by records — something
Fabric leaves to third-party libraries.

## Phase 9 — Shipping

- Publish to a public Maven repository ✅ — a plain Maven repo on GitHub Pages,
  free and login-free to consume; see [publishing.md](publishing.md)
- The website, with generated API documentation
- A conformance suite broad enough to trust a release
- Maven Central, once the API stabilises
