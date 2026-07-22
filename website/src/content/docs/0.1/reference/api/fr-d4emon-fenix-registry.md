---
title: "registry"
description: "Types in fr.d4emon.fenix.registry"
sidebar:
  order: 20
tableOfContents:
  maxHeadingLevel: 2
---

<p class="api-package"><code>fr.d4emon.fenix.registry</code></p>

| Type | What it is |
|---|---|
| [`BlockBuilder`](#blockbuilder) | Describes a block, then registers it. |
| [`CreativePages`](#creativepages) | Pages of creative tabs. |
| [`CreativeTabs`](#creativetabs) | Puts a mod's content in the creative menu. |
| [`EntityAttributes`](#entityattributes) | The default attributes mods have declared. |
| [`Holder`](#holder) | A handle on something a mod registers, usable before it exists. |
| [`ItemBuilder`](#itembuilder) | Describes an item, then registers it. |
| [`MenuFactory`](#menufactory) | Builds a menu on the client, when the server says a window has opened. |
| [`Registrar`](#registrar) | Where a mod declares its content. |
| [`SimpleMenu`](#simplemenu) | A chest-shaped menu: a grid of slots above the player's inventory. |

## BlockBuilder

Describes a block, then registers it.

```java
public static final Holder<Block> RUBY_BLOCK = REGISTRAR.newBlock("ruby_block")
       .strength(3f)
       .requiresTool()
       .withItem()
       .register();
```

The methods here cover what most blocks need. Anything else is reachable
through `#properties(UnaryOperator)`, which hands you vanilla's own
builder — this is a shortcut over that API, never a wall in front of it.

### `BlockBuilder from(Function<BlockBehaviour.Properties,Block> blockFactory)`

Builds a subclass instead of a plain `Block`.

### `BlockBuilder instabreak()`

Makes the block break instantly, like grass.

### `BlockBuilder lightLevel(int level)`

Emits light.

### `BlockBuilder noOcclusion()`

Lets you see through it, like glass.

### `BlockBuilder properties(UnaryOperator<BlockBehaviour.Properties> step)`

Applies anything else vanilla's builder offers.

### `Holder<Block> register()`

Declares the block. Registration itself happens at `apply()`.

### `BlockBuilder requiresTool()`

Requires the right tool to drop anything, like ore.

### `BlockBuilder sound(SoundType sound)`

Sets the sounds it makes.

### `BlockBuilder strength(float destroyTime, float explosionResistance)`

Sets how long it takes to break, and how well it resists explosions.

### `BlockBuilder strength(float strength)`

Sets breaking time and explosion resistance to the same value.

### `BlockBuilder withItem()`

Also registers the item that places this block — what you want unless the
block is only ever placed by code.

## CreativePages

Pages of creative tabs.

Vanilla has no such thing: the tab strip is a fixed grid of two rows by
seven columns, and vanilla's own tabs fill all fourteen. A mod tab has
literally nowhere to go, so Fenix adds the pages that make room.

Page 0 is vanilla's, untouched — someone who installs a mod should still
find the menu exactly where they left it. Mod tabs start on page 1.

Four of vanilla's tabs come along to every page: search, inventory,
hotbar and op blocks. They are tools rather than categories, and a player who
turns the page to find a mod's blocks should not lose the search box to do
it. They sit in columns 5 and 6 of both rows, which is exactly why mod tabs
get {@value #TABS_PER_PAGE} slots and not fourteen.

**Constants**

| Name | What it is |
|---|---|
| `TABS_PER_PAGE` | Columns 0 to 4 of both rows — what is left once the tools are seated. |

### `static int count()`

{@return how many pages there are, at least one}

### `static int current()`

{@return the page being shown}

### `static List<CreativeModeTab> onCurrentPage(List<CreativeModeTab> tabs)`

{@return the tabs belonging to the page being shown}

### `static int pageOf(CreativeModeTab tab)`

{@return which page a tab sits on}

Vanilla's are all on page 0 — where they are <em>registered</em>, which
is what decides whether two of them collide. The four that also travel to
later pages are not registered twice, so they still answer 0.

### `static void turn(int delta)`

Moves to another page, wrapping at both ends.

## CreativeTabs

Puts a mod's content in the creative menu.

```java
CreativeTabs.addTo(CreativeTabs.BUILDING_BLOCKS, ModBlocks.RUBY_BLOCK);
CreativeTabs.addTo(CreativeTabs.INGREDIENTS, ModItems.RUBY);
```

Without this, content is registered but unreachable in game except through
`/give` — which is the difference between a mod a player can use and
one they cannot.

Call it from `onRegister`, alongside the content itself. The
additions are recorded and applied whenever the game builds a tab's contents,
which it does on every resource reload rather than once.

Vanilla's own tab keys are private, so the ones below are rebuilt from
their ids. Any other tab — including another mod's — works by passing its
key directly.

**Constants**

| Name | What it is |
|---|---|
| `BUILDING_BLOCKS` | Stone, planks, and the rest of what you build with. |
| `COLORED_BLOCKS` | Decorative blocks: glass, carpets, plants. |
| `NATURAL_BLOCKS` | Natural blocks: dirt, ore, wood. |
| `FUNCTIONAL_BLOCKS` | Blocks that do something: chests, hoppers, furnaces. |
| `REDSTONE_BLOCKS` | Redstone components. |
| `TOOLS_AND_UTILITIES` | Tools and utilities. |
| `COMBAT` | Weapons and armour. |
| `FOOD_AND_DRINKS` | Food and drink. |
| `INGREDIENTS` | Crafting materials — where most new items belong. |
| `SPAWN_EGGS` | Spawn eggs. |
| `SEARCH` | The search tab. |
| `INVENTORY` | The survival inventory tab. |
| `HOTBAR` | Saved hotbars. |
| `OP_BLOCKS` | Operator blocks, shown only to operators. |

### `static void addTo(ResourceKey<CreativeModeTab> tab, Holder<?>[] content)`

Adds content to a creative tab.

### `static List<Holder<?>> additionsFor(ResourceKey<CreativeModeTab> tab)`

{@return what has been added to a tab, in declaration order}

Read by the mixin that applies these. Not meant for mods.

### `static String titleKey(ResourceKey<CreativeModeTab> tab)`

{@return the translation key for a tab's title}

The one place this is worked out. `Registrar#creativeTab` names
the tab with it and `EmberLanguageProvider` translates it with it,
so the two cannot drift apart — a tab showing its raw key in game is the
sort of thing that ships unnoticed.

## EntityAttributes

The default attributes mods have declared.

Kept beside vanilla's table rather than merged into it, for two reasons
that both come down to timing. Vanilla's is an `ImmutableMap` built in
a static initialiser that resolves every vanilla attribute — so merely
reading it, during `onRegister`, would build it before the attribute
registry is bound and fail there. And a mod's own attribute values are
written against those same unbound holders, so they cannot be computed then
either.

So nothing is resolved until something asks. The mixin on
`DefaultAttributes` looks here first, by which point the game is past
bootstrap and everything binds cleanly.

### `static AttributeSupplier get(EntityType<?> type)`

{@return a mod's attributes for this type, or {@code null}}

Built on the first ask and kept, so the cost lands once and every
entity of the type shares one supplier, as vanilla's do.

### `static boolean has(EntityType<?> type)`

{@return whether a mod declared attributes for this type}

## Holder

A handle on something a mod registers, usable before it exists.

This is what lets content be declared in `static final` fields and
referred to from anywhere, even though registration itself cannot happen
until the game opens its registries:

```java
public static final Holder<Block> RUBY_BLOCK = REGISTRAR.block("ruby_block");
```

Reading it before registration is a mistake with a clear message rather
than a `null` that surfaces somewhere else entirely.

### `T get()`

{@return the registered object}

### `Identifier id()`

{@return the id this was registered under}

### `boolean isBound()`

{@return whether registration has happened}

### `String toString()`

## ItemBuilder

Describes an item, then registers it.

```java
public static final Holder<Item> RUBY = REGISTRAR.newItem("ruby")
       .stacksTo(16)
       .rarity(Rarity.UNCOMMON)
       .register();
```

Anything not covered here is reachable through
`#properties(UnaryOperator)`, which hands you vanilla's own builder.

### `ItemBuilder durability(int uses)`

Makes it a tool with durability, which cannot stack.

### `ItemBuilder fireResistant()`

Survives fire and lava, like netherite.

### `ItemBuilder from(Function<Item.Properties,Item> itemFactory)`

Builds a subclass instead of a plain `Item`.

### `ItemBuilder properties(UnaryOperator<Item.Properties> step)`

Applies anything else vanilla's builder offers.

### `ItemBuilder rarity(Rarity rarity)`

Sets the colour its name is shown in.

### `Holder<Item> register()`

Declares the item. Registration itself happens at `apply()`.

### `ItemBuilder stacksTo(int size)`

Sets the most that fits in one stack.

## MenuFactory

Builds a menu on the client, when the server says a window has opened.

Fenix's own, and public, so that a mod never has to name vanilla's — which
it could not: `MenuType.MenuSupplier` is private, and the widening that
lets this module reach it stays this module's business.

### `T create(int windowId, Inventory inventory)`

Builds one.

## Registrar

Where a mod declares its content.

Content is declared once, in fields, and registered later — the game only
opens its registries for a moment, and a mod should not have to arrange its
code around that:

```java
public final class Content {
   public static final Registrar REGISTRAR = Registrar.of("mymod");

   public static final Holder<Block> RUBY_BLOCK = REGISTRAR.blockWithItem("ruby_block");
   public static final Holder<Item> RUBY = REGISTRAR.item("ruby");
}
```

```java
@Override
public void onRegister(Fenix fenix) {
   Content.REGISTRAR.apply();
}
```

That one call is also what loads the class holding the fields, so nothing
can be declared and then silently never registered.

<h2>What this saves you from</h2>

Registering content by hand against vanilla is a minefield, because
vanilla does bookkeeping <em>around</em> its own registration that a mod
bypasses. Every step below is here because skipping it crashes — and crashes
far from the cause, in vanilla code, which makes it miserable to diagnose:

<ul>
<li>Content must be told its own id <em>before</em> it is constructed, via
   `Properties.setId`.</li>
<li>Block states get their network ids and caches in a single pass in
   `Blocks`' static initialiser, which has already run by the time a
   mod registers. A block that misses it kicks the player with
   "Can't find id for Block{…}" when a block update is encoded, and throws
   "occlusionShapesByFace is null" while rendering.</li>
<li>Vanilla maps a block to its item in `Item.BY_BLOCK`. Without it
   `Block.asItem()` returns air <em>and caches that</em>, so
   `new ItemStack(block)` is empty and the creative search tab dies
   with "Stack size must be exactly 1".</li>
</ul>

### `void apply()`

Registers everything declared so far. Call this from
`onRegister`; calling it twice does nothing the second time.

### `void attributes(Holder<EntityType<T>> type, Supplier<AttributeSupplier.Builder> attributes)`

Gives a living entity its default attributes — health, speed and the
rest.

```java
REGISTRAR.attributes(ModEntities.SPRITE, () -> Mob.createMobAttributes()
       .add(Attributes.MAX_HEALTH, 8)
       .add(Attributes.MOVEMENT_SPEED, 0.25));
```

Not optional for anything living. A `LivingEntity` asks vanilla
for its attributes while it is being constructed, and an entity that is
not in that table dies there with a null map — inside vanilla, nowhere
near the mod that registered it.

### `Holder<Block> block(String name)`

Declares a plain block, with no item form.

### `Holder<Block> block(String name, Function<BlockBehaviour.Properties,Block> factory)`

Declares a block, with no item form.

### `Holder<BlockEntityType<T>> blockEntity(String name, BlockEntityType.BlockEntitySupplier<T> factory, Holder<Block>[] blocks)`

Declares the type behind a block that stores something.

```java
public static final Holder<BlockEntityType<SafeBlockEntity>> SAFE =
       REGISTRAR.blockEntity("safe", SafeBlockEntity::new, ModBlocks.SAFE);
```

The blocks may be declared before or after this call. Block entity
types are registered in a pass of their own, after everything else, so
the order a mod happens to write its fields in cannot matter.

### `Holder<Block> blockWithItem(String name)`

Declares a block together with the item that places it — the usual case.

### `Holder<Block> blockWithItem(String name, Function<BlockBehaviour.Properties,Block> factory)`

Declares a block together with the item that places it.

### `ResourceKey<CreativeModeTab> creativeTab(String name, Holder<?> icon)`

Declares a creative tab of the mod's own.

```java
public static final ResourceKey<CreativeModeTab> TAB =
       REGISTRAR.creativeTab("example_mod", ModItems.RUBY);
```

Fill it the same way as any other tab:
`CreativeTabs.addTo(TAB, ModItems.RUBY)`.

Vanilla's fourteen slots are all taken, so the tab lands on a page of
its own — see `CreativePages`. Its position within that page is
assigned in declaration order, which is why nothing here asks for a row
or a column.

Its title is `itemGroup.<mod id>.<name>`, which
`EmberLanguageProvider` can translate like anything else.

### `Holder<DataComponentType<T>> dataComponent(String name, UnaryOperator<DataComponentType.Builder<T>> build)`

Declares a data component — a typed piece of state a stack carries.

```java
public static final Holder<DataComponentType<Integer>> CHARGE =
       REGISTRAR.dataComponent("charge", builder -> builder
               .persistent(Codec.INT)
               .networkSynchronized(ByteBufCodecs.VAR_INT));
```

Say `persistent` for state that has to survive saving, and
`networkSynchronized` for state the client needs in order to draw.
A component with neither lasts until the stack is next looked at.

### `Holder<T> effect(String name, T effect)`

Declares a status effect.

```java
public static final Holder<MobEffect> GLIMMER =
       REGISTRAR.effect("glimmer", new GlimmerEffect());
```

The effect is a class of the mod's own extending `MobEffect`;
what it does lives in `applyEffectTick`.

### `Holder<EntityType<T>> entity(String name, EntityType.EntityFactory<T> factory, MobCategory category, UnaryOperator<EntityType.Builder<T>> step)`

Declares an entity type.

```java
public static final Holder<EntityType<RubyBolt>> BOLT = REGISTRAR.entity(
       "ruby_bolt", RubyBolt::new, MobCategory.MISC,
       builder -> builder.sized(0.25f, 0.25f));
```

Anything that lives needs attributes too — see `#attributes` —
and anything visible needs a renderer, which is the client's business.

### `Identifier identifier(String name)`

{@return an id in this mod's namespace}

For naming things the registrar does not register — a key binding, a
tag, a file in the mod's own data.

### `Holder<Item> item(String name)`

Declares a plain item.

### `Holder<Item> item(String name, Function<Item.Properties,Item> factory)`

Declares an item.

### `Holder<MenuType<T>> menu(String name, MenuFactory<T> factory)`

Declares a menu type — the thing a block opens.

```java
public static final Holder<MenuType<SafeMenu>> SAFE =
       REGISTRAR.menu("safe", SafeMenu::new);
```

The factory runs on the <em>client</em>, when the server says a window
has opened, and is given only the window id and the player's inventory —
because that is all the client is told. A menu showing a block's contents
therefore builds an empty container here and lets the sync fill it, which
is exactly what vanilla's chests do.

Opening one is the server's job:

```java
player.openMenu(new SimpleMenuProvider(
       (id, inventory, who) -> new SafeMenu(id, inventory, contents),
       Component.translatable("container.mymod.safe")));
```

### `String modId()`

{@return the mod id everything here is namespaced under}

### `BlockBuilder newBlock(String name)`

Starts describing a block.

### `ItemBuilder newItem(String name)`

Starts describing an item.

### `static Registrar of(String modId)`

Creates a registrar for one mod.

### `Holder<SimpleParticleType> particle(String name)`

Declares a particle carrying no data of its own.

This is the half both sides need. The client also has to say what the
particle looks like — `ParticleRendering.register` — and the
textures come from `assets/<namespace>/particles/<name>.json`. A
type with no provider is spawned and never drawn, silently.

### `Holder<SimpleParticleType> particle(String name, boolean alwaysVisible)`

Declares a particle carrying no data of its own.

### `ResourceKey<PlacedFeature> placedFeature(String name)`

{@return the key of one of this mod's placed features}

Names rather than registers: features are datapack data, written by
`EmberOreProvider` and loaded by the game. What this is for is
pointing at one, usually to hand to
`BiomeModifications.addFeature`.

### `Holder<SoundEvent> sound(String name)`

Declares a sound event.

The event is only half of a sound: the other half is an entry in
`sounds.json` naming the ogg files to play, which
`EmberSoundProvider` generates.

### `Holder<Item> spawnEgg(String name, Holder<EntityType<T>> type)`

Declares a spawn egg for an entity.

```java
public static final Holder<Item> WISP_EGG = REGISTRAR.spawnEgg("wisp_spawn_egg", WISP);
```

A spawn egg is an ordinary flat item in 26.2 — one texture, no tint
template — so `EmberModelProvider.flatItem` writes its model and
the two colours are the texture's own.

Registered after every entity type, so the egg and the entity can be
declared in whichever order reads best.

### `void spawnRule(Holder<EntityType<T>> type, SpawnPlacementType placementType, Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate)`

Says where and when an entity may spawn on its own.

```java
REGISTRAR.spawnRule(WISP, SpawnPlacementTypes.ON_GROUND,
       Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules);
```

This is one half of natural spawning; the other is a biome giving the
mob a weight, which is data rather than code. Without <em>this</em> half
the entity never spawns anywhere at all — which reads as a wrong spawn
weight rather than as a missing registration.

Registered after every entity type, so declaration order does not
matter.

## SimpleMenu

A chest-shaped menu: a grid of slots above the player's inventory.

```java
public final class SafeMenu extends SimpleMenu {
   public SafeMenu(int id, Inventory player, Container safe) {
       super(ModContent.SAFE_MENU.get(), id, player, safe, 9, 3);
   }
}
```

Extending `AbstractContainerMenu` directly means writing
`quickMoveStack` — shift-clicking — and that method is the single most
copied-and-broken piece of code in Minecraft modding. The usual version moves
a stack into the wrong half, or loops forever, or silently deletes items when
the destination is full. It is hard to get right because it needs to know
which slots belong to whom, and a mod that lays out its own slots is the only
thing that knows.

So this class lays them out, and gets the method right once.

### `ItemStack quickMoveStack(Player player, int index)`

Moves a stack between the menu and the player's inventory.

Written once, correctly. The contract is unusual and is what trips
every hand-written version: return the stack <em>as it was</em>, and an
empty stack once nothing is left, or the game loops asking again.

### `boolean stillValid(Player player)`

