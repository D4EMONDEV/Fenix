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
  — an entry class may be a singleton, which is what Kotlin's `object` compiles to
- Discovery runs in parallel, and results stay in sorted order ✅
- `breaks` and `after` in the manifest ✅ — refusing a combination, and
  ordering after something without requiring it
- Two mods carrying the same library keep the newer copy, rather than refusing ✅
- A failed launch is written beside the game and shown in a window ✅ — the
  diagnosis was always good, and always went where nobody looked
- `/fenix mods` lists what is loaded, in load order ✅

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

- **Key bindings** ✅ — `KeyBindings.register` and a category of the mod's
  own. Vanilla builds its list of mappings once, from a field initialiser
  naming its own one by one, and never reads it again; a mapping missing from
  it never reaches the controls screen and is never saved, so the key works
  until the player restarts and then silently reverts.
- **Spawn eggs and spawn rules** ✅ — `Registrar.spawnEgg` and
  `Registrar.spawnRule`. Without the rule an entity can be summoned and hatched
  and never appears in the world, which reads as a wrong spawn weight rather
  than as a missing registration.
- **Particles, status effects and data components** ✅ — one line each on the
  common side, plus `ParticleRendering` on the client: a particle type with no
  provider is spawned and never drawn, silently.
- **World generation** ✅ — `EmberOreProvider` writes the two files an ore
  needs, and `BiomeModifications.addFeature` says which biomes want it. The
  alternative, overriding whole biome files in a datapack, does not compose:
  two mods each adding an ore to the plains erase one another.
- **Fluids** ✅ — `Registrar.newFluid` registers the four things a fluid is in
  one pass: a still form and a flowing form, the block it becomes in the world,
  and the bucket that carries it. A `FenixFlowingFluid` mirrors `WaterFluid`,
  configured rather than subclassed. On the client `FluidRendering` names the
  sprites — 26.2 bakes fluid models into a two-entry map of water and lava, and
  a mixin adds a mod's fluids to it, so they are drawn rather than shown as the
  missing-texture checkerboard.
- **Attachments** ✅ — `Registrar.attachment` declares a typed, optionally
  persistent piece of data a mod hangs on an `Entity` or `BlockEntity` without
  editing either class. A mixin makes both an `AttachmentHolder`; a persistent
  attachment is written beside the thing it is attached to, through its codec,
  at the same two methods the thing saves itself with — so it survives a world
  reload with no storage of its own. `get` never writes, so reading an
  attachment off every mob in a level does not quietly grow the save.
- **Custom recipes** ✅ — `Registrar.recipeType` and `Registrar.recipeSerializer`
  register the two halves a mod's own recipe kind needs; a mod writes the recipe
  on vanilla's `SingleItemRecipe`, or any `Recipe`, and its station finds it with
  `getRecipeFor(TYPE, input, level)`. The recipes themselves are datapack JSON,
  read through the serializer. `example-mod`'s reforging table is the crafting
  screen to go with it: a block, a two-slot menu and a recipe of its own.
- **Villager professions and trades** ✅ — `Registrar.poiType` registers a
  job-site point of interest and, crucially, redoes the block-state bookkeeping
  vanilla does at bootstrap, without which a job site is registered and no
  villager ever recognises it. `Registrar.villagerProfession` registers the
  profession that claims it. Trades became datapack data in 26.2 — a `trade_set`
  of `villager_trade` entries the profession names by level — so the API
  registers the profession and points it at the sets, which a mod ships as JSON.

  A mod also has to add its job site to `minecraft:acquirable_job_site`, which
  is the one part Fenix cannot do for it: an unemployed villager searches with
  the `none` profession's predicate, and that predicate is the tag, not the
  registry. Everything else can be right and no villager ever takes the job.
  Fenix cannot write the tag — it is datapack data a pack may legitimately
  override — so it does the next best thing and says so, naming the profession
  and the file, the moment the tags bind.

