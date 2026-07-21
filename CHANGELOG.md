# Changelog

All notable changes to Fenix are recorded here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and Fenix uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Ember is now a set of providers rather than one method: `EmberModelProvider`,
  `EmberLanguageProvider`, `EmberLootTableProvider`, `EmberRecipeProvider` and
  `EmberTagsProvider.BlockTagsProvider`/`.ItemTagsProvider`. A single `collect`
  becomes a dumping ground as a mod grows, and each domain wants a different
  shape — a language provider wants `add(key, value)`, a recipe provider wants a
  builder. Loot tables, recipes and tags are new capabilities, not just moved
  code. Tag files are written under the *tag's* namespace, so a mod joins
  `minecraft:mineable/pickaxe` rather than replacing it.
- **The content registrar** (`fenix-api-registry`). A mod declares blocks and
  items in fields and registers them with one call from `onRegister`; a
  `Holder` stands in until then, and reading it too early says so rather than
  handing back null. What it really buys is the vanilla bookkeeping that
  happens *around* registration and that a mod otherwise bypasses: ids set on
  properties before construction, block-state network ids and caches redone
  (vanilla assigns those in one pass that has already run by the time a mod can
  register), and the `Item.BY_BLOCK` mapping without which `Block.asItem()`
  answers air and caches that answer. Each of those is a crash that surfaces far
  from its cause, inside vanilla code. Verified by registering a block and an
  item through a real `Bootstrap.bootStrap()` under the loader and checking all
  three passes took effect — now a conformance check, driving the whole
  pipeline: a mod jar in a mods directory, the loader discovering it, mixins
  firing `onRegister` while the registries are open, and a probe running as the
  game to inspect the result.
- Fluent builders for content: `newBlock("ruby_block").strength(3f)
  .requiresTool().withItem().register()`, and the same for items. They cover
  what most content needs, and `properties(…)` hands you vanilla's own builder
  for anything else — a shortcut over that API, never a wall in front of it.
- `examples/example-mod` now adds real content, laid out the way a mod would:
  `ModBlocks`, `ModItems` and a shared `ModContent`, rather than everything in
  the mod class.
- **The event bus** (`fenix-api-event`), the foundation the rest of the API
  hangs off. An event carries a context — normally a record — so declaring one
  is two lines instead of a hand-written functional interface and a combiner
  per event, and adding a parameter later does not break every listener's
  signature. `Event` cannot be cancelled and `CancellableEvent`'s listeners
  return a `Flow`, so cancellability is a promise in the type rather than a
  convention. Registration returns a `Subscription`, so a listener that only
  matters while a screen is open or a world is loaded can be taken back off.
  Listeners carry an `int` priority (higher runs first, ties keep registration
  order). Dispatch takes no lock and allocates nothing: registration rebuilds a
  sorted array behind a `volatile`, so registering or unsubscribing during a
  dispatch is safe and simply takes effect from the next one.
- The first game events, fired by mixins: `ClientEvents` (tick), `ServerEvents`
  (started, tick), and cancellable block events on both sides —
  `BlockEvents.BREAK`/`USE` on the server, where cancelling actually holds, and
  `ClientBlockEvents.ATTACK`/`USE` for immediate feedback that never enforces
  anything. Their mixin config requires every injection to land, so a signature
  that stops matching in a future Minecraft fails loudly instead of leaving an
  event that silently never fires.
- The dev plugin gained a `library` mode: Minecraft on the compile classpath
  and nothing else. Fenix's own API modules use it — they *are* the API, so
  depending on it would be circular, and there is nothing to launch.
- A conformance check that the event mixins still land on their real Minecraft
  targets, by reading back the bytecode Mixin produced. This is what catches a
  Minecraft update moving a method the events hang off, whose failure mode is
  otherwise an event that silently never fires.
- `testmod` and `examples/example-mod` now listen to events — server lifecycle,
  ticks and block breaking, including cancelling it. `testing/demo-mod` is a new
  Minecraft-free mod so the fake-game smoke test still exercises mod loading now
  that the other two need the real game.
- **A public Maven repository.** Fenix publishes to a plain Maven repository
  hosted on GitHub Pages — free and login-free to consume — so a mod's whole
  build file is `id("fr.d4emon.fenix.dev")` after adding the repository to
  `pluginManagement`. `publishFenixRepo` builds the repository locally; a
  `Publish` workflow deploys it to Pages on a version tag. Verified by building
  a mod project from outside the repository against the published artifacts.
- **The `fr.d4emon.fenix.dev` Gradle plugin** — a Fenix mod's entire build file
  is now `id("fr.d4emon.fenix.dev")`. It downloads and SHA-1-verifies the
  Minecraft client into the Fenix cache, puts the game and its libraries on the
  compile classpath under real names, wires the API and annotation processor,
  templates `${version}`/`${minecraft_version}` in `fenix.mod.json`, selects the
  game's Java toolchain, and adds `runClient` — which launches the client
  through the loader with the mod in `run/mods`, reusing the vanilla launcher's
  assets. `examples/example-mod` is the proof: it compiles against Minecraft and
  runs through Fenix. In-repo, the Fenix coordinates resolve to the sibling
  projects via dependency substitution, so a fresh clone needs no publish step.
- The dev plugin gained `runServer`, `genSources` and IDE run configurations,
  completing the developer workflow. `runServer` un-bundles Mojang's server jar
  (a bundler since 1.18) and launches the real server through the loader on the
  server side; `genSources` decompiles Minecraft with Vineflower for
  navigation; and during an IntelliJ sync the plugin writes Gradle run
  configurations for all three launch tasks. Fenix still never writes
  `eula=true` — accepting the licence stays the user's act.
