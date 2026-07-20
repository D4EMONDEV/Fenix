# Contributing to Fenix

## Getting set up

You need **JDK 25** — Minecraft 26.2 runs on `java-runtime-epsilon`, major 25.
Everything else comes from the checked-in Gradle wrapper.

```bash
./gradlew build
```

Open the repository in IntelliJ IDEA and let it import the Gradle build. The
project JDK must be 25.

## Layout rules

A few boundaries in this repository exist for reasons that are not obvious from
reading the code. They are documented in [docs/architecture.md](docs/architecture.md);
the short version:

- **`fr.d4emon.fenix.api.*` is loader-side.** Those classes are loaded by the
  parent classloader because the loader itself needs them. Mod code must live
  outside that prefix, or it will be loaded twice and diverge.
- **Anything naming a `net.minecraft.client.*` type goes in a `.client`
  sub-package.** A common class that merely mentions a client type is a
  `NoClassDefFoundError` on a dedicated server.
- **All mixin configs share the root package `fr.d4emon.fenix.mixin.*`.** A
  mixin config declares exactly one package, so modules namespace themselves
  below that root.

## Tests

- Unit tests live next to the module they cover and run under `./gradlew build`.
- **Anything you could only verify by launching the game by hand belongs in
  `testing/conformance` as an automated check.** That rule is the single most
  valuable thing in this repository: a loader bug that only appears once real
  Minecraft classes are loaded is otherwise found by a human, slowly, twice.
- `testing/harness` is a fake game. Use it for loader behaviour that does not
  need Minecraft — it runs in milliseconds.

## Commits and pull requests

- Keep the changelog current: add an entry to [CHANGELOG.md](CHANGELOG.md)
  under `Unreleased`.
- One logical change per pull request.
- Architectural decisions get an entry in [docs/adr/](docs/adr/) — a short note
  on what was decided and why, so the next person does not relitigate it.

## Code style

`.editorconfig` is authoritative: UTF-8, 4 spaces, 120 columns.

Write comments that explain *why*, not *what*. The Minecraft-facing parts of
this codebase are full of decisions that look arbitrary until you know which
vanilla behaviour forced them — those are the comments worth having.
