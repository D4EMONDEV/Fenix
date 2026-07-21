package fr.d4emon.fenix.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

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
        pending.clear();
    }

    private void defer(Runnable registration) {
        if (applied) {
            throw new IllegalStateException(modId + " declared content after registering — the game's "
                    + "registries are shut by now, so this would never have taken effect");
        }
        pending.add(registration);
    }

    private Identifier identifier(String name) {
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
