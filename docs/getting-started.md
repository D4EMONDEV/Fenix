# Getting started

> This workflow works today. What is still missing is listed in
> [roadmap.md](roadmap.md) — networking, config and commands, mainly.

## Building this repository

Requires JDK 25.

```bash
./gradlew build          # compile and test everything
./gradlew installFenix   # publish every artifact to ~/.m2
```

`installFenix` is what you run after changing the loader, the API or the Gradle
plugin, so that a mod project outside this repository picks the change up.

## Writing a mod

`settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        maven("https://d4emondev.github.io/Fenix/")   // the Fenix plugin
        gradlePluginPortal()
    }
}
```

`build.gradle.kts` — the plugin adds the Fenix repository itself, so this is the
whole file:

```kotlin
plugins {
    id("fr.d4emon.fenix.dev") version "0.1.0"
}

fenix {
    minecraft = "26.2"
}
```

That is the whole build file. The plugin puts Minecraft and the whole Fenix API
both on the compile classpath *and* into `run/mods`, so what you write against
is what is there when the game starts.

The API arrives as **one jar** — `fenix-api-<version>+mc<game>.jar` — carrying
its modules inside it. The loader unpacks them and treats each as the mod it is,
so the modules stay independently versioned while the thing you install stays a
single file. See [versioning.md](versioning.md).

To ship against fewer modules, say so and name them:

```kotlin
fenix { api = false }

dependencies {
    fenixMod("fr.d4emon.fenix:fenix-api-event:0.1.0")
}
```

The mod class:

```java
@Mod("example-mod")
public final class ExampleMod implements FenixMod {

    @Override
    public void onInit(Fenix fenix) {
        fenix.logger().info("Hello from {}", fenix.mod().name());
    }
}
```

Plus a `fenix.mod.json` at the root of your resources — see
[mod-metadata.md](mod-metadata.md).

There is no entry in the metadata pointing at `ExampleMod`. The annotation
processor finds the class while it compiles and writes the index into your jar,
so a typo is a compile error rather than a mod that silently never loads.

## Adding content

Declare it in fields and register it once, from `onRegister`:

```java
public final class ModBlocks {
    public static final Holder<Block> RUBY_BLOCK = ModContent.REGISTRAR
            .newBlock("ruby_block")
            .strength(3f)
            .requiresTool()
            .withItem()          // also registers the item that places it
            .register();
}
```

```java
@Override
public void onRegister(Fenix fenix) {
    ModContent.REGISTRAR.apply();
}
```

## The creative menu

Registered content is not yet reachable in game except with `/give`. Put it in a
tab, in the same `onRegister`, after `apply()`:

```java
CreativeTabs.addTo(CreativeTabs.BUILDING_BLOCKS, ModBlocks.RUBY_BLOCK);
CreativeTabs.addTo(CreativeTabs.INGREDIENTS, ModItems.RUBY);
```

Or give the mod a tab of its own, and put content in both:

```java
public static final ResourceKey<CreativeModeTab> TAB =
        REGISTRAR.creativeTab("example_mod", ModItems.RUBY);
```

Vanilla's tab strip is two rows of seven and vanilla fills all fourteen, so a
mod tab lands on a second page. Arrows appear at the top right of the creative
panel, and Page Up and Page Down do the same thing; both are hidden while there
is only one page, which is the case until a mod adds a tab.

Search, the inventory, saved hotbars and operator blocks come along to every
page — they are tools rather than categories, and losing the search box to reach
a mod's blocks is what makes paging feel bad elsewhere. That leaves ten slots a
page for mod tabs.

Translate the title from the tab itself, so renaming it cannot leave the
translation behind:

```java
add(ModContent.TAB, "Example Mod");
```

A `Holder` stands in until registration happens, so content can live in
`static final` fields. That `apply()` call is also what loads the class holding
them — content declared in a class nobody loads is content that never appears.

## Blocks that remember something

A block that stores state needs a block entity. The block implements
`EntityBlock`, and the type is declared alongside it:

```java
public static final Holder<BlockEntityType<TallyBlockEntity>> TALLY =
        REGISTRAR.blockEntity("tally", TallyBlockEntity::new, ModBlocks.TALLY);
```

Declare it before or after the block, whichever reads better — block entity
types are registered in a pass of their own, once every block exists.

A block that does not implement `EntityBlock` is refused here rather than
silently never creating its block entity.

## Entities

```java
public static final Holder<EntityType<Wisp>> WISP = REGISTRAR.entity(
        "wisp", Wisp::new, MobCategory.MISC, builder -> builder.sized(0.25f, 0.25f));
```

Anything that **lives** also needs default attributes, and this is not optional:
a `LivingEntity` asks vanilla for them while it is being constructed, so one
that is missing dies right there, inside vanilla code.

```java
REGISTRAR.attributes(ModContent.SPRITE, () -> Mob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 8)
        .add(Attributes.MOVEMENT_SPEED, 0.25));
```

Pass a lambda rather than a built value: the attribute holders are still
unbound while your mod registers, so Fenix resolves them the first time the
game asks.

Anything **visible** needs a renderer, which is client-only — see below.

