# Changelog

All notable changes to Fenix are recorded here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and Fenix uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.7.0] — 2026-08-15

The release the game reported. Most of what follows was found by opening
Minecraft and looking: fences that would not join, a door that would not open,
three separate causes behind one magenta checker. Each is now covered by a check
that fails without it, because none of them said anything on their own.

Module versions: loader 0.1.2, API 0.5.0 (event 0.4.0, registry 0.4.0,
network 0.2.0, config 0.1.1), Ember 0.3.0, Gradle plugin 0.2.2.

### Removed

- **The website.** All of it: the pages, the generator, the editor, the deploy
  workflow and `docs/website.md`. Only the mark was kept, as
  `website/favicon.png` and `website/favicon.svg` — `logo.svg` was byte-identical
  to the favicon, so nothing else was lost.

  It is being rebuilt from nothing, last, once the API has stopped moving. The
  whole of it is in git history at `v0.6.0` for whoever does that.

  The deploy workflow went with it deliberately rather than being left in place:
  it triggers on changes under `website/`, so deleting the site would have run
  it, and it would have failed on every push after that.

### Fixed

- **A slab gave back one, and a door gave back two.** Both used `dropsSelf`,
  which is right for a cube and wrong for these. A double slab is one block
  holding two, so breaking it owed two and paid one. A door is two block states
  and breaking either breaks both, so it rolled its table twice and paid double.

  Neither says anything. The first reads as a miscount and the second is only
  noticed once somebody has been doing it on purpose for a week. Ember has
  `dropsSlab` and `dropsDoor` now, shaped exactly like vanilla's own tables,
  and the conformance check compares them to `oak_slab` and `oak_door` with the
  block names blanked so only the shape is compared.

- **The nine cut shapes could not be made.** Registered, modelled, named,
  dropping correctly, and obtainable only in creative — a decoration for
  screenshots wearing a survival block's coat. They have stonecutter recipes
  now, and both ores smelt and blast.

- **Five Gradle warnings per module, from Fenix's own plugin.** Reported here
  last time as something every Fenix mod developer sees. That was wrong: they
  only appear in a build whose settings claim the repositories with
  `PREFER_SETTINGS`, which is this repository and not an ordinary mod. The
  plugin now honours `fenix.repositories=false`, which this build sets and a
  mod leaves alone.

- **The loot-table codec check could not read its own new files.** It swapped
  modded ids out of `name` fields but not out of the `block` field of a
  `block_state_property` condition, so the first table to use one failed on the
  substitution rather than on the file. The stand-in is now chosen by the
  property the condition asks about, since a block that lacks the property is
  no better than a modded one the registry has never heard of.

- **Payload handlers ran on a Netty thread.** Both receiving mixins inject at
  HEAD, and HEAD is before the point where vanilla hands off to the game: the
  client's `handleCustomPayload` calls `ensureRunningOnSameThread` a few
  instructions in, and the server's is empty and never calls it at all. So
  every handler any mod wrote ran on the thread that read the bytes.

  It mostly works, which is the problem. The demo's handler asks
  `Minecraft.getInstance().player` for a chat line — what any mod would write —
  and the log shows it running on `Netty Local IO #1`. Under load it ends as a
  disconnect reading `Rendersystem called from wrong thread`, which names no
  mod, no channel and no handler.

  `Channels.deliver` now takes the executor to run on, and the mixins pass the
  client and the server. Only the handler is scheduled: whether a channel wants
  the payload is still decided immediately, because the caller cancels vanilla's
  own handling on that answer and cannot wait a tick to find out.

  `NetworkProbe` checks it: the handler must be queued, not called.

- **The first line Fenix prints was mangled.** The loader logs through
  `System.out` before anything better exists, and on Windows that carries the
  console's code page. An em dash is not in cp850, so `Fenix Loader 0.1.1 - client
  side` arrived with a replacement character where the dash was.

  Nine messages across six modules used one. They are ASCII now, and
  `LogTextConformanceTest` keeps them that way — comments, docs and
  translations are untouched, since those are read through tools that
  handle UTF-8.

- **The ruby door had see-through edges.** The first attempt at its textures
  left the three leftmost columns clear, on the belief that a door's hinge
  stile is empty in vanilla. It is not: `oak_door_bottom.png` is opaque in all
  256 pixels, and `oak_door_top.png` is clear only at its two window panes.

  Those three columns are exactly what `door_bottom_left.json` samples for the
  door's narrow sides, at uv [0,0,3,16] — so the belief cost precisely the
  faces it was wrong about, and the wide faces it did not touch looked correct
  throughout.

  `AssetConformanceTest` now reads the pixels: a model with a vanilla door
  parent has to name a texture that is opaque in the strips the edges come
  from. Verified by putting the transparent stile back.

- **Ruby fences and walls would not connect to their own kind.** A fence asks
  `BlockTags.FENCES` what counts as a fence, and a wall asks `BlockTags.WALLS`;
  the demo's were in neither, so each stood alone in a row of itself. The gate
  went on working the whole time and hid how broad the fault was — a gate is
  matched by class, not by tag, so it was the one join that never depended on
  the missing file.

- **Ruby doors and trapdoors only moved for redstone.** They were built with
  `BlockSetType.IRON`, and iron's answer to `canOpenByHand` is no. That is the
  iron door's whole character, borrowed by accident: it was the first metal in
  the list, not a decision. They use `COPPER` now, which is metal and opens by
  hand.

- **Seven cut shapes broke without dropping anything.** All seven declare
  `requiresTool()` and none was in a `mineable` tag, and a block that requires
  a tool no tag names cannot be broken for its drop by any tool at all. Found
  while fixing the fences, not by anyone playing — it takes a while to notice
  that a slab you mined gave nothing back.

- **A door with no textures, a bucket that could not be drawn, and a spawn egg
  with no name.** Three separate faults with one appearance, the magenta
  checker, which says something is missing and never which of the three files
  it is. The door's textures were described in a comment and never drawn; the
  bucket had a model but no definition in `items/`, which in 26.2 is what
  actually chooses a model; the egg had every asset and no translation.

  `AssetConformanceTest` now checks each link separately — that a texture a
  model names exists, that an item with a model has a definition, and that an
  item that can be drawn can be named. All three were confirmed by reproducing
  the original failures.

- **Half the demo's content was in no creative tab.** The nine cut shapes, the
  sprite's spawn egg and the brine bucket were registered, modelled, and
  unreachable in game except through `/give` — and so were the logs and ores,
  which had been missing since long before. Adding content and listing it are
  two edits, and only the first one is load-bearing, so the second was
  forgotten twice.

  Nothing reported it, because there is nothing to report: no log line, no
  exception, no failing check. It looks exactly like content that was never
  added, which is why it survived several rounds of testing the very blocks it
  hid.

  `CreativeTabConformanceTest` now reads the demo's own declarations and fails
  the build when one of them is in no tab.

- **Two conformance checks could not see the files they check.** Both read Java
  source as text — the demo's declarations, the model provider's shapes — which
  no classpath mentions, so Gradle held the test task up to date across every
  change to either. The first one written this way passed against a demo that
  had already been broken. They are declared inputs now, and the check re-runs
  when the thing it covers moves.

### Changed

- **example-mod is organised.** Twenty-seven classes sat in one package called
  `content`, which said nothing about any of them. They are in fourteen
  packages named after the API areas they exercise — `network/` for what talks
  to the other side, `data/` for what writes files at build time, `command/`
  for commands — so somebody looking for how Fenix does a thing opens the
  package named after it.

  Nothing changed but the arrangement, and that is checked rather than claimed:
  Ember's output is byte-for-byte identical after the move.

  It has a [README](examples/example-mod/README.md) now, with the layout and a
  table of what covers which part of the API.

### Added

- **Tags can be named by constant.** `tag(BlockTags.MINEABLE_WITH_PICKAXE)`
  beside the old `tag("minecraft:mineable/pickaxe")`. Both forms stay: the
  string is still the right answer for a mod's own tag, or one belonging to a
  mod that may not be installed.

  The constant moves the name from the player's problem to javac's. A
  misspelled string writes a perfectly valid file into a tag nothing reads —
  no log line, no warning, and the game carries on — which is how the demo's
  fences spent three releases standing alone in a row of fences. Both overloads
  are typed, so a block tag cannot be described by the item provider:

  ```
  error: cannot find symbol            symbol: variable FENCEZ
  error: no suitable method found for tag(TagKey<Item>)
  ```

  Verified by writing both mistakes and watching the build refuse them, and by
  checking that the two forms write byte-identical files.

- **Tags can hold tags**, through `addTag`, in both forms. Vanilla leans on
  this — `#fences` is `#wooden_fences` plus one block, not a list of every
  fence — and the demo now does the same: its nine cut shapes are one
  `example-mod:ruby_shapes` tag that the mineable tag refers to once.

