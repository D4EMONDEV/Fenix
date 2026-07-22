# Roadmap

Phases are ordered so that each one is verifiable on its own. Phases 0 to 4 and
6 to 8 are done; phase 5 is in progress and phase 9 is half done.

The numbering is the order they were built in, not their importance — phase 5
is last to close because "the API" has no natural end.

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
  priorities, first-class unsubscription, lock-free dispatch. Server and client
  ticks, server lifecycle, levels loading and saving, players joining, leaving,
  dying and respawning, entities spawning (cancellable) and dying, and
  cancellable block break/use on both sides.
- **Registry** ✅ — a deferred `Registrar` with `Holder`s and fluent builders,
  absorbing the vanilla bookkeeping that a mod otherwise skips and crashes on.
  Blocks, items, block entities, sounds and entities — with their default
  attributes and, on the client, their renderers.
- **Resources** ✅ — every mod jar is handed to the game as a resource pack, so
  its models, textures and translations are actually read.
- **Creative tabs** ✅ — content goes into vanilla's tabs, or into a tab of the
  mod's own. Vanilla's strip holds exactly fourteen and vanilla fills all
  fourteen, so Fenix adds pages, with arrows at the top right of the panel and
  on Page Up/Page Down. Search, inventory, hotbars and op blocks travel to
  every page.

- **Menus** ✅ — `Registrar.menu` and a `SimpleMenu` that lays out slots and
  gets `quickMoveStack` right, plus client-side screen registration.
- **Access widening** ✅ — some of vanilla's doors are shut in a way no mixin
  can open, `MenuType` among them. `accessible` declarations in a mod's manifest
  are applied by the loader at run time *and* by the Gradle plugin to the jar
  the mod compiles against, so the two cannot disagree.

Still missing, in the order a mod author hits them:

- **Keybindings.** Nothing today. A client mod that wants a key has to reach
  into `KeyMapping` itself, and every one that does will do it differently.
- **Render layers.** Every block Fenix registers renders opaque. There is no
  way to say cutout or translucent, so glass, plants and anything with a hole
  in it are simply not writable yet.
- **Spawn eggs and spawn rules.** An entity can be registered and then only
  summoned by command.
- **The rest of the registries** — particles, effects, enchantments, fluids,
  recipe serializers. Each is a small addition on the pattern `menu` set; the
  work is knowing which vanilla bookkeeping each one skips.

## Phase 6 — Ember ✅

Assets and data generated from Java, as a set of providers:
`EmberModelProvider`, `EmberLanguageProvider`, `EmberLootTableProvider`,
`EmberRecipeProvider`, `EmberSoundProvider` and
`EmberTagsProvider.BlockTagsProvider`/`.ItemTagsProvider`.
Run with `gradlew ember`; output lands in `src/main/generated`.

Textures and ogg files are what it cannot generate.

## Phase 7 — Networking 🚧

- **Typed payloads** ✅ — `ToServer`/`ToClient` carrying a `StreamCodec`, with
  the direction in the type. Every mod payload travels inside one of two
  envelopes Fenix registers with vanilla, because vanilla builds its payload
  table eagerly from a list captured at class-load time: a mod type there would
  depend on vanilla's own class-loading order, and would vanish silently when
  that order changed. Two constant types carry no such bet, and an unknown
  channel can be named in a log instead of discarded without a word.
- **Registry sync** ✅ — the server states what it has on join, the client
  compares, and a mismatch is a disconnect naming the mod that is missing.
  Detection and a clear refusal, never live remapping: a client admitted with
  shifted network ids sees the wrong blocks, or is kicked by vanilla naming a
  block it cannot find, and nothing in that mentions the mod at fault. Digests
  per registry keep it to a few hundred bytes; the mod namespaces travel in
  full, which is what lets the refusal be specific.

## Phase 8 — Commands and config ✅

- **Commands** ✅ — `CommandEvents.REGISTER` hands out the dispatcher, and
  `Commands` covers the Brigadier boilerplate: `run(…)` swallows the `return 1`,
  and `operator()` names the permission that 26.2's `PermissionLevel` rework
  replaced numeric levels with.
- **Config** ✅ — `Config.of(fenix, DEFAULTS)` over a record. A missing setting
  takes its default rather than zero, an unknown key is named rather than
  dropped, and the file is rewritten complete so a setting added by an update is
  visible. Validation lives in the record's compact constructor, and its message
  reaches the player prefixed with the file and field.

## Phase 9 — Shipping

- Publish to a public Maven repository ✅ — a plain Maven repo on GitHub Pages,
  free and login-free to consume; see [publishing.md](publishing.md)
- The website — **written, and hosted nowhere.** `d4emondev.github.io/Fenix/`
  is the Maven repository, and a repository can only serve one Pages site. It
  needs either a custom domain or a `D4EMONDEV.github.io` user site; nothing
  in CI builds or deploys `website/` today.
- Generated API documentation — every module publishes a `-javadoc` jar, but
  nothing aggregates them into something readable in a browser.
- A conformance suite broad enough to trust a release — ten checks today, each
  verified to fail when the thing it covers is sabotaged. Untested end to end:
  the installer against a real `.minecraft`, and Ember's output against a real
  resource load.
- Maven Central, once the API stabilises
