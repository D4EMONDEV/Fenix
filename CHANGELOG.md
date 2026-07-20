# Changelog

All notable changes to Fenix are recorded here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and Fenix uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- Launch time: the classloader was reopening and reparsing the game jar's
  31,000-entry central directory for **every class it defined** — the
  uncached-connection fix for Windows file locks, applied per read. Real
  Minecraft took minutes to reach the title screen. Jars added to the child
  scope are now opened once, kept open, and read through their in-memory
  index; `close()` still releases the locks, and every classloader test passes
  unchanged. Loading 2000 real game classes: 163.9 s before, 0.4 s after.

### Added

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