- **Recipes can take a tag as an ingredient**, through `define(char, TagKey)`
  and `ingredient(TagKey)`. Most vanilla recipes are written that way, and a
  recipe naming one block where a family was meant works for that block and
  silently refuses the other eleven — the player who tried birch concludes the
  mod is broken.

- **Recipes for the three block entities.** The tally, the safe and the
  reforging station had none at all, so the parts of the demo that show the
  most were reachable only in creative. Two of the three take planks by tag.


- **Three API areas the demo had never used.** Block entity rendering shipped,
  was documented, had its own task marked done, and went four releases without
  the demo ever registering a renderer — so nothing would have noticed if it
  had never worked. Level events and client block events were in the same
  position.

  The tally block now draws its count on its own top face, levels are logged as
  they load and save, and swinging at a glowing ruby block is refused on the
  client before the server is asked.

- **`DemoCoverageConformanceTest`.** The demo is the only place the API is used
  the way somebody would actually write a mod, so an entry point it never calls
  is one nobody has ever called. This finds them: 36 static entry points, 27
  used by the demo, 9 named in an allowlist with a reason each.

  Its first version passed while the thing it checked was deleted, because the
  unused import left behind still carried the name. It reads the sources with
  the import lines stripped now.


- **`Registrar.blockSetType`.** A door, trapdoor, button and pressure plate
  share a block set type, and it decides two things that look unrelated: the
  sounds they make, and whether a hand can open them. Vanilla ships one per
  wood and one per metal, so a mod adding a door had to borrow somebody else's
  character — which is how the ruby door spent two releases refusing to open,
  and then a third sounding like copper.

  `BlockSetType.register` is private, so this widens it through an `accessible`
  entry. Registering rather than merely constructing is the point:
  `BlockSetType.CODEC` resolves by name out of a table only that method writes
  to, so an unregistered type cannot be read back. The probe checks all three —
  that it is in `values()`, that it opens by hand, and that its own name parses
  back to it.

- **Ember writes stonecutting, smelting and blasting recipes**, and loot tables
  for slabs and doors.

- **Two checks that registrations do something, not merely exist.**
  `BehaviourProbe` runs inside the real game and asks the two questions the rest
  of this module cannot: that a removed spawn is actually gone from the biome's
  table, and that a game rule the mod registered survives being written out and
  read back through the same codec a world save uses.

  Both are wiring that can be entirely in place and have no effect, and in both
  cases the symptom is an absence. A removal that quietly did nothing looks like
  a mob that is rare today; a rule that does not survive a save looks like a
  player who forgot they changed it, and will insist they did not.

- **`example-mod` has a living creature.** `RubySprite` is a `PathfinderMob`
  with health, movement speed, follow range, four goals and the mod's own
  attribute — plus a spawn egg, a name, and a place in the overworld's spawn
  table. Everything in the API that deals with living things went
  undemonstrated until now, because the only entity was a thrown projectile with
  no brain and no attributes.

  It closes the gap left two releases ago: `Registrar.attribute` had nothing
  carrying it, which is exactly the mistake its own documentation warns about.

  It also showed a wart in that method. `AttributeSupplier.Builder.add` wants
  the game's own `Holder`, and `Registrar` hands back Fenix's, so the demo has
  to call `BuiltInRegistries.ATTRIBUTE.wrapAsHolder(...)` — ceremony of exactly
  the kind Fenix exists to absorb. It is written down where it happens, and the
  method should return something usable directly.

  It is drawn, too: a model with hand-written geometry, a layer registered
  through `EntityModels`, a `MobRenderer`, and a placeholder texture the build
  generated rather than leaving the magenta checker. Between them they exercise
  the whole client path a mod's own creature takes — which nothing did before,
  because the wisp reuses a vanilla renderer.
- **`EntityAttributes.holder`** — bridges Fenix's holder to the game's, which is
  what `AttributeSupplier.Builder.add` wants. Without it a mod wrote
  `BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute.get())` in the middle of a
  builder chain, which is the ceremony Fenix exists to absorb; the mob demo did
  exactly that for one release and said so in a comment.

  The two holders cannot simply be one type. Fenix hands its own back *before*
  the attribute exists, so content can be declared in a field; the game's is a
  reference its registry creates at registration, and there is nothing to hand
  back until then.
- **`PlayerEvents.PICKED_UP` and `CHANGED_DIMENSION`.** Picking an item up
  reads the stack at HEAD, while it is still what was lying on the ground — by
  the time the method returns it has been merged into whatever the player
  already had and its count no longer says what was collected. It fires on the
  server alone, because both sides call that method and firing on the client
  would double every count a listener keeps.

  The dimension change hooks the private method the game calls to award the
  travel advancements, which is the one place that knows both ends of the trip.
  Injecting on `teleport` would fire for every teleport inside a dimension too
  and would have to guess whether the world actually changed — a guess that is
  right until somebody teleports to the same coordinates in the same world.

- **Two more events, one per side.** `EntityEvents.INTERACT` fires when a player
  right-clicks an entity, cancellable, hooked on `Player` rather than on the
  packet handler so the client's own prediction fires it too — a listener that
  only ever saw the server would let the client open a screen the server then
  refuses, which reads as the screen flickering shut. It fires on both sides, and
  the documentation says so, because a listener that changes the world without
  checking `isClientSide` does it twice in single-player.

  `ClientEvents.SCREEN` fires whenever the client changes screen, including to
  nothing — where a mod adds a button to somebody else's screen, or notices an
  inventory closing. At HEAD, before the screen is initialised, which is the only
  moment a widget can be added to the list it builds.

  Both are new mixin files and both are in the ASM check, confirmed to fail with
  one dropped from its config.
- **Three events the everyday cases needed.** `EntityEvents.HURT` fires before
  anything living takes damage and cancelling it stops the hit outright — no
  knockback, no animation, no death. `PlayerEvents.USE_ITEM` is the right-click
  that hits no block, which is what eating, drinking, throwing and drawing a bow
  all begin as; `BlockEvents.USE` was only ever the other half.
  `ServerEvents.STOPPING` is the last moment a mod can write anything while the
  world can still be reached.

  All three inject into classes Fenix already had a mixin on, so no new mixin
  file and no config change.

  The ASM check that proves injections still land kept **one** handler per
  target class, which was fine until three events arrived on classes that
  already had one — it would have checked the old name and shadowed the new. It
  now takes a list per class. Confirmed to fail with the injection point
  renamed.

- **`Registrar.gameRule`** — a custom game rule, in both the shapes vanilla has:
  a boolean and a bounded integer. It appears in `/gamerule`, in the
  world-creation screen, and is saved with the world rather than with the
  installation — which is what makes it the right home for "should this mod's
  thing happen at all". A config file is per install; a game rule is per world
  and can be changed by somebody who cannot edit files.

  In 26.2 game rules moved to `net.minecraft.world.level.gamerules` and became a
  real registry, with an eight-argument constructor taking a codec, an argument
  type, a visitor and a feature-flag set. Those are wired the way the game wires
  its own, read out of its source rather than guessed: a rule built with the
  wrong visitor or codec registers, appears, and then fails to save or to show in
  the creation screen, none of which is a crash.

  An integer default outside its own range is rejected. The game accepts it at
  registration and only complains the first time somebody resets the rule, which
  is a long way from the line that caused it.

  `example-mod` declares one of each. The headless game boots with them
  registered, which proves the construction is valid; that a rule round-trips
  through a saved world is not covered by a test.
