package fr.d4emon.fenix.event;

import fr.d4emon.fenix.mixin.event.LootTableAccessor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * What the game drops.
 *
 * <pre>{@code
 * LootEvents.LOADING.register(loot -> {
 *     if (loot.id().equals(Identifier.parse("minecraft:blocks/stone"))) {
 *         loot.addPool(LootPool.lootPool()
 *                 .setRolls(ConstantValue.exactly(1))
 *                 .add(LootItem.lootTableItem(ModItems.RUBY.get()))
 *                 .build());
 *     }
 * });
 * }</pre>
 *
 * <p>Adding to a table rather than replacing it is what lets two mods both drop
 * something from stone. Overriding the file in a datapack does not compose: the
 * second mod's copy wins and the first mod's drop is gone, with nothing to say
 * so — the same reason {@code BiomeModifications} exists.
 *
 * <p>Fires as tables are read, on every datapack reload, so a mod's additions
 * come back each time exactly as vanilla's own tables do.
 */
public final class LootEvents {

    /**
     * A loot table on its way in, before anything can roll it.
     *
     * <p>Mutable on purpose, like the tooltip event's list: a listener says what
     * it wants changed and the next listener sees the result, so two mods adding
     * to the same table both get their pool.
     */
    public static final class Loading {

        private final Identifier id;
        private LootTable table;

        Loading(Identifier id, LootTable table) {
            this.id = id;
            this.table = table;
        }

        /** {@return which table this is, such as {@code minecraft:blocks/stone}} */
        public Identifier id() {
            return id;
        }

        /** {@return the table as it stands, including earlier listeners' changes} */
        public LootTable table() {
            return table;
        }

        /**
         * Adds a pool to the table.
         *
         * <p>The table is rebuilt rather than edited — a loot table holds its
         * pools in an immutable list — so this is a replacement that keeps
         * everything already there.
         *
         * @param pool the pool to add
         */
        public void addPool(LootPool pool) {
            Objects.requireNonNull(pool, "pool");
            LootTableAccessor source = (LootTableAccessor) (Object) table;
            List<LootPool> pools = new ArrayList<>(source.fenix$pools());
            pools.add(pool);
            table = new LootTable(source.fenix$paramSet(), source.fenix$randomSequence(),
                    List.copyOf(pools), source.fenix$functions());
        }

        /**
         * Replaces the table outright.
         *
         * <p>Reach for {@link #addPool} instead where it fits: a replacement
         * throws away whatever another mod added before this listener ran, and
         * whatever a datapack the player installed had to say.
         *
         * @param replacement the table to use instead
         */
        public void replace(LootTable replacement) {
            table = Objects.requireNonNull(replacement, "replacement");
        }
    }

    /** Fires for each loot table as it is read, before anything rolls it. */
    public static final Event<Loading> LOADING = Event.create();

    private LootEvents() {
    }

    /**
     * Runs every listener over one table. Called by the mixin on the loader.
     *
     * @param id    the table's id
     * @param table the table as read from the datapack
     * @return the table to register, changed or not
     */
    public static LootTable fire(Identifier id, LootTable table) {
        Loading loading = new Loading(id, table);
        LOADING.fire(loading);
        return loading.table();
    }
}
