package fr.d4emon.fenix.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import fr.d4emon.fenix.mixin.registry.SpawnPlacementsInvoker;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.function.UnaryOperator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Where a mod declares its content.
 *
 * <p>Content is declared once, in fields, and registered later — the game only
 * opens its registries for a moment, and a mod should not have to arrange its
 * code around that:
 *
 * <pre>{@code
 * public final class Content {
 *     public static final Registrar REGISTRAR = Registrar.of("mymod");
 *
 *     public static final Holder<Block> RUBY_BLOCK = REGISTRAR.blockWithItem("ruby_block");
 *     public static final Holder<Item> RUBY = REGISTRAR.item("ruby");
 * }
 * }</pre>
 *
 * <pre>{@code
 * @Override
 * public void onRegister(Fenix fenix) {
 *     Content.REGISTRAR.apply();
 * }
 * }</pre>
 *
 * <p>That one call is also what loads the class holding the fields, so nothing
 * can be declared and then silently never registered.
 *
 * <h2>What this saves you from</h2>
 *
 * <p>Registering content by hand against vanilla is a minefield, because
 * vanilla does bookkeeping <em>around</em> its own registration that a mod
 * bypasses. Every step below is here because skipping it crashes — and crashes
 * far from the cause, in vanilla code, which makes it miserable to diagnose:
 *
 * <ul>
 * <li>Content must be told its own id <em>before</em> it is constructed, via
 *     {@code Properties.setId}.</li>
 * <li>Block states get their network ids and caches in a single pass in
 *     {@code Blocks}' static initialiser, which has already run by the time a
 *     mod registers. A block that misses it kicks the player with
 *     "Can't find id for Block{…}" when a block update is encoded, and throws
 *     "occlusionShapesByFace is null" while rendering.</li>
 * <li>Vanilla maps a block to its item in {@code Item.BY_BLOCK}. Without it
 *     {@code Block.asItem()} returns air <em>and caches that</em>, so
 *     {@code new ItemStack(block)} is empty and the creative search tab dies
 *     with "Stack size must be exactly 1".</li>
 * </ul>
 */
public final class Registrar {

    private final String modId;
    private final List<Runnable> pending = new ArrayList<>();
    private final List<Runnable> pendingLate = new ArrayList<>();
    private boolean applied;

    private Registrar(String modId) {
        this.modId = Objects.requireNonNull(modId, "modId");
    }

    /**
     * Creates a registrar for one mod.
     *
     * @param modId the mod's id, used as the namespace of everything registered
     * @return the registrar
     */
    public static Registrar of(String modId) {
        return new Registrar(modId);
    }

    /**
     * {@return the mod id everything here is namespaced under}
     */
    public String modId() {
        return modId;
    }

    // ------------------------------------------------------------------
    // Builders — the readable way in
    // ------------------------------------------------------------------

    /**
     * Starts describing a block.
     *
     * @param name the path part of its id
     * @return a builder; call {@code register()} when done
     */
    public BlockBuilder newBlock(String name) {
        return new BlockBuilder(this, Objects.requireNonNull(name, "name"));
    }

    /**
     * Starts describing an item.
     *
     * @param name the path part of its id
     * @return a builder; call {@code register()} when done
     */
    public ItemBuilder newItem(String name) {
        return new ItemBuilder(this, Objects.requireNonNull(name, "name"));
    }

    // ------------------------------------------------------------------
    // Items
    // ------------------------------------------------------------------

    /**
     * Declares a plain item.
     *
     * @param name the path part of its id
     * @return a handle, bound once {@link #apply()} runs
     */
    public Holder<Item> item(String name) {
        return item(name, Item::new);
    }

    /**
     * Declares an item.
     *
     * @param name    the path part of its id
     * @param factory builds it from properties that already carry its id
     * @return a handle, bound once {@link #apply()} runs
     */
    public Holder<Item> item(String name, Function<Item.Properties, Item> factory) {
        Objects.requireNonNull(factory, "factory");
        Identifier id = identifier(name);
        Holder<Item> holder = new Holder<>(id);

        defer(() -> {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
            // The id has to be on the properties before construction: an item
            // built without it fails later, when something asks for its id.
            Item item = factory.apply(new Item.Properties().setId(key));
            holder.bind(Registry.register(BuiltInRegistries.ITEM, key, item));
        });
        return holder;
    }