- **`Registrar.attribute`** — a named number every entity carries, which
  equipment, effects and other mods can add to. This is how a mod gives entities
  a stat vanilla has no word for — mana, weight, a resistance of its own —
  without keeping a map on the side, and it is one of the everyday things Fabric
  and NeoForge mods do that Fenix had no answer for.

  A base value outside its own range is rejected rather than passed on. Vanilla
  clamps silently, so the attribute would hold a different number than the one
  written and nothing would say which.

  Not demonstrated in `example-mod`: an attribute belongs to a `LivingEntity`
  and the example mod's only entity is a plain `Entity`. Registering one that
  nothing carries is exactly the mistake the method's own documentation warns
  about, so the demo waits for a mob that can hold it.

- **`door` in `EmberModelProvider`** — the thirteenth shape and the last one
  missing: two blocks tall, hinged on either side, open or shut, thirty-two
  states answered by eight models. Unlike the others it takes three textures of
  its own rather than borrowing, because neither half of a door reads as a full
  block and the item is a flat picture that reads as neither half.
- **`BiomeModifications.removeSpawn`** — the counterpart, and what a mod that
  reshapes the world rather than adding to it needs: taking a mob out of one
  biome, or out of everywhere so the mod's own replaces it.

  Removals are applied after every addition, so a removal wins against another
  mod adding the same mob. The opposite order would let a mod quietly undo a
  removal by registering later. Removing something that was never there is not
  an error — a selector covering the overworld matches plenty of biomes that
  never had the mob — so the log reports how many entries actually went, which
  is the number worth reading. A removal that matched nothing rebuilds nothing,
  because otherwise every non-match would allocate a map per biome per load.

  `example-mod` now takes bats out of the overworld.
- **`BiomeModifications.addSpawn`** — a mob appears naturally in every biome a
  selector matches. `addFeature` already put an ore in the ground; this is its
  missing counterpart, and without it registering an entity gave you one that
  could only be summoned by hand with nothing anywhere saying why. That is the
  commonest reason a new mob is never seen.

  It takes the `Holder` the registrar returned, and reads it when biomes load.
  The first version took the `EntityType`, which meant calling `.get()` — and a
  holder is not bound until the registrar is applied, so the natural place to
  say where a mob spawns, beside the line that registers it, was exactly where
  it threw. The demo hit that on its first run. Taking the holder removes the
  order from the problem entirely; the overload taking a type is still there for
  a vanilla mob.

  A weight of zero is rejected rather than passed on: the game accepts it, never
  picks it, and leaves a correct-looking line to stare at. The log says what
  happened once per mob, and its absence is the answer to "why is my mob never
  anywhere".

  It needed a mixin on `MobSpawnSettings`, whose table is built immutable when a
  biome loads. The map is replaced rather than written through — the loaded
  registries are shared, so editing the original would either throw or change
  something another world is using — and it is an `EnumMap`, because the game
  reads it for every spawn attempt in every loaded chunk.

  `example-mod`'s ruby wisp now spawns in the overworld. The ASM check that
  proves the feature path still lands now proves the spawn path too, all three
  steps of it, and was confirmed to fail when the mixin is dropped from its
  config — which is how a mixin stops applying in practice.
- **Twelve more shapes in `EmberModelProvider`.** It knew three — a cube, a
  pillar and a flat item — which covers a decorative block and nothing a block
  usually grows into. It now writes slabs, stairs, fences, fence gates, walls,
  trapdoors, buttons, pressure plates, plants, top-and-bottom cubes, blocks with
  a face, and items held like a tool.

  Each takes its textures from another block, the way vanilla's oak slab uses
  the oak planks texture, so a whole family of blocks costs no new artwork.

  The rotations are vanilla's own, read out of the game's blockstate files
  rather than remembered — and two of them **were** remembered wrongly first: a
  furnace and a button are drawn facing north, not south, and every one of their
  variants would have faced ninety degrees off. That is not a crash. The block
  renders, pointing somewhere else, and only somebody standing in the right
  place ever notices.

  `example-mod` gains a ruby slab, fence, wall and gate — between them the four
  blockstate shapes the writer has to get right: simple variants, multipart with
  boolean sides, multipart with three-valued ones, and sixteen rotated variants.
  A check compares each against the vanilla block it is modelled on, with the
  model names normalised away, and all four come out identical.

### Fixed

- **The model conformance check crashed on any multipart blockstate.** It read
  `variants` and nothing else, so the first fence generated turned it into a
  `NullPointerException` rather than a check. It now reads both shapes, and was
  confirmed to still fail when a model a fence names is taken away.

- **Videos in a page.** `:::video <url>` plays a file inline or embeds a YouTube
  link on `youtube-nocookie.com`, with an optional caption. A recording says in
  six seconds what a paragraph says badly, and Markdown has no syntax for one.
- **An editor in the site.** `/admin` — reachable from **Contribute** in the
  header — lists the pages, edits one with a live preview beside it, and commits
  straight to the repository through GitHub's API. A fine-grained token scoped
  to this repository is kept in the browser and sent nowhere else; there is no
  backend for it to pass through, which is why this works on a static site. A
  save quotes the hash it started from, so an edit made from another tab is
  refused rather than overwritten. Every page also carries an "Edit this page on
  GitHub" link.

  Content stays in git — diffs, history, and a way back. A database would have
  had none of that.
- **Pages can be created, not only edited.** A section, a file name, a title, a
  description and an order; the front matter is written for you, because a page
  without it has no name in the sidebar and nothing says so. The create refuses
  a path that already exists, the same way a save refuses a stale one.

  The page list is read from the repository rather than from this site's own
  bundle — the bundle is a snapshot of the last build, so a page added a minute
  ago would not be in it, and looking for it there is the first thing anybody
  does after making one.
- **The site is responsive**, at three widths that each drop what stops
  mattering rather than shrinking everything: the contents list goes first, then
  the sidebar's stickiness and the second column of every pair, then the
  header's navigation. Checked at 1280, 768 and 375 across the home,
  documentation, generator and editor — no page scrolls sideways, and code
  blocks scroll inside their own box.
- **More on the home page**: the build file, the three commands that cover most
  of a day, what is different, and the current release — in bands of alternating
  surface, so a long page has a rhythm.
- **[Writing these pages](website/content/0.1/reference/writing-docs.md)**,
  documenting the front matter, the callouts, the video syntax, code titles and
  where a file goes.

### Fixed

- **The `:::` syntaxes were rewritten inside fenced code blocks.** They run
  before Markdown is parsed and so could not tell a `:::video` meant to be shown
  from one meant to be acted on. The page documenting the syntax was the first
  casualty: its examples were replaced by the HTML they produce, so the one page
  explaining how to embed a video was the one page that could not show you.
  Fences are now lifted out before the rewrite and put back after.
- **The editor's preview showed the front matter as a heading.** It rendered the
  whole file, while the real page renders the body — so the one screen whose job
  is to show what a page will look like was the one showing something else. Both
  now go through the same `stripFrontMatter`.

### Changed

- **The documentation reads like documentation.** Three columns with room in the
  middle, collapsible sidebar groups, the game version beside the page title,
  previous/next links at the foot, and line numbers and an optional file path on
  every code block — `title="…"` was already written in these pages and was
  being parsed off and thrown away. Line numbers are their own column rather
  than woven into the markup, because the highlighter can leave a span open
  across a newline and splitting its output would tear the tags apart.
- **The home page is a landing page**: the mark, the name, one sentence, and the
  two doors somebody actually arrives through.
- The installer has been run against a real `.minecraft` and works;
  `docs/roadmap.md` said it was untested.

## [0.6.0] — 2026-08-12

A listener that throws no longer takes the game with it, a generated project can
come with nothing in it, and the website is composed again rather than merely
plain.

Published artifacts that changed: the event module `0.2.0` → `0.3.0`, the API
set `0.3.0` → `0.4.0` because one of its modules moved, and the Gradle plugin
`0.2.0` → `0.2.1` because it carries `platforms.json` and that now names the new
API. Loader `0.1.1`, Ember `0.2.0`, processor `0.1.0` and installer `0.1.2` are
unchanged.

### Fixed