## Sounds

```java
public static final Holder<SoundEvent> CHIME = REGISTRAR.sound("chime");
```

That is only half a sound. The other half is `sounds.json`, which
`EmberSoundProvider` writes:

```java
@Generator
public final class ModSounds extends EmberSoundProvider {
    @Override
    protected void sounds() {
        add(ModContent.CHIME, "chime");
    }
}
```

The `.ogg` files themselves go in `assets/<mod id>/sounds/`, alongside your
textures — those two are what Ember cannot generate for you.

## Talking between client and server

A channel carries one kind of payload one way, and which way is in its type —
a `ToServer` cannot be sent to a client, and that is a compile error rather
than a packet nobody handles.

```java title="common"
public static final ToServer<OpenSafe> OPEN_SAFE =
        ToServer.of(Identifier.parse("mymod:open_safe"), OpenSafe.CODEC);
public static final ToClient<SafeContents> CONTENTS =
        ToClient.of(Identifier.parse("mymod:contents"), SafeContents.CODEC);
```

```java
// server, once
OPEN_SAFE.receive((open, player) -> CONTENTS.send(player, contentsAt(open.pos())));

// client, once
CONTENTS.receive(contents -> show(contents));

// either, whenever
OPEN_SAFE.send(new OpenSafe(pos));
CONTENTS.sendAll(server, everything());
```

Handlers run on the game thread of their side, so anything they reach is safe
to touch. **Treat what arrives on the server as untrusted**: a client can send
anything at any time, so check that the player is actually near the block they
claim to be opening.

A client with no handler for a channel drops it, which is what lets a server
run a mod its players do not have.

### When the two sides disagree

Fenix compares registries on join by itself: the server states what it has, the
client compares, and a client missing one of the server's mods is disconnected
with a sentence naming it.

Without that, such a client is admitted and then falls apart in ways that name
nothing useful — one absent block shifts every network id after it, so the
player sees the wrong blocks or is kicked by vanilla naming a block it cannot
find. There is nothing to switch on, and nothing to write.

## The two sides

A mod has two source directories:

```
src/main/java/…          common — the mod itself
src/main/generated/…     what Ember writes
src/client/java/…        client only — mostly how it looks
```

`src/client` may use `src/main`. **The reverse is a compile error**, on purpose:
common code compiles against Minecraft with the client half removed, so naming
`net.minecraft.client.Minecraft` from `src/main` fails with a line number rather
than at run time on somebody else's dedicated server.

Both halves get a `@Mod` class, the same annotation and the same interface —
what makes one client-only is where it lives:

```java title="src/client/java/com/example/client/ExampleModClient.java"
@Mod("example-mod")
public final class ExampleModClient implements FenixMod {
    @Override
    public void onRegister(Fenix fenix) {
        EntityRendering.register(ModContent.WISP, ThrownItemRenderer::new);
    }
}
```

They ship in one jar but are indexed separately, so a dedicated server is never
even told the client class exists. The common half always runs first, which is
why the client half can rely on content it registered.

The source set appears on its own the moment `src/client/java` exists. There is
nothing to switch on and no entry point to declare anywhere.

## Commands

```java
CommandEvents.REGISTER.register(registration -> registration.dispatcher().register(
        literal("wisp")
                .requires(operator())
                .then(argument("count", IntegerArgumentType.integer(1, 20))
                        .executes(run(context -> spawn(context, …))))));
```

Fenix's `Commands` replaces vanilla's — use it and you never need the other. It
is a shortcut over Brigadier, never a wall in front of it: every builder it
returns is Brigadier's own.

What it removes is the `return 1` nobody reads, and the `hasPermission(2)` that
means nothing on sight — Minecraft 26.2 replaced numeric levels with named
permissions, so `operator()` is the one `/gamemode` asks for.

The event fires on server start *and* on every datapack reload, so add rather
than accumulate.

## Reacting to the game

```java
BlockEvents.BREAK.register(event -> isProtected(event.pos()) ? Flow.CANCEL : Flow.CONTINUE);
```

`BlockEvents` is the server's, which is where cancelling actually holds.
`ClientBlockEvents` exists only to make a refusal feel immediate — never as the
enforcement point.

## Generating resources

Models, translations, loot tables, recipes and tags are described in Java:

```java
@Generator
public final class ModLanguage extends EmberLanguageProvider {
    @Override
    protected void translations() {
        add(ModBlocks.RUBY_BLOCK, "Ruby Block");
    }
}
```

`./gradlew ember` writes them into `src/main/generated`, which is part of your
resources. Textures and ogg files are what you still supply yourself.

Generators live in `src/main/java`, never in `src/client/java` — they describe
files against registered content and their output lands under `src/main`, so a
client-side one would be an inversion. Ember does not look there, and the
annotation processor says so rather than letting it be discovered as a missing
model in game.

Without a loot table a block breaks into nothing, silently — so generate one
for every block you add.

## Running it

```bash
./gradlew runClient
./gradlew runServer
./gradlew ember        # regenerate assets and data
./gradlew genSources   # decompile Minecraft for navigation
```
