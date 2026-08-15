# example-mod

The mod Fenix is tested with. It exists to use every part of the API at least
once, in the way a real mod would, so that anything broken is broken here first.

`./gradlew :example-mod:runClient` launches it.
[docs/testing-example-mod.md](../../docs/testing-example-mod.md) says what to
look at once the game is open.

## Layout

The packages are named after the API areas they exercise, not after Java
categories. `network/` holds what talks to the other side, `data/` holds what
writes files at build time, `registry/` holds the handles everything else is
reached by. Somebody looking for how Fenix does commands opens `command/`.

```
fr/d4emon/fenix/example/
  ExampleMod.java        the entry point: @Mod, and the listeners that outlive a world
  registry/              what gets registered, and the handles the rest of the mod holds
    ModContent            the registrar, and everything with no better home
    ModBlocks  ModItems
  block/                 the blocks with behaviour of their own
    entity/               the three block entities behind them
  item/                  RubyHammer
  entity/                RubyWisp (thrown), RubySprite (walks, has a brain)
  effect/                the status effect
  menu/                  the two screens' server halves
  recipe/                the reforging recipe: a recipe type the mod registered
  command/               /wisp and /rubyore, and the mod's own argument type
  network/               the payloads that travel between the sides
  config/                the config record
  worldgen/              the ore feature
  data/                  the Ember generators - models, loot, recipes, tags, two languages
```

The client half is a separate source set, because a server has no client
classes and a mod that mentions one on the wrong side crashes at load:

```
fr/d4emon/fenix/example/client/
  ExampleModClient.java  the client entry point, and its listeners
  ModKeys.java           key bindings
  render/                the sprite's model and renderer, the tally's renderer
  screen/                the two screens
```

## What it covers

Every mod-facing part of the API. Three classes are deliberately absent —
`CreativePages`, `VillagerJobSites` and `ModPackSource` — because Fenix calls
them itself and a mod never does.

| Area | Where |
|------|-------|
| Blocks, items, block entities, menus | `registry/`, `block/`, `menu/` |
| Nine cut shapes, with tags, loot and recipes | `registry/ModBlocks`, `data/` |
| Entities, attributes, spawn eggs, spawn table changes | `entity/`, `registry/ModContent` |
| Fluid, particle, status effect, potion | `registry/ModContent`, `effect/` |
| Recipe type and serializer | `recipe/` |
| Villager profession and job site | `registry/ModContent` |
| Commands, and a custom argument type | `command/` |
| Payloads both ways | `network/` |
| Game rules, attachments, block interactions | `registry/ModContent` |
| Config | `config/` |
| Server, level, player, entity, block and loot events | `ExampleMod.java` |
| Client, tooltip, HUD, key and client-block events | `client/ExampleModClient.java` |
| Entity and block entity rendering, fluid and particle rendering | `client/render/` |
| Ember: models, blockstates, loot, recipes, tags, languages | `data/` |
| Tags by constant and by name, tags holding tags | `data/ModTags` |
| Recipes taking a tag rather than one item | `data/ModRecipes` |

## Generated files

`src/main/generated` is written by `./gradlew :example-mod:ember` and committed.
Committed on purpose: the conformance checks read those files rather than
running the generators, so a change in what Ember writes shows up as a diff in a
review rather than as a surprise in a later build.