- **Block interactions** ✅ — `BlockInteractions` covers the behaviour vanilla
  keeps in tables rather than on the block: flammability, composting, stripping,
  waxing, oxidation and furnace fuel. Each table is filled once at bootstrap from
  a list of vanilla's own content, so a modded wood type looks like wood and
  quietly is not — it will not catch fire, an axe does nothing to it, and a
  furnace refuses it, with nothing logged because from vanilla's side nothing is
  wrong. The tables that cannot be added to are answered ahead of; the two that
  are read inline in several places are replaced whole; the composter's, which
  is mutable, is written into at `apply()`.

- **Potions and brewing** ✅ — `Registrar.potion` registers a potion; `Brewing`
  says what makes it. Both halves are needed and the second is the one that is
  easy to miss: vanilla builds its brewing table once per server from a fixed
  list and throws the builder away, so a registered potion that nothing brews
  into can be given by command and made by no brewing stand in the world. Fenix
  catches the builder while it is still open and fills it through the same public
  methods vanilla just used, so a mod's mixes survive a datapack reload the way
  vanilla's do.
- **More events** ✅ — a tooltip being built, this client joining or leaving a
  world, the HUD being drawn, and a loot table being read. The tooltip event
  hands over a live, writable list so a mod can put a line where it belongs
  relative to what is already there. The HUD event is taken on `Hud` rather than
  `Gui`: the graphics object is a local in `Gui.extractRenderState` and a
  parameter one level in, so the same moment is reachable without capturing a
  local. `LootEvents.LOADING` catches tables while they are still a map, before
  they are frozen into a registry, and `addPool` adds to a table rather than
  replacing it — two mods can both drop something from stone, which is exactly
  what overriding the file in a datapack cannot do.

Three things worth writing down because they *look* missing and are not:

- **Render layers.** 26.2 derives them from the texture's own alpha, in
  `BakedQuad.MaterialInfo.of` via `ChunkSectionLayer.byTransparency`, so glass
  and plants render correctly with no registration at all. The
  `ItemBlockRenderTypes` table earlier versions needed is gone.
- **Enchantments.** Datapack data since 1.21, not a code registry.
- **Paintings.** Likewise: `PAINTING_VARIANT` is loaded by `RegistryDataLoader`
  in 26.2, so a painting is a JSON file rather than something to register. It
  belongs to Ember, whenever Ember grows a provider for it.

## Phase 6 — Ember ✅

Assets and data generated from Java, as a set of providers:
`EmberModelProvider`, `EmberLanguageProvider`, `EmberLootTableProvider`,
`EmberRecipeProvider`, `EmberSoundProvider`, `EmberOreProvider` and
`EmberTagsProvider.BlockTagsProvider`/`.ItemTagsProvider`.
Run with `gradlew ember`; output lands in `src/main/generated`.

Textures and ogg files are what it cannot generate, and so are particle
definition files — the small `particles/<name>.json` listing a particle's
sprites is still written by hand.

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
- The website — **written, and hosted nowhere.** React, Vite and TypeScript;
  `npm run build` in `website/` produces a static `dist/`. `d4emondev.github.io/Fenix/`
  is the Maven repository, and a repository can only serve one Pages site. It
  needs either a custom domain or a `D4EMONDEV.github.io` user site; nothing
  in CI builds or deploys `website/` today.
- A project generator on the website ✅ — the form, the templates and the zip
  writer all run in the browser, so the page stays a static file. It ships the
  Gradle wrapper too: without it the first line of the README it writes,
  `./gradlew runClient`, names a file the reader does not have.
- Generated API documentation ✅ — `./gradlew apiDocsSite` writes the reference
  into the website as Markdown, versioned with the rest of the documentation.
  Written by a doclet rather than by hand, so the reference cannot describe an
  API the compiler does not have. `./gradlew apiDocs` still produces plain
  Javadoc, for anyone who wants it.
- A conformance suite broad enough to trust a release — nineteen checks today, each
  verified to fail when the thing it covers is sabotaged. Untested end to end:
  the installer against a real `.minecraft`, and Ember's output against a real
  resource load.
- Maven Central, once the API stabilises