- **A listener that throws is contained.** `Event.fire` and
  `CancellableEvent.fire` let the exception out, and an event is fired from
  inside Minecraft — so one broken listener ended the game with a crash report
  naming a vanilla method, and stopped every listener registered after it, none
  of which had done anything wrong. It is now caught, logged at `ERROR` with the
  stack trace and the listener named, and the event carries on. For a
  cancellable event a listener that threw counts as `CONTINUE`: it decided
  nothing, and cancelling on its behalf would let one broken mod silently veto
  everything the event guards.

  `Error` is deliberately still not caught — an `OutOfMemoryError` is not a
  listener misbehaving.

  Covered by a test on each path, and each was checked by removing the
  containment and watching it fail with the listener's own exception. The logger
  is the JDK's `System.Logger` and not Minecraft's: the event bus is otherwise
  plain Java, which is what lets it be unit-tested in milliseconds, and reaching
  for `com.mojang.logging.LogUtils` for one line put the game on the test
  runtime classpath and broke every test in the module.

### Added

- **Starter content is an option.** Off, a generated project is its entry point
  and its build files — no `ModContent`, no `ModBlocks`, no `ModItems`, no
  generators, no placeholder art. The resource folders are still there and still
  empty, so nobody has to work out where a texture goes. That needed real
  directory entries in the zip writer: a zip has no other notion of a folder,
  and a `.gitkeep` put there to outwit git is exactly the content that was not
  wanted. Git still will not track them, which is git's business and is said in
  the generated README.
- **Ember is offered whether or not there is starter content.** The Gradle
  plugin puts Ember on every mod's compile classpath and registers `ember`
  whatever the generator wrote, so wanting it without the starter block is an
  ordinary thing to want — all that was missing was somewhere to put a
  generator, and the empty `data` package is now written for it. The checkbox
  used to grey itself out here, which said the opposite of what was true.
- **The generated mod class keeps its id in a constant.** `@Mod(MyMod.MODID)`
  rather than a literal repeated in the annotation, the registrar, the client
  half and every resource path. It has to be a compile-time constant to sit in
  an annotation, which `static final String` with a literal is; the annotation
  processor resolves it and writes the same index it always did — checked by
  reading `fenix.index.json` and `fenix.index.client.json` out of a built jar,
  because a build that succeeds while the index comes out empty is exactly the
  silent failure this would hide.

### Changed

- **The website is composed again rather than merely plain.** The previous pass
  took the decoration off and stopped there, which left a page with no mark on
  it, one type size and nothing to look at. The mark is back and given a surface
  of its own, the sections alternate between the dark page and the paper band,
  and the type has a scale. No orbits, glows or fake terminal chrome return.
- The file tree in the generator draws folders that are meant to stay empty, and
  the starter content moved to `template-content.ts` — it is most of what the
  generator writes and none of what the generator decides.

## [0.5.0] — 2026-08-12

A `fenix { }` block that reads as one list, a generator that asks three
questions instead of eight, and a website with the decoration taken off.

One published artifact changed: the Gradle plugin, `0.1.6` → `0.2.0`. The minor
rather than the patch, because the extension's property names changed and a
build using the old ones no longer configures. Everything else keeps the
versions 0.3.0 pinned — loader `0.1.1`, API set `0.3.0` (registry `0.3.0`,
event `0.2.0`, resource `0.1.1`, core `0.1.0`, network `0.1.0`, command `0.1.1`,
config `0.1.0`), Ember `0.2.0`, processor `0.1.0`, installer `0.1.2`.

### Migrating

| Before | Now |
|---|---|
| `fenix { loaderVersion = "…" }` | `fenix { loader = "…" }` |
| `fenix { apiVersion = "0.3.0+mc26.2" }` | `fenix { api = "0.3.0" }` |
| `fenix { api = false }` | `fenix { bundle = false }` |

A build that names only `minecraft` needs no change, which is nearly all of
them.

### Changed

- **`fenix { }` names every Fenix version, one line each.** `loader`, `api` and
  `ember` are now overridable, which is what testing an unreleased loader or a
  locally built Ember looks like. Each defaults to the lookup for the game
  version, so a build that names only `minecraft` is unchanged.

  Two renames come with it, and they break a build that used the old names.
  `loaderVersion` is now `loader`, so the block reads as one list rather than
  one property spelled unlike its neighbours. `apiVersion` is now `api` — which
  the boolean previously held, so that boolean is now `bundle`, named for what
  it turns off: `fenix-api` is a bundle jar carrying every module, and a mod
  that sets it to `false` is still very much using the API.

  `api` and `ember` accept a bare version or one carrying `+mc26.2`; the suffix
  is appended from `minecraft` when it is missing, because that is the number
  a build file actually wants to write.
- **The project generator asks three questions instead of eight.** Mixins,
  networking, commands, configuration and CI are gone from it. A checkbox per
  feature produces a project that is a tour of the API rather than a starting
  point, and every box left ticked is code somebody reads before deleting.
  What is left changes the shape of the project rather than its text: Ember,
  whether to split `src/client` from `src/main`, and Kotlin or Groovy build
  scripts. The mod id follows the mod name until a checkbox hands it over.
  Verified by building all four combinations of Ember and build-script language.
- **The generated preview shows a real tree.** Connectors rather than a flat
  list grouped under path headings, with single-child directory chains folded
  the way an IDE folds middle packages — `com/example/mymod/` on one row instead
  of three, which is most of the depth of a Java project and none of its shape.
- **The site is plainer.** The orbit rings, radial glows, gridded backdrop,
  iconed cards, numbered feature columns and fake terminal chrome are gone.
  They were decoration standing in for content on a page whose job is a
  sentence and a code sample.

## [0.4.0] — 2026-08-12

One resolution bug that made Fenix unusable on a machine with a half-populated
`~/.m2`, the groundwork for releasing against more than one Minecraft version,
and a website whose documentation is now written rather than generated.

Only one published artifact changed: the Gradle plugin, `0.1.5` → `0.1.6`. The
loader, the API modules, Ember, the processor and the installer are untouched
and keep the versions 0.3.0 pinned — loader `0.1.1`, API set `0.3.0`
(registry `0.3.0`, event `0.2.0`, resource `0.1.1`, core `0.1.0`,
network `0.1.0`, command `0.1.1`, config `0.1.0`), Ember `0.2.0`,
processor `0.1.0`, installer `0.1.2`.

### Fixed

- **A mod could not resolve Minecraft's libraries when `~/.m2` held a partial
  copy of one.** `addRepositories` put an unscoped `mavenLocal()` first, so any
  module with a pom in a developer's local Maven cache was claimed from there —
  and Gradle does not fall back to another repository once one has claimed a
  module. Maven routinely leaves a pom behind without the classified jars beside
  it, which is how a real build failed on `com.mojang:jtracy` with only the
  natives jar missing. The error named a Mojang library and a path in `~/.m2`,
  so it read as a corrupt cache rather than as a line in the Fenix plugin.
  Both `mavenLocal()` and the Fenix repository are now restricted to Fenix's own
  coordinates; Minecraft's libraries have no business coming from `~/.m2`
  anyway. Verified by reproducing the failure against the same half-populated
  cache, and by putting the unscoped call back to confirm the failure returns.

### Added

- **Fenix can be released for more than one Minecraft version.** The API is
  compiled against the game, so a release belongs to one game version and to no
  other — but the Gradle plugin knew exactly one pairing, the one baked in when
  it was built. Setting `fenix { minecraft = "26.3" }` downloaded 26.3 and left
  the API coordinate pointing at the release for 26.2. Nothing failed at
  configuration time; the mod compiled against a jar for the wrong game and broke
  later, at class loading, naming a missing Minecraft method — which reads as a
  Fenix bug rather than as a mismatched pair.

  `platforms.json` now carries every game version Fenix has released for and the
  module versions built for each. The plugin ships it and looks up whichever
  version the mod asked for; asking for one that is not in the table fails at
  configuration, naming the ones that are. A table baked into the jar rather
  than fetched keeps a mod's build offline and reproducible, and costs only a
  plugin release per game version — which a game version needs anyway.

  `:fenix-gradle-plugin:checkPlatforms` fails the build if the table and
  `gradle.properties` disagree about the current line, if the current line is
  not listed first, or if the plugin version named for the website is not the
  one being built. Two files stating the same release is exactly the drift this
  release is about. [`docs/game-versions.md`](docs/game-versions.md) covers the
  branch strategy that goes with it.

### Changed

