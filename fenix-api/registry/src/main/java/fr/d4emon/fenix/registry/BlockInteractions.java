package fr.d4emon.fenix.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The behaviour vanilla keeps in tables rather than on the block itself.
 *
 * <pre>{@code
 * BlockInteractions.flammable(ModBlocks.RUBY_PLANKS, 5, 20);
 * BlockInteractions.strippable(ModBlocks.RUBY_LOG, ModBlocks.STRIPPED_RUBY_LOG);
 * BlockInteractions.fuel(ModItems.RUBY_COAL, 1600);
 * }</pre>
 *
 * <p>A block's properties say how hard it is and what sound it makes. They do
 * not say whether it burns, composts, strips, waxes, oxidises or fuels a
 * furnace: each of those lives in a table somewhere else in the game, filled in
 * one pass during bootstrap, long before a mod exists. So a modded wood type
 * looks and behaves like wood and quietly is not: it will not catch fire, an axe
 * does nothing to it, and it cannot go in a furnace. Nothing warns, because from
 * vanilla's side nothing is wrong — the block simply is not in the table.
 *
 * <p>Every table here is either immutable or built once, so the ones that cannot
 * be added to are consulted <em>before</em> vanilla's by a mixin, the same
 * arrangement {@code EntityAttributes} uses. The composter's is the exception:
 * it is a public mutable map, so its entries go straight in.
 *
 * <p>Call these from {@code onRegister}, in any order relative to registration —
 * blocks are resolved the first time the game asks a question, not when they are
 * declared.
 */
public final class BlockInteractions {

    /**
     * How readily a block catches, and how readily it burns away.
     *
     * <p>Public because the mixin on {@code FireBlock} reads it; a mod declares
     * one through {@link #flammable} rather than building it.
     *
     * @param igniteOdds how easily fire spreads to the block
     * @param burnOdds   how easily the block is consumed once alight
     */
    public record Flammability(int igniteOdds, int burnOdds) {
    }

    private static final LazyTable<Block, Flammability> FLAMMABLE = new LazyTable<>();
    private static final LazyTable<Block, Supplier<Block>> STRIPPABLE = new LazyTable<>();
    private static final LazyTable<Block, Supplier<Block>> WAXABLE = new LazyTable<>();
    private static final LazyTable<Block, Supplier<Block>> OXIDISES_TO = new LazyTable<>();
    private static final LazyTable<Item, Integer> FUEL = new LazyTable<>();

    /** Composter entries declared before their item existed; see {@link #compostable}. */
    private static final List<Map.Entry<Holder<? extends ItemLike>, Float>> PENDING_COMPOST =
            new ArrayList<>();

    private BlockInteractions() {
    }

    /**
     * Lets a block catch fire and burn away.
     *
     * <p>Vanilla's planks are {@code (5, 20)} and its logs {@code (5, 5)}; leaves
     * are {@code (30, 60)}. The first number is how readily fire spreads to the
     * block, the second how readily the block is consumed once alight.
     *
     * @param block      the block
     * @param igniteOdds how easily it catches, 0 to 100
     * @param burnOdds   how easily it burns away, 0 to 100
     */
    public static void flammable(Holder<Block> block, int igniteOdds, int burnOdds) {
        FLAMMABLE.declare(block, new Flammability(igniteOdds, burnOdds));
    }

    /**
     * Lets an item go into a composter.
     *
     * <p>Vanilla's leaves and seeds are {@code 0.3f}, its crops {@code 0.65f},
     * a cake {@code 1.0f} — the chance one item raises the compost level.
     *
     * <p>Unlike the rest here this reaches vanilla's own table directly, which is
     * public and mutable, so a composter finds it with no help from Fenix.
     *
     * @param item   the item, a block's own item included
     * @param chance the chance of raising the level, above 0 and at most 1
     */
    public static void compostable(Holder<? extends ItemLike> item, float chance) {
        Objects.requireNonNull(item, "item");
        if (item.isBound()) {
            ComposterBlock.COMPOSTABLES.put(item.get(), chance);
            return;
        }
        // Not registered yet, so there is nothing to key the map by. Held until
        // apply() runs, which is the moment every holder becomes readable —
        // otherwise declaring this beside the item, which reads best, would be
        // the one arrangement that does not work.
        synchronized (PENDING_COMPOST) {
            PENDING_COMPOST.add(Map.entry(item, chance));
        }
    }

    /**
     * Writes any held composter entries into vanilla's table.
     *
     * <p>Called by {@link Registrar#apply()}. The composter reads its own map in
     * five places and that map is mutable, so the entries go in rather than
     * being answered around — but they can only go in once the items exist.
     */
    static void flushCompostables() {
        synchronized (PENDING_COMPOST) {
            for (Map.Entry<Holder<? extends ItemLike>, Float> entry : PENDING_COMPOST) {
                // put(K, float), not put(K, Float): the boxed overload is the
                // deprecated one every fastutil map carries for compatibility.
                ComposterBlock.COMPOSTABLES.put(entry.getKey().get(), entry.getValue().floatValue());
            }
            PENDING_COMPOST.clear();
        }
    }