    // ------------------------------------------------------------------
    // Blocks
    // ------------------------------------------------------------------

    /**
     * Declares a plain block, with no item form.
     *
     * @param name the path part of its id
     * @return a handle, bound once {@link #apply()} runs
     */
    public Holder<Block> block(String name) {
        return block(name, Block::new);
    }

    /**
     * Declares a block, with no item form.
     *
     * @param name    the path part of its id
     * @param factory builds it from properties that already carry its id
     * @return a handle, bound once {@link #apply()} runs
     */
    public Holder<Block> block(String name, Function<BlockBehaviour.Properties, Block> factory) {
        Objects.requireNonNull(factory, "factory");
        Identifier id = identifier(name);
        Holder<Block> holder = new Holder<>(id);

        defer(() -> {
            ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
            Block block = factory.apply(BlockBehaviour.Properties.of().setId(key));
            holder.bind(Registry.register(BuiltInRegistries.BLOCK, key, block));
            finaliseStates(block);
        });
        return holder;
    }

    /**
     * Declares a block together with the item that places it — the usual case.
     *
     * @param name the path part of the id, shared by the block and its item
     * @return a handle on the block, bound once {@link #apply()} runs
     */
    public Holder<Block> blockWithItem(String name) {
        return blockWithItem(name, Block::new);
    }

    /**
     * Declares a block together with the item that places it.
     *
     * @param name    the path part of the id, shared by the block and its item
     * @param factory builds the block from properties that already carry its id
     * @return a handle on the block, bound once {@link #apply()} runs
     */
    public Holder<Block> blockWithItem(String name, Function<BlockBehaviour.Properties, Block> factory) {
        Holder<Block> block = block(name, factory);
        Identifier id = identifier(name);

        defer(() -> {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
            // useBlockDescriptionPrefix: a block's item takes its name from the
            // block's translation key, which is what a player expects.
            BlockItem item = new BlockItem(block.get(),
                    new Item.Properties().setId(key).useBlockDescriptionPrefix());
            Registry.register(BuiltInRegistries.ITEM, key, item);

            // The mapping vanilla makes for its own blocks. Without it
            // Block.asItem() answers air and remembers that answer.
            item.registerBlocks(Item.BY_BLOCK, item);
        });
        return block;
    }

    // ------------------------------------------------------------------
    // Creative tabs
    // ------------------------------------------------------------------

    /**
     * Declares a creative tab of the mod's own.
     *
     * <pre>{@code
     * public static final ResourceKey<CreativeModeTab> TAB =
     *         REGISTRAR.creativeTab("example_mod", ModItems.RUBY);
     * }</pre>
     *
     * <p>Fill it the same way as any other tab:
     * {@code CreativeTabs.addTo(TAB, ModItems.RUBY)}.
     *
     * <p>Vanilla's fourteen slots are all taken, so the tab lands on a page of
     * its own — see {@link CreativePages}. Its position within that page is
     * assigned in declaration order, which is why nothing here asks for a row
     * or a column.
     *
     * <p>Its title is {@code itemGroup.<mod id>.<name>}, which
     * {@code EmberLanguageProvider} can translate like anything else.
     *
     * @param name the path part of its id
     * @param icon the block or item shown on the tab itself
     * @return its key, usable immediately
     */
    public ResourceKey<CreativeModeTab> creativeTab(String name, Holder<?> icon) {
        Objects.requireNonNull(icon, "icon");
        Identifier id = identifier(name);
        ResourceKey<CreativeModeTab> key = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id);

