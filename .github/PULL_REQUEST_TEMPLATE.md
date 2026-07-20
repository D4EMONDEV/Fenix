## What this changes

<!-- And why. If it fixes an issue, link it. -->

## How it was verified

<!--
Unit tests, a conformance check, or a manual run — say which.

If it could only be verified by launching the game by hand, consider whether it
belongs in testing/conformance instead. See CONTRIBUTING.md.
-->

## Checklist

- [ ] `./gradlew build` passes
- [ ] `CHANGELOG.md` updated under `Unreleased`
- [ ] An ADR added under `docs/adr/`, if this closes off a reasonable alternative
- [ ] Nothing under `net.minecraft.client.*` is referenced outside a `.client` package