    /**
     * Lets an axe strip a block into another — a log into a stripped log.
     *
     * <p>Both blocks need an {@code axis} property, which they have if they are
     * built on {@code RotatedPillarBlock}: vanilla carries the axis across so a
     * sideways log stays sideways, and reads it off the stripped block without
     * checking first.
     *
     * @param block    the block an axe is used on
     * @param stripped what it becomes
     */
    public static void strippable(Holder<Block> block, Holder<Block> stripped) {
        Objects.requireNonNull(stripped, "stripped");
        STRIPPABLE.declare(block, stripped::get);
    }

    /**
     * Lets a honeycomb wax a block, and an axe scrape it off again.
     *
     * <p>The reverse direction comes free: vanilla reads the same table
     * backwards, so scraping works the moment waxing does.
     *
     * @param block the block honeycomb is used on
     * @param waxed what it becomes
     */
    public static void waxable(Holder<Block> block, Holder<Block> waxed) {
        Objects.requireNonNull(waxed, "waxed");
        WAXABLE.declare(block, waxed::get);
    }

    /**
     * Lets a block weather into the next stage, as copper does.
     *
     * <p>Declare each step of the chain — unoxidised to exposed, exposed to
     * weathered, and so on. Scraping back a stage comes free, the same way
     * unwaxing does, because vanilla reads the chain in both directions.
     *
     * <p>The block itself still has to implement vanilla's
     * {@code WeatheringCopper} to weather over time; this is the table that says
     * what it turns into.
     *
     * @param block the block that weathers
     * @param next  the stage after it
     */
    public static void oxidation(Holder<Block> block, Holder<Block> next) {
        Objects.requireNonNull(next, "next");
        OXIDISES_TO.declare(block, next::get);
    }

    /**
     * Lets an item burn in a furnace.
     *
     * <p>Vanilla measures in ticks: coal is 1600, a plank 300, a lava bucket
     * 20000. An item can also become fuel by joining a tag vanilla already
     * burns, {@code minecraft:logs} among them — worth preferring when it fits,
     * since a datapack can then tune it.
     *
     * @param item  the item
     * @param ticks how long one of them burns
     */
    public static void fuel(Holder<? extends ItemLike> item, int ticks) {
        Objects.requireNonNull(item, "item");
        FUEL.declare(() -> item.get().asItem(), ticks);
    }

    // ------------------------------------------------------------------
    // What the mixins read
    // ------------------------------------------------------------------

    /** {@return a mod's flammability for a block, or {@code null}} */
    public static Flammability flammabilityOf(Block block) {
        return FLAMMABLE.get(block);
    }

    /** {@return what a mod's block strips into, or {@code null}} */
    public static Block strippedOf(Block block) {
        return resolve(STRIPPABLE.get(block));
    }

    /**
     * {@return every waxing a mod declared, block to waxed block}
     *
     * <p>The whole table rather than one lookup, because vanilla's is replaced
     * wholesale: four unrelated places read it inline — an axe, a carved
     * pumpkin, a copper chest, a lightning bolt — and answering ahead of each
     * would mean four injections and a fifth the next time vanilla adds one.
     */
    public static Map<Block, Block> waxables() {
        return WAXABLE.resolvedTargets();
    }

    /** {@return every oxidation step a mod declared, block to the stage after it} */
    public static Map<Block, Block> oxidations() {
        return OXIDISES_TO.resolvedTargets();
    }

    /** {@return how long a mod's item burns, or {@code null}} */
    public static Integer fuelOf(Item item) {
        return FUEL.get(item);
    }

    private static Block resolve(Supplier<Block> value) {
        return value == null ? null : value.get();
    }

    /**
     * Declarations kept until something asks, then resolved once.
     *
     * <p>A mod declares against a {@link Holder}, which is a promise rather than
     * a block, so nothing here can be keyed by block until registration has
     * happened. Resolving on the first question rather than at declaration is
     * what lets these be called in whatever order reads best — including before
     * the content they name is registered.
     *
     * @param <K> what the table is keyed by once resolved
     * @param <V> what it answers
     */
    private static final class LazyTable<K, V> {

        private final List<Map.Entry<Supplier<K>, V>> pending = new ArrayList<>();
        private final Map<K, V> resolved = new IdentityHashMap<>();

        void declare(Supplier<K> key, V value) {
            Objects.requireNonNull(key, "key");
            synchronized (this) {
                pending.add(Map.entry(key, value));
            }
        }

        V get(K key) {
            drain();
            return resolved.get(key);
        }

        /**
         * {@return every entry, with the target resolved}
         *
         * <p>Only meaningful where the value is a promise of a block, which is
         * every table vanilla keeps as a whole map rather than answers one
         * question at a time.
         */
        @SuppressWarnings("unchecked")
        Map<K, Block> resolvedTargets() {
            drain();
            Map<K, Block> targets = new IdentityHashMap<>();
            resolved.forEach((key, value) ->
                    targets.put(key, ((Supplier<Block>) value).get()));
            return targets;
        }

        private void drain() {
            if (pending.isEmpty()) {
                return;
            }
            synchronized (this) {
                for (Map.Entry<Supplier<K>, V> entry : pending) {
                    resolved.put(entry.getKey().get(), entry.getValue());
                }
                pending.clear();
            }
        }
    }
}
