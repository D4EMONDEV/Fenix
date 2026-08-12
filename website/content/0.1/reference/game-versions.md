---
title: Minecraft versions
description: Which Fenix release goes with which game version, and how a mod picks.
order: 3
---

# Supporting several Minecraft versions

Fenix's API is compiled against Minecraft. That single fact decides everything
on this page.

## Why one API cannot serve two games

Minecraft renames and moves things every release, and it does so without
deprecation. Adapting Fenix to 26.2 alone meant following the text-drawing call
to a new name, the creative tab structure to a new shape, and villager trades
out of code and into datapacks. None of those are additions that an older
compile could ignore — they are the same operation spelled differently.

So an API jar belongs to exactly one game version. That is what `+mc26.2` in a
coordinate says, and why the loader refuses at startup to run a mod whose
`depends.minecraft` does not match the game it found.

The loader itself is a different story. It classloads, discovers mods, and
starts Mixin, and almost none of that touches a Minecraft class. It carries no
game version, and one loader build runs several games.

## One branch per game version

`main` is the version currently being developed. When Minecraft ships a release
that breaks the API, `main` becomes that release and the previous one continues
on its own branch:

```
main          26.2  ← current
mc/26.1       26.1  ← maintenance
```

The alternative — one branch compiling against several game versions, with
source-set overlays for the parts that differ — was considered and rejected. It
pays a permanent structural cost for something Minecraft breaks anyway: the
first rename splits the file, and the overlay grows until the shared part is the
package declaration.

A branch costs a cherry-pick per fix, and only for fixes that matter to an older
line. Most do not.

## How a mod finds the right versions

It states the game version and nothing else:

```kotlin
plugins { id("fr.d4emon.fenix.dev") version "0.2.1" }
fenix { minecraft = "26.2" }
```

The plugin carries [`platforms.json`](https://github.com/D4EMONDEV/Fenix/blob/main/platforms.json) — every game version
Fenix has released for, and the module versions built for each — and looks up
whichever version the mod asked for.

```json
{
  "minecraft": "26.2",
  "branch": "main",
  "status": "current",
  "java": 25,
  "loader": "0.1.1",
  "api": "0.3.0",
  "ember": "0.2.0",
  "processor": "0.1.0"
}
```

Asking for a version that is not in the table fails at configuration, naming the
ones that are. That error is the whole point of the table. Before it, the plugin
knew the single pairing baked in when it was built: `fenix { minecraft = "26.3" }`
downloaded 26.3 and left the API coordinate pointing at the release for 26.2.
Nothing failed. The mod compiled against a jar for the wrong game and broke
later, at class loading, naming a missing Minecraft method — which reads as a
Fenix bug rather than as a mismatched pair.

### Why a table in the jar and not a lookup

The plugin could fetch the pairings from the Maven repository and always know
the newest. It does not, because that puts a network call in every mod's
configuration phase and makes an offline build fail for a reason that has
nothing to do with being offline.

The cost of baking it in is that a new game version needs a new plugin release.
That is not a cost: a new game version needs a new API release anyway, and the
two ship together.

## Releasing a new game version

1. Branch the outgoing version: `git switch -c mc/<old> && git push -u origin mc/<old>`.
   Set its entry's `status` to `maintenance` and its `branch` to `mc/<old>`.
2. On `main`, set `minecraft_version` in `gradle.properties`.
3. Fix what the game broke. Bump the module versions that changed.
4. Add the new entry to `platforms.json`, **first in the list** — the first entry
   is what a mod gets when it names no version.
5. Bump `version_gradle_plugin` and publish. Mods on older lines keep working
   with it; that is what the table is for.

Step 4 is checked, not trusted. `:fenix-gradle-plugin:checkPlatforms` fails the
build if `platforms.json` and `gradle.properties` disagree about the current
line, or if the current line is not listed first. Two files state the same
release and nothing about editing one makes you edit the other, so the build
refuses to produce a plugin where they have drifted.
