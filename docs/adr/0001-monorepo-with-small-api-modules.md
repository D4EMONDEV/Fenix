# 0001 — Monorepo, with the API split into small modules

**Status:** Accepted — 2026-07-20

## Context

The loader, the API, the annotation processor, the Gradle plugin and the
datagen tool all change together in early development. A change to the loader's
mod index format touches the processor and the plugin in the same breath.

Separately, the API needs a shape. Fabric ships one mod per API module and nests
them with Jar-in-Jar. NeoForge ships one large artifact. Both work.

## Decision

One repository, versioned together.

The API is split into small independent modules — `fenix-api-event`,
`fenix-api-registry`, and so on — each publishable on its own. `fenix-api` is an
aggregate that depends on all of them.

## Consequences

- A cross-cutting change is one commit and one review, and CI proves the whole
  thing still fits together.
- A mod that only listens for events does not carry the networking stack.
- `fenix-api` keeps the easy path easy: one dependency line pulls everything.
- Every module shares a version number, so a slice gets a release even when
  nothing in it changed. Acceptable in exchange for never debugging a
  version matrix.
- Splitting later would have been a breaking change for every mod. Splitting now
  costs nothing.
