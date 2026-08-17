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

### Data the mod ships

None of this is visible by walking around; it is checked with commands.

```
/advancement grant @s only example-mod:root
/advancement grant @s everything
```

The tab appears in the advancement screen with **Ruby Age** at its root. Four
of them: the root, the hammer, the nine shapes as a challenge, and either
spawn egg as a goal.

```
/enchant @s example-mod:ruby_edge 3
```

on a held sword. It is in the damage exclusive set, so it refuses to sit beside
Sharpness — that refusal is the test, not a bug.

```
/damage @s 4 example-mod:ruby_shard
/damage @s 4 example-mod:ruby_burn
```

Both should produce a death message naming the mod's own wording rather than a
blank line, and `ruby_burn` should be reduced by Fire Protection because it is
in `#minecraft:is_fire`.

Kill a ruby sprite: it drops zero to two rubies, more with Looting. A wisp
drops one. Neither had a table before.

Trade with the jeweller: one emerald buys a ruby, one ruby buys an emerald.

The painting and the banner pattern are drawn and should show:

```
/give @s minecraft:painting[minecraft:entity_data={id:"minecraft:painting",variant:"example-mod:ruby_vein"}]
```

Two blocks across, one high, a ruby seam through dark stone. The banner pattern
is applied at a loom with a banner and any dye — the shape is a cut gem, and it
takes the dye's colour because the texture is a mask rather than a picture.

### The biome, and the dimension that holds it

```
/execute in example-mod:ruby_realm run tp @s ~ 128 ~
```

A roofed, unlit cavern world made entirely of `example-mod:ruby_caverns`. Dark
red fog, no sky, ambient light at 0.1. Dig around: ruby ore generates here,
because the biome names it in the underground-ores step.

```
/locate biome example-mod:ruby_caverns
```

Run **inside** the dimension it will find one immediately. Run in the overworld
it will not, and that is correct: Minecraft has no datapack way to add a biome
to the overworld's noise settings, so a dimension of its own is the only door.
That is why the demo has one.

If `/execute in` answers *unknown dimension*, the pair of files did not load —
a dimension and its type are two files and both are needed.


The realm's ground is the mod's own ruby block, not stone — it has its own
noise settings now rather than borrowing vanilla's caves. Sea level is 48 and
rock gives way to air at 96, so arriving puts you underground: dig up. A realm
made of stone means the dimension is still pointing at `minecraft:caves`.

### The shrine

```
/place structure example-mod:ruby_shrine
```

A five-by-five ruby floor with four pillars and a glowing block on each. It
should appear where you are standing.

It also generates on its own, rarely — every 24 chunks on average:

```
/locate structure example-mod:ruby_shrine
```

That one needs a world generated **after** the mod was installed, like the ore.

The template is a `.nbt` written byte by byte rather than made in game, which is
possible and is not what Ember is for. If `/place` works and `/locate` finds
nothing, the structure set did not load; if `/place` produces empty air, the
template did not.

It should **not** come out pristine. A processor list weathers it on the way in:
roughly one block in ten missing, some moss, and about three stone bricks in ten
cracked. Place it a few times — the gaps fall in different places each time,
which is the processor running rather than the template having holes in it. A
shrine that is identical every time and complete every time means the pool piece
is not going through the list.

### The armour

Four pieces in the **Combat** tab: helmet, chestplate, leggings, boots. Put the
set on.

The armour bar should show 18 points, and the tooltip should mention toughness
1.5. That half is the material. The other half is whether you can **see** it on
the body — a mod's armour protects perfectly and renders invisible when its
equipment asset is missing, with nothing in the log either way.

Look at yourself in third person, or have a friend look. Then check the baby
variant, which is a separate texture and a separate way to be invisible:

```
/summon minecraft:zombie ~ ~ ~ {IsBaby:1b,ArmorItems:[{},{},{},{id:"example-mod:ruby_helmet",count:1}]}
```

### The advancement that counts your swings

Most advancements are earned by something vanilla can see. This one is not: the
count lives on the player as an attachment, and only the mod that keeps it can
say when it is high enough.

Take a ruby hammer and right-click blocks with it. Every fifth swing sends a
chat line with the running total. At **twenty-five**, the *Well Swung* toast
should appear, under *Something to Hit With* in the mod's advancement tab.

The number survives logging out, because the attachment is persistent — so
swinging fifteen times, quitting to the title screen, rejoining and swinging ten
more should still earn it.

Two ways for this to go wrong, and they look the same from inside the game.
If the trigger is registered and never fired, the advancement simply never
arrives and reads as a threshold that is too high. If it is fired and never
registered, the advancement file fails to load and the whole tab is short one
entry — check the tree rather than the toast.

### The music disc

`ruby_disc` is in the tab. Put it in a jukebox: the tooltip names **Ruby Waltz**
and a comparator beside the jukebox reads 11.

You should hear it — a three-note figure on a bell, four seconds long, then
silence and the jukebox stops. The disc item, the song file, the sound event and
the `.ogg` are four separate things, and only the last one makes noise. If the
tooltip is right and the comparator reads 11 and nothing plays, the first three
are fine and the audio is the one that is missing.

The four seconds matter: the song declares that length and the game trusts it
for the comparator and for when to stop. A track shorter than its declaration
leaves the jukebox holding a silence.

### The goat horn

There is no horn item, on purpose. Unlike a disc's song, an instrument is a
component on the **stack** rather than on the item type, so a horn is reached
by putting the component on one:

```
/give @s minecraft:goat_horn[minecraft:instrument="example-mod:ruby_horn"]
```

Blowing it plays the mod's chime and carries 128 blocks rather than vanilla's
256. Walk away from a friend blowing it: it should cut out at about half the
distance a vanilla horn does.

## What this does not cover

Nothing here checks the **server** side on a real server. `./gradlew
:example-mod:runServer` starts one, but it locks its world directory, so stop it
before running it again.

The sprite's model has never been looked at by anyone — it compiles and its
layer is registered, so it will not crash, but whether its texture is unwrapped
the right way round is a question only opening the game answers.