- **The API reference is no longer generated from Javadoc.** `apiDocsSite` and
  the `tools/api-doclet` module are gone, and with them the twenty-odd package
  pages the site carried. A generated reference says a method exists; it does not
  say which of two ways survives a datapack reload, or which mistake fails
  silently. The documentation is now written by hand, against signatures read
  from the source — new guides for events, client-side code, networking,
  commands, configuration, and mixins and access widening. `apiDocs`, the plain
  browsable Javadoc an IDE reads, is untouched.
- **The project generator produces a complete project.** Options for a client
  source set, content, Ember, mixins, config, commands, networking, a licence and
  a GitHub Actions workflow — and placeholder textures the generator draws, so
  the first launch shows a block rather than the magenta-and-black checker that
  reads as a broken mod. Verified by generating three configurations and building
  each: everything on, everything off, and content without Ember.
- **The website reads its version numbers from `platforms.json`.** The Minecraft
  version, the API version, the plugin version a build file applies and the
  versions written into a generated project all came from constants in the site's
  own source, which every release had to remember to edit. A generated project
  naming a version nobody published fails on its first build, and the visitor has
  no way to tell it was the website that was wrong.

## [0.3.0] — 2026-07-23

Three more registry gaps closed, Ember grown in three directions, and the site
repaired — plus a Gradle bug that made every one of those changes invisible
until the output directory was deleted by hand.

Module versions this release pins: loader `0.1.1`, API set `0.3.0`
(registry `0.3.0`, event `0.2.0`, resource `0.1.1`, core `0.1.0`,
network `0.1.0`, command `0.1.1`, config `0.1.0`), Ember `0.2.0`,
processor `0.1.0`, installer `0.1.2`, Gradle plugin `0.1.5`.

### Added

- **Ore loot tables that behave like ore.** `EmberLootTableProvider.dropsOre`
  writes what a player assumes the moment a block looks like ore: its material
  normally, itself under Silk Touch, more under Fortune. The plain `drops` table
  gives none of the three — Silk Touch yields the material like any other pick
  and Fortune does nothing — and nothing says the table was the reason, so it is
  a block that works and feels broken. `example-mod`'s ruby ores had exactly that
  table until now. `dropsWithSilkTouch` covers the other shape, the one glass
  has. A conformance check parses every generated loot table with Minecraft's own
  codec, which is the only thing that would notice the format changing under
  Fenix in a game update.
- **Ember writes any language, not only English.** `EmberLanguageProvider` takes
  a language code — `super("fr_fr")` from a second generator, since generators
  are built through their no-argument constructor. The code is checked, because
  `fr_FR`, the shape Java's own `Locale` prints, writes a file the game never
  looks for: nothing fails, the mod is simply untranslated, and the author is the
  one person who cannot notice because they read the language they wrote.
  `example-mod` now ships French. A conformance check compares every other
  language against English and refuses a key English does not define — one that
  exists only in a translation never displays, which usually means a key renamed
  on one side only.

- **Entity model layers.** `EntityModels.register` lets a mod's entity have a
  model of its own. Vanilla builds the entire layer table in one method and
  hands it back immutable, so there was nothing to add to — a Fenix mod could
  only reuse a vanilla renderer, which is why the example wisp borrowed the
  thrown-item one. A renderer asking for a layer that is not in the table throws
  `No model for layer` from its own constructor, while the client is loading.
- **Pillar models in Ember.** `cubeColumn` writes the upright model, the
  horizontal one and the three-axis blockstate — the shape that keeps a log's
  end grain when it is laid on its side. `example-mod`'s ruby log used `cubeAll`
  as a stand-in until now. A new conformance check reads every generated
  blockstate and asserts each model it names was actually written: a blockstate
  pointing at a file nobody generated draws the missing-model cube, which reads
  as a texture problem and is a filename problem, and nothing logs it.
- **Custom command argument types.** `Registrar.commandArgument` fills both
  tables vanilla keeps: the registry, and a map keyed by the Brigadier class
  that it reads while writing the command tree for a joining player. Only the
  first is obvious, and a mod that stops there has a command that works
  perfectly in single player and then stops anybody connecting — the failure is
  `Unrecognized argument type`, thrown from inside the join, naming a Brigadier
  class and no mod. The conformance check calls the very method that throws, and
  fails when the second table is skipped.
- **Block entity renderers.** `BlockEntityRendering.register`, for the part of a
  block its model cannot express — a chest lid, a sign's text, an item turning
  above a pedestal. Vanilla's table is a private static map with a private
  `register`, so a mod had no way in, and a block entity without a renderer draws
  nothing while the block itself renders perfectly. Nothing logs, because from
  vanilla's side nothing is wrong.

### Fixed

- **The website rendered unstyled.** The home page had been rewritten to markup
  whose classes had no CSS at all, so it drew as a column of underlined links,
  and the documentation grid had gone the same way — its class renamed without
  the stylesheet following, which also left the sidebar unable to collapse on a
  narrow screen. Rebuilt on the design the stylesheet already carried. Two more
  came out of checking rather than looking: the page scrolled sideways by 63px on
  a phone, because a grid child defaults to `min-width: auto` and refused to
  shrink below the code block inside it; and the light theme drew near-black text
  on near-black cards, because the contrast surface flips with the theme and the
  colours written on it were hardcoded. Contrast on those cards went from about
  1.0 — invisible — to 15.8.
- **Documentation links no longer carry the version.** They named it in full, so
  every release meant rewriting a dozen of them by hand, and one release meant
  getting them wrong. They are written `/docs/@latest/…` now and resolved when
  the page renders.
- **`gradlew ember` did nothing after a change.** The task declared its output
  directory and no inputs at all, so Gradle judged it up to date whenever that
  directory was unchanged, however much the mod had moved — add a generator, run
  `ember`, and nothing happens. It now takes the mod's compiled classes as its
  input, which is where both the generators and the index naming them live.
  Classes rather than the whole source set output, because `src/main/generated`
  is itself a resource directory and taking the resources would make the task's
  input contain its own output. Found by adding a generator and watching nothing
  happen — and the first fix was incomplete: it took the mod's classes but not
  Ember's own jar, so updating a generator left a stale directory behind. Found
  the same way, by sabotaging a generator and watching the output not change.

## [0.2.0] — 2026-07-23

Six new areas of the API, and the two fixes that made the demo honest: every
new world asking to confirm experimental features, and a Gradle plugin that
named artifact versions nobody had published.

Module versions this release pins: loader `0.1.1`, API set `0.2.0`
(registry `0.2.0`, event `0.2.0`, resource `0.1.1`, core `0.1.0`,
network `0.1.0`, command `0.1.1`, config `0.1.0`), Ember `0.1.1`,
processor `0.1.0`, installer `0.1.2`, Gradle plugin `0.1.4`.

### Added

- **Potions and brewing.** `Registrar.potion` registers a potion — taking the
  effect as a handle and building the instance later, so it can be declared
  beside the effect it names rather than after it. `Brewing.mix` and its
  neighbours say what makes it. The second half is the one that is easy to miss:
  vanilla builds its brewing table once per server from a fixed list and throws
  the builder away, so a registered potion nothing brews into can be given by
  command and made by no brewing stand anywhere. Fenix catches the builder while
  it is open and fills it through the same public methods vanilla just used, so a
  mod's mixes also survive a datapack reload. A conformance check reads the mix
  back out of the table a server actually builds.
- **Loot table modification.** `LootEvents.LOADING` fires for each table as it is
  read, and `addPool` adds to it rather than replacing it — so two mods can both
  drop something from stone, which is exactly what overriding the file in a
  datapack cannot do: the second copy wins and the first mod's drop is gone with
  nothing said. Loot tables became a datapack registry, so there is no "load one
  table" method to hook; this catches them while they are still a map, the one
  moment they are both parsed and still changeable. Rebuilding a table needs its
  private constructor, which Fenix widens from the manifest — a conformance check
  runs the whole thing through a real launch, since nothing about that
  transformation is visible on an ordinary classpath.
- **HUD rendering.** `HudRenderEvents.RENDER` fires once a frame after vanilla's
  own HUD, only while a world is on screen. Taken on `Hud` rather than `Gui`,
  where the pass looks like it starts: `Gui.extractRenderState` builds the
  graphics object as a local and hands it down, and one level in it is a
  parameter — the same moment, without capturing a local.
