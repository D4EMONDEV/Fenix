# Testing the example mod by hand

`./gradlew :example-mod:runClient` launches the game with `example-mod`
installed. This is the list of what it puts there and how to see each thing,
because "it launched" only proves the loader started.

Everything below is in the **Example Mod** creative tab unless it says otherwise.
Give yourself creative mode and `/time set day` first.

## The quickest pass

Six things, in about two minutes, that between them prove the loader, the
registry, Mixin, the network and the client are all doing their jobs.

1. **Open the creative tab.** It exists at all → the registrar ran and the tab
   was added to a list vanilla builds once and freezes. Everything the mod
   registers is in it, and the build now fails if something is left out — but
   the check reads source, so it cannot tell you the tab *renders*, only that
   nothing was forgotten.
2. **Place a ruby block, break it.** It drops itself → the generated loot table
   loaded. A block that drops nothing means Ember's output never reached the
   game, which is silent otherwise.
3. **Place the stairs, the fence, the wall and the gate against each other.**
   The fence and wall should grow arms towards their neighbours and the stairs
   should turn corners. This is the only way to see that the generated
   blockstates are right — the conformance check compares them to vanilla's, but
   only your eyes check that the block has the properties to match them.
4. **Spawn a ruby sprite** with its egg. It should walk, float in water, and turn
   its head to look at you.
5. **Run `/wisp 3`.** Three wisps appear → commands registered through the event
   bus, which is what makes them survive `/reload`. Run `/reload`, then `/wisp`
   again: still there.
6. **Look at a ruby in your inventory.** The tooltip has a dark red extra line →
   the client-side tooltip event fired.

If all six work, nothing structural is broken.

## Everything, by area

### Blocks and shapes

`ruby_block`, `glowing_ruby_block` (it emits light), `ruby_ore` and
`deepslate_ruby_ore`, `ruby_log` and `stripped_ruby_log` (place a log on its
side — the end grain should stay on the ends), and the nine cut shapes:
`ruby_slab`, `ruby_stairs`, `ruby_fence`, `ruby_gate`, `ruby_wall`,
`ruby_trapdoor`, `ruby_button`, `ruby_plate`, `ruby_door`.

The door is two blocks tall and should open both halves together, **by hand** —
it uses the mod's own block set type, `example-mod:ruby`. If it only answers a lever, the set
type has lost its `openableByHand`.

The gate should sink into a wall placed beside it. Fences and walls connect
through `#minecraft:fences` and `#minecraft:walls`, not through their class, so
a fence standing alone in a row of fences means a tag file did not load — the
gate will keep joining regardless and is not evidence either way.

All nine are cut from a ruby block on a **stonecutter**, and both ores smelt.
Breaking a double slab should give **two** slabs back, and breaking either half
of a door should give **one** door, not two.

Break each of them with a pickaxe and check something drops. Seven of the nine
require a tool, and a `mineable` tag that is missing costs the drop rather than
the block.

### Ore generation

`/locate` will not help. Dig down in a fresh chunk, or `/setblock` is not the
test — the point is that ore appears in stone it generated into. Make a new
world, fly down to y=30 or so, and look. Ruby ore is added to overworld biomes
through a placed feature, so it only appears in chunks generated **after** the
mod was installed.

### The sprite, and the mod's own attribute

The spawn egg is `ruby_sprite_spawn_egg`. Once one is standing there:

```
/data get entity @e[type=example-mod:ruby_sprite,limit=1] attributes
```

should list `example-mod:ruby_charge` at 3.0 alongside the vanilla ones. That is
the whole point of the attribute — a stat vanilla has no word for, saved with
the entity.

The sprite also spawns on its own in the overworld, uncommonly. Bats do **not**
spawn any more: the mod removes them, which is easiest to see in a cave.

### Game rules

```
/gamerule example-mod:wisps_spawn false
/gamerule example-mod:wisp_limit 5
```

Both appear in the world-creation screen's rule list too. They are saved with
the world, not with the installation — make a second world and they are back at
their defaults.

### The reforging station and the safe

`ruby_reforging` and `ruby_safe` are block entities with screens. Right-click
each. The safe keeps what you put in it across a world reload; the reforging
table takes a ruby hammer and a ruby and gives back a repaired hammer, through a
recipe type the mod registered itself.

`ruby_tally` shows its count **on its own top face**, drawn by a block entity
renderer, and counts how many times you hit it — that number
travels client to server and back through the mod's own payloads.

### The jeweller villager

Place a `ruby_reforging` near an unemployed villager. They should take the
jeweller profession and offer ruby trades. This is the one that needs patience:
villagers claim a job site on their own schedule.

### Fluid, particle and effect

`ruby_brine` has a bucket. Pour some — it should render tinted red and flow.
`ruby_glimmer` is a status effect with its own icon; the `glimmering` potion
gives it.

### The client's own refusals

Swing at a `glowing_ruby_block` in survival. The swing is refused on the client,
before the server is asked, and a line appears above the hotbar — that is a
client block event. In creative it breaks normally.

### Commands

`/wisp [count]` throws wisps. `/rubyore <which>` takes the mod's own argument
type — press tab and it should suggest the ore names rather than nothing. That
suggestion is what proves a custom argument type was registered rather than
falling back to a plain string.

## What this does not cover

Nothing here checks the **server** side on a real server. `./gradlew
:example-mod:runServer` starts one, but it locks its world directory, so stop it
before running it again.

The sprite's model has never been looked at by anyone — it compiles and its
layer is registered, so it will not crash, but whether its texture is unwrapped
the right way round is a question only opening the game answers.