        defer(() -> {
            int slot = CreativePages.claimSlot();
            CreativeModeTab tab = CreativeModeTab
                    .builder(CreativePages.rowOf(slot), CreativePages.columnOf(slot))
                    .title(Component.translatable(CreativeTabs.titleKey(key)))
                    .icon(() -> new ItemStack(itemOf(icon)))
                    // No displayItems: the builder defaults to generating
                    // nothing, and CreativeTabs.addTo is the one way content
                    // reaches a tab, vanilla's or this mod's alike, so there is
                    // only ever one thing to learn.
                    .build();
            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, key, tab);
        });
        return key;
    }

    /**
     * A block stands in for the item that places it, so an icon can be either.
     */
    private static Item itemOf(Holder<?> holder) {
        Object value = holder.get();
        return switch (value) {
            case Item item -> item;
            case Block block -> block.asItem();
            default -> throw new IllegalArgumentException(
                    holder.id() + " is neither a block nor an item, so it cannot be a tab icon");
        };
    }

    // ------------------------------------------------------------------
    // Block entities
    // ------------------------------------------------------------------

    /**
     * Declares the type behind a block that stores something.
     *
     * <pre>{@code
     * public static final Holder<BlockEntityType<SafeBlockEntity>> SAFE =
     *         REGISTRAR.blockEntity("safe", SafeBlockEntity::new, ModBlocks.SAFE);
     * }</pre>
     *
     * <p>The blocks may be declared before or after this call. Block entity
     * types are registered in a pass of their own, after everything else, so
     * the order a mod happens to write its fields in cannot matter.
     *
     * @param <T>     the block entity class
     * @param name    the path part of its id
     * @param factory builds one, given where it is and what it is
     * @param blocks  the blocks that carry it; at least one
     * @return a handle, bound once {@link #apply()} runs
     * @throws IllegalArgumentException if no block is given
     */
    @SafeVarargs
    public final <T extends BlockEntity> Holder<BlockEntityType<T>> blockEntity(
            String name, BlockEntityType.BlockEntitySupplier<T> factory, Holder<Block>... blocks) {
        Objects.requireNonNull(factory, "factory");
        if (blocks.length == 0) {
            throw new IllegalArgumentException(name + " has no blocks — a block entity type that "
                    + "belongs to no block can never be created, and nothing would say so");
        }
        Identifier id = identifier(name);
        Holder<BlockEntityType<T>> holder = new Holder<>(id);

        deferLate(() -> {
            Set<Block> valid = new LinkedHashSet<>();
            for (Holder<Block> block : blocks) {
                valid.add(requireEntityBlock(block, id));
            }
            ResourceKey<BlockEntityType<?>> key = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, id);
            holder.bind(Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key,
                    new BlockEntityType<>(factory, valid)));
        });
        return holder;
    }

    /**
     * A block that does not answer {@code EntityBlock} never creates its block
     * entity: the type is registered, the block places fine, and whatever it
     * was meant to store is quietly never there. Refusing here turns a bug
     * found hours later in game into one found at startup.
     */
    private static Block requireEntityBlock(Holder<Block> holder, Identifier type) {
        Block block = Objects.requireNonNull(holder, "block").get();
        if (!(block instanceof EntityBlock)) {
            throw new IllegalArgumentException(holder.id() + " carries the block entity " + type
                    + " but does not implement EntityBlock, so the game would never create one");
        }
        return block;
    }

    // ------------------------------------------------------------------
    // Entities
    // ------------------------------------------------------------------

    /**
     * Declares an entity type.
     *
     * <pre>{@code
     * public static final Holder<EntityType<RubyBolt>> BOLT = REGISTRAR.entity(
     *         "ruby_bolt", RubyBolt::new, MobCategory.MISC,
     *         builder -> builder.sized(0.25f, 0.25f));
     * }</pre>
     *
     * <p>Anything that lives needs attributes too — see {@link #attributes} —
     * and anything visible needs a renderer, which is the client's business.
     *
     * @param <T>      the entity class
     * @param name     the path part of its id
     * @param factory  builds one, given its type and the level it is in
     * @param category what kind of thing it is; drives spawning and despawning
     * @param step     further shaping of the type, most often {@code sized}
     * @return a handle, bound once {@link #apply()} runs
     */
    public <T extends Entity> Holder<EntityType<T>> entity(
            String name, EntityType.EntityFactory<T> factory, MobCategory category,
            UnaryOperator<EntityType.Builder<T>> step) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(step, "step");
        Identifier id = identifier(name);
        Holder<EntityType<T>> holder = new Holder<>(id);

        defer(() -> {
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
            // The key goes into build(), not just into the registry: the type
            // keeps it for its translation key and its save id.
            holder.bind(Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
                    step.apply(EntityType.Builder.of(factory, category)).build(key)));
        });
        return holder;
    }

    /**
     * Gives a living entity its default attributes — health, speed and the
     * rest.
     *
     * <pre>{@code
     * REGISTRAR.attributes(ModEntities.SPRITE, () -> Mob.createMobAttributes()
     *         .add(Attributes.MAX_HEALTH, 8)
     *         .add(Attributes.MOVEMENT_SPEED, 0.25));
     * }</pre>
     *
     * <p>Not optional for anything living. A {@code LivingEntity} asks vanilla
     * for its attributes while it is being constructed, and an entity that is
     * not in that table dies there with a null map — inside vanilla, nowhere
     * near the mod that registered it.
     *
     * @param <T>        the entity class
     * @param type       the type to describe
     * @param attributes builds the attribute set; called once, during
     *                   {@link #apply()}
     */
    public <T extends LivingEntity> void attributes(Holder<EntityType<T>> type,
                                                    Supplier<AttributeSupplier.Builder> attributes) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(attributes, "attributes");

        // Recorded, not built: the values are written against attribute
        // holders that are still unbound while a mod registers, so nothing is
        // resolved until the game first asks for them.
        deferLate(() -> EntityAttributes.declare(type.get(), attributes));
    }

    // ------------------------------------------------------------------
    // Menus
    // ------------------------------------------------------------------

    /**
     * Declares a menu type — the thing a block opens.
     *
     * <pre>{@code
     * public static final Holder<MenuType<SafeMenu>> SAFE =
     *         REGISTRAR.menu("safe", SafeMenu::new);
     * }</pre>
     *
     * <p>The factory runs on the <em>client</em>, when the server says a window
     * has opened, and is given only the window id and the player's inventory —
     * because that is all the client is told. A menu showing a block's contents
     * therefore builds an empty container here and lets the sync fill it, which
     * is exactly what vanilla's chests do.
     *
     * <p>Opening one is the server's job:
     *
     * <pre>{@code
     * player.openMenu(new SimpleMenuProvider(
     *         (id, inventory, who) -> new SafeMenu(id, inventory, contents),
     *         Component.translatable("container.mymod.safe")));
     * }</pre>
     *
     * @param <T>     the menu class
     * @param name    the path part of its id
     * @param factory builds one on the client
     * @return a handle, bound once {@link #apply()} runs
     */
    public <T extends AbstractContainerMenu> Holder<MenuType<T>> menu(
            String name, MenuFactory<T> factory) {
        Objects.requireNonNull(factory, "factory");
        Identifier id = identifier(name);
        Holder<MenuType<T>> holder = new Holder<>(id);

        defer(() -> {
            ResourceKey<MenuType<?>> key = ResourceKey.create(Registries.MENU, id);
            // Both the constructor and its parameter type are private in
            // vanilla; this module declares them accessible, which is why the
            // mod above never has to know they exist.
            holder.bind(Registry.register(BuiltInRegistries.MENU, key,
                    new MenuType<>(factory::create, FeatureFlags.VANILLA_SET)));
        });
        return holder;
    }

    // ------------------------------------------------------------------
    // Sounds
    // ------------------------------------------------------------------

    /**
     * Declares a sound event.
     *
     * <p>The event is only half of a sound: the other half is an entry in
     * {@code sounds.json} naming the ogg files to play, which
     * {@code EmberSoundProvider} generates.
     *
     * @param name the path part of its id, and the name used in {@code sounds.json}
     * @return a handle, bound once {@link #apply()} runs
     */
    public Holder<SoundEvent> sound(String name) {
        Identifier id = identifier(name);
        Holder<SoundEvent> holder = new Holder<>(id);

        defer(() -> {
            ResourceKey<SoundEvent> key = ResourceKey.create(Registries.SOUND_EVENT, id);
            holder.bind(Registry.register(BuiltInRegistries.SOUND_EVENT, key,
                    SoundEvent.createVariableRangeEvent(id)));
        });
        return holder;
    }

    // ------------------------------------------------------------------
    // Spawning
    // ------------------------------------------------------------------

    /**
     * Declares a spawn egg for an entity.
     *
     * <pre>{@code
     * public static final Holder<Item> WISP_EGG = REGISTRAR.spawnEgg("wisp_spawn_egg", WISP);
     * }</pre>
     *
     * <p>A spawn egg is an ordinary flat item in 26.2 — one texture, no tint
     * template — so {@code EmberModelProvider.flatItem} writes its model and
     * the two colours are the texture's own.
     *
     * <p>Registered after every entity type, so the egg and the entity can be
     * declared in whichever order reads best.
     *
     * @param <T>  the entity class
     * @param name the path part of the item's id
     * @param type the entity it spawns
     * @return a handle, bound once {@link #apply()} runs
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public <T extends Entity> Holder<Item> spawnEgg(String name, Holder<EntityType<T>> type) {
        Objects.requireNonNull(type, "type");
        Identifier id = identifier(name);
        Holder<Item> holder = new Holder<>(id);

        // Late, because the properties need the entity type itself rather than
        // a promise of one.
        deferLate(() -> {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
            holder.bind(Registry.register(BuiltInRegistries.ITEM, key,
                    new SpawnEggItem(new Item.Properties().setId(key).spawnEgg(type.get()))));
        });
        return holder;
    }

    /**
     * Says where and when an entity may spawn on its own.
     *
     * <pre>{@code
     * REGISTRAR.spawnRule(WISP, SpawnPlacementTypes.ON_GROUND,
     *         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules);
     * }</pre>
     *
     * <p>This is one half of natural spawning; the other is a biome giving the
     * mob a weight, which is data rather than code. Without <em>this</em> half
     * the entity never spawns anywhere at all — which reads as a wrong spawn
     * weight rather than as a missing registration.
     *
     * <p>Registered after every entity type, so declaration order does not
     * matter.
     *
     * @param <T>           the mob class
     * @param type          the entity type
     * @param placementType what it needs underfoot
     * @param heightmap     which surface a candidate position is measured from
     * @param predicate     the final say — light level, difficulty, whatever
     *                      the mob cares about
     * @throws NullPointerException if any argument is {@code null}
     */
    public <T extends Mob> void spawnRule(Holder<EntityType<T>> type,
                                          SpawnPlacementType placementType,
                                          Heightmap.Types heightmap,
                                          SpawnPlacements.SpawnPredicate<T> predicate) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(placementType, "placementType");
        Objects.requireNonNull(heightmap, "heightmap");
        Objects.requireNonNull(predicate, "predicate");

        deferLate(() -> SpawnPlacementsInvoker.fenix$register(
                type.get(), placementType, heightmap, predicate));
    }

    // ------------------------------------------------------------------
    // Smaller registries
    // ------------------------------------------------------------------

    /**
     * Declares a particle carrying no data of its own.
     *
     * <p>This is the half both sides need. The client also has to say what the
     * particle looks like, and needs a file under {@code particles/} naming its
     * textures.
     *
     * @param name the path part of its id
     * @return a handle, bound once {@link #apply()} runs
     */
    public Holder<SimpleParticleType> particle(String name) {
        return particle(name, false);
    }

    /**
     * Declares a particle carrying no data of its own.
     *
     * @param name          the path part of its id
     * @param alwaysVisible {@code true} to ignore the particle-distance
     *                      setting, as vanilla's explosions do. Reserve it for
     *                      particles carrying meaning a player must not miss.
     * @return a handle, bound once {@link #apply()} runs
     */
    public Holder<SimpleParticleType> particle(String name, boolean alwaysVisible) {
        Identifier id = identifier(name);
        Holder<SimpleParticleType> holder = new Holder<>(id);

        defer(() -> {
            ResourceKey<ParticleType<?>> key = ResourceKey.create(Registries.PARTICLE_TYPE, id);
            holder.bind(Registry.register(BuiltInRegistries.PARTICLE_TYPE, key,
                    new SimpleParticleType(alwaysVisible)));
        });
        return holder;
    }

    /**
     * Declares a status effect.
     *
     * <pre>{@code
     * public static final Holder<MobEffect> GLIMMER =
     *         REGISTRAR.effect("glimmer", new GlimmerEffect());
     * }</pre>
     *
     * <p>The effect is a class of the mod's own extending {@code MobEffect};
     * what it does lives in {@code applyEffectTick}.
     *
     * @param <T>    the effect class
     * @param name   the path part of its id
     * @param effect the effect
     * @return a handle, bound once {@link #apply()} runs
     * @throws NullPointerException if {@code effect} is {@code null}
     */
    public <T extends MobEffect> Holder<T> effect(String name, T effect) {
        Objects.requireNonNull(effect, "effect");
        Identifier id = identifier(name);
        Holder<T> holder = new Holder<>(id);

        defer(() -> {
            ResourceKey<MobEffect> key = ResourceKey.create(Registries.MOB_EFFECT, id);
            Registry.register(BuiltInRegistries.MOB_EFFECT, key, effect);
            holder.bind(effect);
        });
        return holder;
    }

    /**
     * Declares a data component — a typed piece of state a stack carries.
     *
     * <pre>{@code
     * public static final Holder<DataComponentType<Integer>> CHARGE =
     *         REGISTRAR.dataComponent("charge", builder -> builder
     *                 .persistent(Codec.INT)
     *                 .networkSynchronized(ByteBufCodecs.VAR_INT));
     * }</pre>
     *
     * <p>Say {@code persistent} for state that has to survive saving, and
     * {@code networkSynchronized} for state the client needs in order to draw.
     * A component with neither lasts until the stack is next looked at.
     *
     * @param <T>   the value type
     * @param name  the path part of its id
     * @param build fills in the builder
     * @return a handle, bound once {@link #apply()} runs
     * @throws NullPointerException if {@code build} is {@code null}
     */
    public <T> Holder<DataComponentType<T>> dataComponent(
            String name, UnaryOperator<DataComponentType.Builder<T>> build) {
        Objects.requireNonNull(build, "build");
        Identifier id = identifier(name);
        Holder<DataComponentType<T>> holder = new Holder<>(id);

        defer(() -> {
            ResourceKey<DataComponentType<?>> key =
                    ResourceKey.create(Registries.DATA_COMPONENT_TYPE, id);
            holder.bind(Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, key,
                    build.apply(DataComponentType.builder()).build()));
        });
        return holder;
    }

    /**
     * {@return the key of one of this mod's placed features}
     *
     * <p>Names rather than registers: features are datapack data, written by
     * {@code EmberOreProvider} and loaded by the game. What this is for is
     * pointing at one, usually to hand to
     * {@code BiomeModifications.addFeature}.
     *
     * @param name the file's name under {@code worldgen/placed_feature/}
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public ResourceKey<PlacedFeature> placedFeature(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, identifier(name));
    }

    // ------------------------------------------------------------------
    // Applying
    // ------------------------------------------------------------------

    /**
     * Registers everything declared so far. Call this from
     * {@code onRegister}; calling it twice does nothing the second time.
     */
    public void apply() {
        if (applied) {
            return;
        }
        applied = true;
        for (Runnable registration : pending) {
            registration.run();
        }
        // Anything that needs another registration to have happened already —
        // block entity types need their blocks — waits for this second pass,
        // so a mod never has to order its own declarations to suit us.
        for (Runnable registration : pendingLate) {
            registration.run();
        }
        pending.clear();
        pendingLate.clear();
    }

    private void defer(Runnable registration) {
        requireOpen();
        pending.add(registration);
    }

    private void deferLate(Runnable registration) {
        requireOpen();
        pendingLate.add(registration);
    }

    private void requireOpen() {
        if (applied) {
            throw new IllegalStateException(modId + " declared content after registering — the game's "
                    + "registries are shut by now, so this would never have taken effect");
        }
    }

    /**
     * {@return an id in this mod's namespace}
     *
     * <p>For naming things the registrar does not register — a key binding, a
     * tag, a file in the mod's own data.
     *
     * @param name the path part
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public Identifier identifier(String name) {
        return Identifier.fromNamespaceAndPath(modId, Objects.requireNonNull(name, "name"));
    }

    /**
     * Redoes, for one block, the pass vanilla runs over its own blocks at the
     * tail of {@code Blocks}' static initialiser — long before a mod can
     * register anything.
     */
    private static void finaliseStates(Block block) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            Block.BLOCK_STATE_REGISTRY.add(state);
            state.initCache();
        }
    }
}