- **Mixin integration.** Mods can now transform the game. `FenixMixinService`
  bridges the SpongePowered Mixin fork to the loader's classloader; `MixinSetup`
  brings the environment up, registers every config (the loader's own and each
  mod's `mixins`), and hands classes to the transformer as they load. The
  classloader pins ASM and Mixin to the parent so a transformed game class and
  the transformer share one copy of `CallbackInfo`, and defines Mixin's
  synthetic classes on demand. No refmaps — the game is unobfuscated — and
  mixins target Minecraft by string, so a mod (and the loader itself) compiles
  without the game on its classpath. Proven by a conformance test that applies
  a mixin to a synthetic target through the real pipeline.
- Lifecycle mixins fire the later phases from inside real Minecraft:
  `onRegister` at the head of `BuiltInRegistries.freeze`, `onInit` at the tail
  of the client and server constructors. `testmod` ships a title mixin that
  appends " | Fenix Loader" to the window title — the visible proof a mod
  reached into the game.
- The installer now ships Mixin and ASM alongside the loader, listed as
  libraries in the version manifest, so the launcher puts the whole
  transformation stack on the classpath.
- `fenix-installer`: writes a Fenix profile into `.minecraft` — the loader jars
  Maven-style under `libraries/`, a version manifest inheriting from vanilla
  with `Launch` as the main class, and a launcher profile entry. The loader
  jars are embedded in the installer jar, so it is one self-contained file.
  Everything already present in `launcher_profiles.json` is preserved, and a
  reinstall updates the profile instead of duplicating it.
- `GameLocator`: recognises Minecraft on the classpath or in an explicit jar —
  client main checked before server main, since the client jar contains both —
  and reads the game version from the jar's `version.json`, which feeds the
  `minecraft` builtin so `"minecraft": "~26.2"` dependencies are enforced.
- `Launch` speaks launcher: Fenix options moved to a `--fenix.*` namespace,
  every other argument passes through to the game untouched, and the vanilla
  `--gameDir` is peeked so loader and game agree on the game directory.
  `--fenix.dryRun` runs the whole pipeline and stops after proving the game
  main class resolves through the Fenix classloader.
- The launch pipeline: `Launch` wires discovery, resolution, classloading and
  instantiation together, fires `onPreLaunch`, then hands over to the game's
  main class inside the child scope; the game fires `onRegister`/`onInit` back
  through `FenixHooks`. Each mod receives its own `Fenix` context with a scoped
  logger and per-mod config directory. `gradlew :test-harness:runDemo` boots
  the fake game with `testmod` installed — the whole pipeline, no Minecraft.
- `fenix-processor`: the annotation processor behind `@Mod`. Writes
  `fenix.index.json` into the jar at compile time and rejects, with a compiler
  error, every mistake that would otherwise crash at launch: abstract or
  non-public classes, non-static inner classes, missing public no-arg
  constructors, classes not implementing `FenixMod`, invalid or duplicate ids.
- `FenixClassLoader`: child-first classloading over the game and mod jars, with
  `fr.d4emon.fenix.loader.` and `fr.d4emon.fenix.api.` pinned to the parent so
  the contracts exist exactly once, a chaining `ClassTransformer` hook applied
  at class definition, child-first resources, and uncached jar access so closed
  loaders release their file locks on Windows.
- Mod discovery and resolution in `fenix-loader`. `ModDiscoverer` scans the
  mods directory and reports every unreadable jar at once; `ModResolver` checks
  ids, sides and dependency ranges, then produces a deterministic load order —
  dependencies first, alphabetical among the unconstrained — naming every
  problem in a single failure, including the full path of a dependency cycle.
- `fenix.mod.json` parsing in `fenix-loader`: `ModMetadata`, `ModDependency`,
  `ModSide` and a hand-walked reader whose failures always name the jar and the
  offending field.
- `VersionRange` in `fenix-api-core`, supporting `*`, exact versions, `>=`,
  `>`, `<=`, `<`, `^` and `~`, with the caret tightening below `1.0.0`.
- `fenix-api-core`, the contracts every other module builds on: the `@Mod`
  annotation, the `FenixMod` lifecycle (`onPreLaunch`, `onRegister`, `onInit`),
  the `Fenix` context, `ModInfo`, `Side`, `Version` and `FenixLogger`.
- Initial repository scaffolding: monorepo layout, Gradle build with convention
  plugins in `build-logic`, version catalogue, and the module boundaries for the
  loader, the split API, the annotation processor, the installer, Ember, the
  Gradle plugin, the test harness, the conformance suite and the website.

### Changed

- The version is now `0.1.0`, not `0.1.0-SNAPSHOT`: a statically hosted
  repository serves releases, and a pre-release sorts below its release so
  `>=0.1.0` would otherwise reject the loader.

### Fixed

- The dev plugin no longer copies non-mods into `run/mods`. A `fenixMod`
  dependency brings its own dependencies, and a plain library among them —
  `fenix-api-core`, which the loader supplies on the parent classpath — made the
  loader refuse to start.
- Launch time: the classloader was reopening and reparsing the game jar's
  31,000-entry central directory for **every class it defined** — the
  uncached-connection fix for Windows file locks, applied per read. Real
  Minecraft took minutes to reach the title screen. Jars added to the child
  scope are now opened once, kept open, and read through their in-memory
  index; `close()` still releases the locks, and every classloader test passes
  unchanged. Loading 2000 real game classes: 163.9 s before, 0.4 s after.