- **Two more events.** `ItemTooltipEvents.BUILD` fires as a tooltip is assembled
  and hands over a live, writable list, so a mod can place a line relative to
  what is already there rather than only at the end. `ClientEvents.CONNECTED`
  and `DISCONNECTED` fire when this client joins or leaves a world — the latter
  at `close`, which covers being kicked and the connection dropping, not only a
  polite exit, since those are the occasions stale per-world state matters most.
- **Block interactions — the behaviour vanilla keeps in tables.** `BlockInteractions`
  covers flammability, composting, stripping, waxing, oxidation and furnace fuel,
  one line each. Every one of those lives in a table somewhere else in the game,
  filled once at bootstrap from a list of vanilla's own content, so a modded wood
  type looks and behaves like wood and quietly is not: it will not catch fire, an
  axe does nothing to it, and a furnace refuses it. Nothing warns, because from
  vanilla's side nothing is wrong — the block simply is not in the table. The
  immutable tables are answered ahead of; the waxing table, which four unrelated
  places read inline, is replaced whole so every reader finds mod entries in both
  directions; the composter's, which is mutable, is written into at `apply()`.
  Blocks are resolved the first time the game asks, so these can be declared in
  any order relative to registration. Five of the six are proven by behaviour
  against real Minecraft — asking vanilla's own lookups what a modded block burns
  like, strips into, waxes into and weathers into — and each was verified to fail
  when sabotaged.
