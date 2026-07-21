# Changelog

All notable changes to Fenix are recorded here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and Fenix uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Registry sync.** The server states what it has as each player joins, the
  client compares, and a client missing one of the server's mods is disconnected
  with a sentence naming it. Without it such a client is admitted and then falls
  apart: one absent block shifts every network id after it, so the player sees
  the wrong blocks or is kicked by vanilla naming a block it cannot find, and
  none of that mentions the mod at fault. Detection and a clear refusal — never
  quietly remapping ids, which trades a confusing disconnect for a world that
  corrupts slowly. Registries travel as digests, so the check costs a few hundred
  bytes rather than the megabytes a modpack's ids would; the mod namespaces go in
  full, which is what turns "these differ" into a name. A client without Fenix
  answers nothing and is left alone.
- The example mod now uses typed payloads for its tally block: the server sends
  the count, and shift-clicking asks for a reset. The reset handler checks the
  player is actually near the block, because a client can send any position at
  any time — the sort of thing an example should show rather than mention.

- **Typed payloads** (`fenix-api-network`). `ToServer` and `ToClient` carry a
  `StreamCodec` and put the direction in the type, so sending one the wrong way
  is a compile error rather than a packet nobody handles. Server handlers are
  given the player who sent it; a client with no handler for a channel drops it,
  which is what lets a server run a mod its players do not have.

  Every mod payload travels inside one of two envelopes Fenix registers with
  vanilla, and that is the load-bearing decision. Vanilla builds its payload
  table once, eagerly, from a list captured when the packet class is first
  loaded — so a mod type in that table would have to be registered before a
  moment decided by vanilla's own class-loading order, which could change on any
  update and whose failure is silent: the packet decodes as a discarded payload
  and is never heard from again. Two constant types carry no such bet, since the
  injection that adds them runs at transform time, always before any static
  initialiser. Mods then register whenever they like, and the ordering question
  disappears rather than being answered. The cost is one identifier per packet;
  the gain is that an unknown channel gets named instead of vanishing.

- The Fenix API is a `fenixMod` dependency by default rather than a compile-only
  one, so a mod's build file needs no `dependencies` block at all. The two
  disagreeing was how you got a mod that compiled and then could not find, at
  run time, the class it was written against — the precise failure Fenix exists
  to move earlier. A mod that wants fewer modules sets `fenix { api = false }`
  and names them.
- `@Generator` in a client source set is now a compile error naming the fix.
  Ember reads only the common index, so one there was silently skipped and its
  files never written — discovered later as a missing model in game.

- **Split source sets.** A mod is now written in `src/main/java`, with an
  optional `src/client/java` beside it. Common code compiles against Minecraft
  with the client half stripped out, so naming a `net.minecraft.client` type
  from common code is a `javac` error with a line number instead of a
  `NoClassDefFoundError` on somebody else's dedicated server — which is the
  worst place to find it, because a mod author develops on a client and never
  sees it. This was the last place where Fenix asked for a convention where it
  could have asked the compiler.

  Client code may use common code; the reverse cannot compile, which is both
  the useful direction and the only one that can be enforced. Each half gets a
  `@Mod` class — the same annotation, the same `FenixMod` interface; what makes
  one client-only is where the file lives. They ship in one jar but are indexed
  separately (`fenix.index.client.json`), so a server is never told the client
  class exists. The common half runs first, so the client half can rely on what
  it registered.

  Nothing to switch on: the source set appears when `src/client/java` does. The
  common jar is derived from the client jar by removing the four roots a
  dedicated server does not ship, so it costs one pass over a file already on
  disk rather than another download. `fenix-api-event`, `fenix-api-registry`,
  `testmod` and the example mod are all split this way — the API lives by the
  rule it asks for.

- **Block entities** (`Registrar.blockEntity`). Getting the valid-blocks set
  wrong is silent: the type registers, the block places, and the game simply
  never creates the block entity, so whatever it stored is never there. A block
  that does not implement `EntityBlock` is now refused at startup for the same
  reason. Block entity types register in a second pass, after everything else,
  so a mod can declare a type and its block in whichever order reads best
  instead of ordering its fields to suit the registrar.
- **Entities** (`Registrar.entity`), their default attributes
  (`Registrar.attributes`) and their renderers (`EntityRendering.register`,
  client-only). Attributes are not optional for anything living: a
  `LivingEntity` asks vanilla for them inside its own constructor, so one that
  is missing dies there, in vanilla code, nowhere near the mod. Vanilla's table
  is an `ImmutableMap` and cannot be added to, and merely reading it during
  registration would build it before the attribute registry is bound — so Fenix
  keeps its own table and consults it first, resolving a mod's values lazily on
  the first ask. The renderer table is vanilla's and mutable but its `register`
  is private; an entity missing from it is invisible, which vanilla mentions
  once in the log and never again.
- **Sounds** (`Registrar.sound`) and `EmberSoundProvider`, which writes the
  `sounds.json` half — a sound event without it plays nothing, silently.
- **Creative tabs**, with pages. `CreativeTabs.addTo` puts content in vanilla's
  tabs and `Registrar.creativeTab` gives a mod one of its own — without either,
  registered content is reachable only through `/give`, which is the difference
  between a mod a player can use and one they cannot. Vanilla's tab strip is
  two rows of seven and vanilla fills all fourteen, so a mod tab has nowhere to
  go: Fenix pages the strip, keeping vanilla's tabs alone on page 0 and putting
  mod tabs on pages after it, with the recipe book's own arrows drawn at the top
  right of the panel. Two consequences worth naming. Narrowing
  `CreativeModeTabs.tabs()` is what makes drawing, clicking and tooltips agree,
  since the screen asks five separate times. And vanilla's bootstrap refuses to
  start when two tabs share a row and column — true of every mod tab, since all
  fourteen squares are taken — so that check is widened to include the page
  rather than dropped: two tabs actually drawn on top of each other still refuse
  to load. Search, the inventory, saved hotbars and operator blocks travel to
  every page — they are tools rather than categories, and losing the search box
  to reach a mod's blocks is what makes paging feel bad elsewhere; that is also
  why a page holds ten mod tabs and not fourteen. The arrows are real widgets
  with Fenix's own sprites, drawn in the palette the panel already uses, so
  hovering, focus, narration and the `Page 1/2` tooltip come from the screen
  rather than from hand-rolled hit-testing. Page Up and Page Down do the same.
- `EmberLanguageProvider.add(ResourceKey<CreativeModeTab>, String)`, which
  derives the translation key from the tab instead of taking it as a string.
  `CreativeTabs.titleKey` is now the single place that key is worked out, so a
  renamed tab cannot leave its translation behind — which in game reads as a tab
  titled `itemGroup.your-mod.something`.
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
