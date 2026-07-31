package fr.d4emon.fenix.probe;

import fr.d4emon.fenix.event.LootEvents;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/**
 * Runs as the game: adds a pool to a loot table and checks it took.
 *
 * <p>Adding rather than replacing is the whole point — two mods both dropping
 * something from stone is the case a datapack override cannot serve, since the
 * second copy of the file wins and the first mod's drop disappears with nothing
 * said. So the check is that a table gains a pool and keeps the ones it had.
 *
 * <p>This also exercises the part most likely to break silently: rebuilding a
 * loot table needs its private constructor, which Fenix widens from the
 * manifest. If that widening ever stops being applied the failure is an
 * {@code IllegalAccessError} from inside a datapack load, which is a miserable
 * place to meet one.
 */
public final class LootProbe {

    private LootProbe() {
    }

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        LootTable original = LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(Items.STONE)))
                .build();

        LootEvents.LOADING.register(loot -> {
            if (loot.id().equals(Identifier.parse("minecraft:blocks/stone"))) {
                loot.addPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(Items.DIAMOND))
                        .build());
            }
        });

        LootTable changed = LootEvents.fire(Identifier.parse("minecraft:blocks/stone"), original);
        require(changed != original, "adding a pool should produce a new table");
        require(pools(changed) == pools(original) + 1,
                "the table should have gained exactly one pool, and kept the one it had — "
                        + "replacing instead of adding is how one mod erases another's drops");

        // A table nobody asked about must come back untouched, or every listener
        // would pay for every table in the game.
        LootTable other = LootEvents.fire(Identifier.parse("minecraft:blocks/dirt"), original);
        require(other == original, "a table no listener claimed should be handed back as it was");

        System.out.println("loot conformance: all checks passed");
    }

    /** {@return how many pools a table rolls} */
    private static int pools(LootTable table) {
        try {
            var field = LootTable.class.getDeclaredField("pools");
            field.setAccessible(true);
            return ((java.util.List<?>) field.get(table)).size();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("loot conformance failed: could not read the pools", e);
        }
    }

    private static void require(boolean condition, String what) {
        if (!condition) {
            throw new AssertionError("loot conformance failed: " + what);
        }
    }
}