- **Custom recipes.** `Registrar.recipeType` and `Registrar.recipeSerializer`
  register the two halves a mod's own recipe kind needs — the type a station
  looks up by, and the pair of codecs that read the recipe from a datapack and
  send it over the wire. A mod writes the recipe itself on vanilla's
  `SingleItemRecipe` (the stonecutter's base) or any `Recipe`, and its station
  finds a match with `getRecipeFor(TYPE, input, level)`. `example-mod` gains the
  crafting screen the roadmap asked for: a reforging table — a block, a two-slot
  menu, a block entity that reforges on a timer, and a recipe type of its own.
  A conformance check registers a custom type and serializer in real Minecraft
  and round-trips a recipe through its own codec.
- **Villager professions and trades.** `Registrar.poiType` registers a job-site
  point of interest — and redoes the block-state bookkeeping vanilla does once
  at bootstrap, without which the job site is in the registry but invisible to
  the villager AI, so the profession exists and no villager ever takes it.
  `Registrar.villagerProfession` registers the profession that claims it. Trades
  became datapack data in 26.2 — a `trade_set` of `villager_trade` entries a
  profession names by level — so the API registers the profession and points it
  at the sets, which a mod ships as JSON; `example-mod` adds a jeweller who works
  at the reforging table and trades in rubies. A conformance check proves the
  profession and point of interest register, that the job-site block resolves to
  the point of interest, and that the profession accepts it — each verified to
  fail when sabotaged.

  A mod must also add its job site to the `minecraft:acquirable_job_site` tag,
  and Fenix now says so when it has not: an unemployed villager searches with the
  `none` profession's predicate, which is that tag rather than the registry, so a
  job site outside it is never looked for and every other part can be correct
  while no villager ever takes the job. Fenix does not write the tag — a datapack
  may legitimately override it — but the moment the tags bind it names the
  profession, the job site and the file to write. Found by playing the demo, not
  by the suite, which is why two checks were added: one that the guard can see
  the failure, one that `example-mod` ships the tag.
- **Fluids, in one call.** `Registrar.newFluid` registers the four things a
  fluid actually is — a still form, a flowing form, the block it becomes in the
  world, and the bucket that carries it — wired to each other in a single pass.
  A `FenixFlowingFluid` mirrors vanilla's `WaterFluid`, configured through a
  builder instead of subclassed. On the client, `FluidRendering` names the still
  and flowing sprites: 26.2 stopped hardcoding water and lava in the fluid
  renderer and bakes their models into a two-entry map, and a mixin adds a mod's
  fluids to it — so a modded fluid draws instead of showing the missing-texture
  checkerboard. A conformance check proves the four registrations name each
  other, against real Minecraft.
- **Attachments — typed data on entities and block entities.** `Registrar.attachment`
  declares a piece of state a mod hangs on an `Entity` or a `BlockEntity`
  without editing either class, the way Forge's capabilities did. A mixin makes
  both an `AttachmentHolder`; a persistent attachment carries a codec and is
  written beside the thing it belongs to, at the very methods that thing already
  saves itself with, so it survives a world reload with no storage of its own. A
  transient attachment carries no codec and is never written. `get` returns a
  default without storing it, so reading one off every mob in a level does not
  quietly attach a default to all of them and grow the save. A conformance check
  round-trips a block entity through its own save path — and verifies the mixin
  reached `Entity` — against real Minecraft.
- **A project generator on the website.** Fill in a name and a package, tick
  what you want, and download a zip that builds: sources, `fenix.mod.json`, the
  build files and the Gradle wrapper. Everything happens in the browser —
  nothing is uploaded, and there is no service to keep running.

### Fixed

- **Every new world asked the player to confirm experimental features.** A mod
  jar was handed to the game as a pack with no `KnownPack`, and vanilla decides
  the lifecycle of a datapack registry entry by exactly that: no known pack means
  `Lifecycle.experimental`, one experimental registry makes
  `allRegistriesLifecycle()` experimental, and `CreateWorldScreen` warns on
  anything that is not stable. One worldgen file in one mod was enough, and the
  warning named no mod, no file and no pack — so it read as something Fenix was
  doing to the game rather than a missing field. Mod packs now declare a
  `KnownPack` of the mod's own namespace, id and version, which is what the field
  is for: it tells a joining client which packs it already has, and Fenix's
  registry sync has already insisted the client has the same mods. A conformance
  check drives the real pack source inside a launch and fails without it.

- **The Gradle plugin named versions that had never been published.** It wrote
  one version into three coordinates — `fenix-loader`, `fenix-processor` and
  `ember` all took the repository's umbrella version — and each module is
  versioned on its own axis, so each asked for an artifact that does not exist.
  Nothing here could see it: a composite build substitutes the projects and
  never reads the number, so every mod in this repository resolved fine while
  every mod outside it failed at `compileJava`. The plugin now bakes in the four
  real versions, each expanded from `gradle.properties` at build time. Found by
  generating a project from the website and building it, which is the first
  thing that ever consumed the published plugin the way a mod author does.

Ore generation, which needed four fixes and none of them showed up outside the
game. The feature is applied to 55 biomes now; before, it reached none.

- **The create-world screen never received mod datapacks.** The mixin that adds
  Fenix as a pack source decided which kind of repository it was looking at by
  finding a folder source, and `CreateWorldScreen` builds
  `new PackRepository(new ServerPacksSource(…))` — no folder at all. So mod data
  was absent from the load that decides a new world's worldgen registries, and
  an ore added to a biome had no feature to point at. A `ServerPacksSource` now
  means datapacks; it is nameable on both sides, which is what pushed the
  original rule towards folders.

- **Applying a modification cast to the mixin class.** A mixin is a template:
  Mixin merges its members into the target and then refuses to load the
  template, so the cast compiled and could never run. A plain interface carries
  the method now — and lives outside the mixin package, because Mixin owns every
  class in a package a config declares, not merely the ones it lists.

- **The injection was on the method both callers share**, so it also ran for a
  client receiving registries from a server it had joined. That world was
  decided by the server, and a client generates no terrain of its own.

- **A missing feature refused the launch.** The game loads its worldgen
  registries in passes, and a pass before the mod's datapack is found is normal
  rather than broken; throwing there took the world-creation screen down with
  it. Missing is skipped now, and the load that carries the feature logs once —
  the id, the biome count and the step. That line is the answer to "is my ore
  live", and its absence is the tell when nothing generates.

### Changed

- Two conformance checks that would have caught the first two, each verified to
  fail without its fix: a datapack repository built with no folder still
  receives Fenix's source, and `BiomeGenerationSettings` implements the
  interface the modification is applied through. Asserting the injection landed
  was never enough — it always had.

## [0.1.3] — 2026-07-22

Six things the loader was missing, in the order a person meets them.

### Added

- **A failure reaches the person who caused it.** Fenix already said what went
  wrong and said it well; it printed that to standard output and exited, which
  for anyone starting the game from the Minecraft launcher means the window
  vanishes and nothing else happens. The report is written to
  `fenix-launch-error.txt` beside the game — a file survives, and it is what
  gets pasted into a bug report — and then shown in a window when there is a
  screen. A dedicated server keeps the console it always had.

- **`breaks`**, for a combination a mod refuses to run in. Without it the
  incompatibility surfaced as a crash inside one of the two mods, naming
  neither and blaming whichever happened to be on the stack.

- **`after`**, which orders without requiring. A compatibility patch has to run
  after the mod it patches and still load when that mod is absent; saying that
  with `depends` was the only way before, and it turned every optional
  integration into a hard requirement.

- **`/fenix` and `/fenix mods`**, listing what is loaded, in load order — which
  is the order that matters when two mods disagree. Nothing in the game knew
  what Fenix had loaded, so the first question asked of any broken world was
  the one nobody could answer without reading a folder. Open to everyone: the
  server already sends every mod's namespace to every client that joins.

- **A singleton entry class is used rather than constructed.** Kotlin compiles
  `object ExampleMod : FenixMod` to a class with a private constructor and a
  public static final `INSTANCE`, so requiring a public no-argument constructor
  ruled out the idiomatic way to write a Kotlin mod — and said so with an error
  about a constructor nobody had written.

- **The API reference is on the website**, generated by a doclet from the
  javadoc in the source — searchable, themed like the rest, and versioned by
  the same mechanism as every other page. `./gradlew apiDocsSite` writes it.

  A doclet rather than a parser because javadoc has already resolved every type
  and every comment by the time one runs: nothing has to guess what a name
  refers to. And generated rather than written, because a hand-kept reference
  is a second copy of the API that starts drifting the day it is finished.

  One page per package, not per class. A hundred and fifty entries in a sidebar
  is not a reference anyone browses; twenty-two, each holding a package's types
  under their own headings, is — and a type still has a link of its own.

  Members named `fenix$` are left out. They are public so that a mixin in
  another package can reach them, not because a mod should call them, and
  listing them invites exactly the call their own javadoc asks the reader not
  to make.

### Changed

- **Discovery runs in parallel.** Every jar is opened, parsed and unpacked
  independently of every other, which at a hundred mods was a hundred file
  reads waiting on each other for no reason. Results are collected per jar and
  flattened afterwards, so the order stays the sorted one — which is the order
  a log has to read in for anyone to follow it.

- **Two mods carrying the same library is no longer a refusal.** It is
  ordinary, and neither author chose it or can fix it: the newer copy wins.
  Refusing meant any two mods sharing a dependency could not be installed
  together. Two loose jars in the mods folder are still the player's to sort
  out — somebody put both files there, and only they can say which they meant.

## [0.1.2] — 2026-07-22

### Added

- **World generation.** `EmberOreProvider` writes the two files an ore needs —
  a configured feature saying what to place, a placed feature saying where —
  and `BiomeModifications.addFeature` says which biomes want it, with
  `BiomeSelectors` for the usual answers.

  Code rather than data for the last part, because the alternative does not
  compose: a datapack that redefines `minecraft:plains` to add an ore replaces
  the whole biome, so two mods doing it erase each other and the player sees
  whichever loaded last. Fenix adds to the biome the game actually loaded.

  Applied when datapacks finish loading, and at no other moment. Earlier, biome
  tags are not bound, so a selector asking whether a biome is in the overworld
  gets the wrong answer; later, a chunk may already have generated from the
  unmodified biome, and a world with the ore in some chunks and not others is
  worse than one without it.

- **`ParticleRendering`**, the client half `Registrar.particle` was missing —
  and which its own documentation had promised. A particle type with no
  provider is spawned and never drawn: the client looks one up, finds nothing
  and returns, with nothing logged and nothing crashed. Vanilla's provider
  table is filled once, in a method naming its own particles one by one, so
  Fenix appends at the end of it — which is also where the sprite sets are
  still being collected, so a particle registered later would have no textures.

  `SpriteParticleFactory` is Fenix's own for the third time now: vanilla's
  equivalent is a private nested interface, so a mod passing a method reference
  could not compile against it.

- Ember knows a status effect's translation key. It handled blocks and items
  and threw on anything else, which is the right failure and was reached the
  first time an effect was declared.

- `Registrar.placedFeature` names one of the mod's own features, and
  `Registrar.identifier` is now public — for naming things the registrar does
  not register, like a key binding or a tag.

- The example mod now demonstrates every registry added since 0.1.0. The ruby
  hammer does three at once — it counts its swings in a data component, sparks
  where it lands, and glimmers every fifth swing — and ruby ore generates
  underground in overworld biomes.

- Two more conformance checks, each verified to fail when sabotaged. The
  worldgen files Ember writes are parsed with Minecraft's own codecs, which is
  the only honest check on generated data: a misspelled field fails no build,
  fails no startup and logs nothing — the entry is dropped and the ore is never
  anywhere. Misspelling `discard_chance_on_air_exposure` now fails the build.
  The second covers the two biome injections still landing.

### Fixed

- The list of jars a bundle carries was read when the sync task was
  *configured*, from whatever bundle had been built at the time. So the first
  build after a version bump excluded the previous version's names, let this
  version's modules through, and put every module in the directory twice —
  once loose, once unpacked from the bundle — which the loader refused to start
  over. It is read when the task runs now.

- `runClient` and `ember` left last build's jars in their mods directory, so
  the first version bump put two of everything there and the loader refused to
  start over duplicate ids — correctly, and about something the author did
  nothing to cause. Both directories now mirror the build rather than
  accumulating.

## [0.1.1] — 2026-07-22

### Added

- **Key bindings.** `KeyBindings.register` binds a key and returns the mapping
  to ask; `KeyBindings.category` makes a group of the mod's own. Vanilla builds
  its list of mappings once, in a field initialiser naming its own one by one,
  and never reads it again — so a mapping missing from that list never reaches
  the controls screen and is never written to `options.txt`. The key works
  until the player restarts, then silently returns to its default, with nothing
  logged. Fenix appends to the list at the end of `Options`' constructor, which
  is after the initialiser and before anything reads it.

- **Spawn eggs and spawn rules.** `Registrar.spawnEgg` and
  `Registrar.spawnRule`. The egg holds its entity type rather than a promise of
  one, so it is registered in the pass that runs once every entity exists —
  which is what lets the two be declared in whichever order reads best. Without
  the rule an entity can be summoned and hatched from its egg and still never
  appear in the world, which reads as a wrong spawn weight rather than as a
  missing registration.

- **Particles, status effects and data components.** One line each:
  `particle`, `effect`, `dataComponent`. `SimpleParticleType`'s constructor is
  protected, so the registry module widens it rather than making every mod
  write a subclass to reach `super`.

- **`./gradlew apiDocs`** — one browsable Javadoc site over every module, both
  halves of each, into `build/docs/api`. The `-javadoc` jars each module
  already published are what an IDE reads and what nobody browses. The client
  halves are included deliberately: `KeyBindings`, `MenuScreens` and
  `EntityRendering` live there, and a per-module split is what hides them.

- Two more conformance checks, each verified to fail when what it covers is
  sabotaged: the key binding injection landing on `Options` — including that
  `keyMappings` is no longer final, without which the injection lands and its
  assignment does nothing — and spawn eggs, spawn rules, particles, effects and
  data components reaching their registries.

- `fenix-api` declares the modules it carries, so one line in `depends` is now
  enough for the whole API. Carrying a module and depending on it are not the
  same thing, and the gap was invisible until looked at: a mod naming only
  `fenix-api` was placed *before* `fenix-api-registry` in the load order,
  because nothing tied the two together and unconstrained mods fall back to
  alphabetical. Nothing breaks today — no API module has an entrypoint to run —
  but the first one that does would have broken a mod that did nothing wrong.

  The list is generated from the same one the jar-in-jar packaging uses, so a
  module cannot be carried without also being declared.

- A working menu in the example mod — the ruby safe: a block entity holding
  twenty-seven slots, opened server-side, drawn client-side.
- A menu in the registry conformance check. `MenuType`'s constructor and the
  interface it takes are both private, so registering one at all only works if
  the loader really widened them in the jar the game loads. That transformation
  had no end-to-end test; disabling it now fails the check with the
  `IllegalAccessError` a player would otherwise meet on opening a chest.

### Changed

- The roadmap said phases 7 onward were not started while marking both done
  further down. It also listed two gaps that are not gaps in 26.2, now written
  down as such so they are not "fixed" later:
  **render layers**, which the game derives from a texture's own alpha channel
  — glass and plants render correctly with no registration, and the
  `ItemBlockRenderTypes` table earlier versions needed is gone — and
  **enchantments**, which have been datapack data since 1.21.


### Fixed

- `MenuScreens.register` could not be called by a mod. It took vanilla's
  `ScreenConstructor`, which is private, so `javac` refused the method reference
  at the call site — the same trap `MenuFactory` was written to avoid on the
  other half of the API, missed here. It now takes Fenix's own `ScreenFactory`
  and adapts internally, where naming vanilla's type is this module's privilege
  rather than a mod's problem.

  Found by writing the demo. Nothing had ever called the method from outside the
  module that declares it, which is the one place the mistake was invisible.

- The example mod declared three API modules while using six, and had for
  weeks. Nothing complained, because `depends` asserts presence and all six
  were present anyway — which is the argument for naming the bundle instead of
  keeping a list by hand. It now depends on `fenix-api` alone.

- Blocks in the example mod declaring `requiresTool()` without a
  `mineable/pickaxe` tag. No tool is the correct one for such a block, so it
  broke without ever dropping.

## [0.1.0] — 2026-07-22

The first release: a loader that runs real Minecraft, an API a content mod can
be written against, and an installer a player can double-click.

### Added

- **Access widening**, and the menus it unblocked. Mixin already reaches a
  private field or method with `@Accessor` and `@Invoker`; what it cannot do is
  make a type *nameable*. `MenuType`'s constructor is private and its parameter
  is a private interface, so there was nothing a mod could write down — in any
  package. Verified rather than assumed: a class placed in
  `net.minecraft.world.inventory` still failed to compile.

  A mod now declares `accessible` entries in its manifest. The loader raises
  those members to public before anything loads them, and the Gradle plugin
  applies the same declarations to the copy of Minecraft the mod compiles
  against — both reading the one file the mod already ships, so what `javac`
  allows and what the game allows cannot drift apart. Making a nested type
  nameable takes two edits, not one: the type's own flags and the
  `InnerClasses` entry, which is what `javac` actually reads.

  On top of it: `Registrar.menu`, `MenuScreens.register` on the client, and
  `SimpleMenu` — which lays the slots out and implements `quickMoveStack`, the
  single most copied-and-broken method in Minecraft modding. The version that
  circulates moves stacks into the wrong half, loops forever, or deletes items
  when the destination is full.

  One thing fell out of it: `fenix.mod.json` is no longer run through Groovy's
  template engine, which read the `$` in `MenuType$MenuSupplier` as a variable
  and failed the build. Escaping it would have left the source file invalid
  JSON, which everything else reads.

- **Config** (`fenix-api-config`), the last of phase 8. A record is the schema,
  the defaults and the documentation at once: its component names are the file's
  keys, its types decide what a value may be, and the instance you pass is what a
  missing setting falls back to. There is no separate spec to keep in step.

  Gson can read records itself and does the one thing that matters wrongly: a
  field missing from the file becomes `null` or zero, in silence. Somebody who
  deletes a line, or who updates to a version that added one, gets a mod behaving
  as though they had asked for nothing. So the record is built component by
  component, taking the default for anything absent — which is also what makes it
  possible to name an unknown key rather than drop it. A mistyped setting that
  quietly does nothing is the configuration bug that costs an evening.

  The file is rewritten complete after every load, so a setting an update added
  appears with its value instead of staying invisible until somebody reads a
  changelog. Validation belongs in the compact constructor, the one place a value
  cannot get in without passing through, and the author's message reaches the
  player prefixed with the file and field rather than as a stack trace.

  Unlike the rest of the API this is pure logic, so it is the first module
  covered by ordinary unit tests rather than by a conformance check that boots
  the game.

- **Player, entity and level events.** `PlayerEvents` (joined, left, died,
  respawned), `EntityEvents` (spawning — cancellable — and died) and
  `LevelEvents` (loaded, saving). The module had ticks and blocks, which made it
  a demonstration rather than an API.

  Where each fires is the whole design. `LEFT` fires while the player is still
  readable, because a moment later there is no inventory and no position to look
  at. `RESPAWNED` carries the *new* player, since respawning replaces the object
  rather than resetting it — anything a mod attached to the old one is gone.
  `SPAWNING` cancels before the entity joins the level, so a refused spawn never
  existed rather than being removed a tick after everyone saw it. `LOADED` fires
  once per level rather than once per server, which is what a mod keeping
  per-world state actually wants.

  Player death and entity death fire from one injection rather than two, because
  a player is a living entity and two sites would eventually disagree about
  that.

- **Commands** (`fenix-api-command`). `CommandEvents.REGISTER` hands a listener
  the dispatcher, and `Commands` covers what Brigadier makes tedious: `run(…)`
  takes a body returning nothing, since `executes` wants an int nobody reads and
  forgetting it is a compile error whose message says nothing about commands.
  `operator()` is the permission `/gamemode` asks for — Minecraft 26.2 replaced
  numeric levels with named permissions, so the `hasPermission(2)` in every
  older mod is both obsolete and meaningless on sight. Every builder returned is
  Brigadier's own: a shortcut over that API, never a wall in front of it.

- **The installer is an application.** `Fenix Installer.exe`, built with
  jpackage, with a window that asks two questions it has already answered and a
  button. Installing a mod loader is something people do once, often before they
  know what a loader is, and every question asked is a chance to answer it
  wrongly. It carries its own Java runtime: Minecraft ships one but does not put
  it on the `PATH`, so an installer needing one would turn away exactly the
  people it exists for. Trimmed to the modules it uses, that is 49 MB unpacked
  and 33 MB to download. Run with arguments, or on a machine with no screen, it
  is still the command-line tool it was — a headless server is where scripting
  an install actually matters.

  `--type app-image` rather than msi: those need the WiX toolset installed by
  hand, and a build that only works on a prepared machine breaks the first time
  somebody else runs it. A signed system installer is a release concern and
  belongs in CI.

- **The API ships as one jar.** `fenix-api-<version>+mc<game>.jar` carries its
  modules under `META-INF/jars/`, and the loader unpacks anything it finds there
  and treats each as the mod it is. A player installing the API drops in one
  file rather than four — and rather than five once the next module lands, or
  keeping their versions in step by hand. The modules stay independently
  versioned and independently publishable; only what you install changes.

  Nothing lists the nested jars: a manifest that can disagree with the archive
  eventually does, and the directory is already the truth. They are unpacked
  beside the mods rather than among them, into `.fenix/jars`, so the directory
  can be deleted to force a clean unpack and its contents cannot be mistaken for
  something a player installed. An unpacked jar is reused when its size matches,
  which catches a version change without reading every jar in full on every
  launch.
- **A versioning scheme**, in [docs/versioning.md](docs/versioning.md). The
  loader, the API set and each API module now carry their own version, because
  they mean different things and move at different speeds — a fix in the
  registry should not make every mod that only uses events look out of date.
  Anything built against Minecraft carries the game version as build metadata
  (`0.1.0+mc26.2`): those artifacts only work with the game they were built for,
  and a coordinate that does not say so invites finding out at run time. A
  module's version is derived from its project name, so adding one means adding
  a line to `gradle.properties` and nothing else.

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


- The installer offers the game versions it finds rather than asking for one to
  be typed. That text box was its last remaining way to fail: a name that did
  not match a folder produced an error the player then had to go and check. The
  list is what the launcher has actually run — the same condition the install
  needs — so anything offered will work, and versions sort newest first counting
  numbers as numbers, since "26.10" above "26.2" is noticed at once and trusted
  less for. A second list carries the Fenix versions available for the game
  version chosen: one today, and honestly so, because the loader and its
  libraries travel inside the installer and the only version it can lay down is
  the one it carries.
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
