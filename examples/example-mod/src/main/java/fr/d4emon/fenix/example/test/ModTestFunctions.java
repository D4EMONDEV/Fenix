package fr.d4emon.fenix.example.test;

import fr.d4emon.fenix.example.block.entity.RubyReforgingBlockEntity;
import fr.d4emon.fenix.example.registry.ModBlocks;
import fr.d4emon.fenix.example.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import fr.d4emon.fenix.example.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The mod's game tests: code that runs inside a real world and asserts.
 *
 * <p>These check what no static check can. Whether a loot table parses is a
 * question about a file, and the conformance suite answers it. Whether breaking
 * the block actually drops the item is a question about a world, and only a
 * world answers it.
 *
 * <p>Each function here is named by a {@code test_instance} file Ember writes.
 * A function nothing names never runs, and reports as absent rather than as a
 * failure.
 */
public final class ModTestFunctions {

    /** Where each test works, one block in from the structure's corner. */
    private static final BlockPos ORIGIN = new BlockPos(1, 2, 1);

    private ModTestFunctions() {
    }

    /**
     * Touches this class, which is what runs the field initialisers above.
     *
     * <p>Nothing at runtime refers to these fields — the test instance files
     * name them by id, not by symbol — so without this call the class is never
     * loaded and not one test function is registered. The runner then finds the
     * instances, looks for their functions and reports "missing test function"
     * for every one.
     */
    public static void load() {
    }

    /**
     * The ore drops with a pickaxe, and drops nothing without one.
     *
     * <p>Both halves, because each alone passes for the wrong reason. The loot
     * table is generated, parsed by the conformance suite and proven to be JSON
     * the game's codec accepts — and none of that says the table is reached. A
     * block marked {@code requiresTool} that is in no {@code mineable} tag
     * drops nothing at all, with a valid table, a correct hardness and no log
     * line. That happened here, to seven blocks at once.
     *
     * <p>Broken through a player rather than through {@code Block.getDrops}:
     * whether the tool was correct is decided in the player's own break path,
     * not by the loot table, so asking the table directly answers a question
     * nobody was asking.
     */
    public static final Identifier ORE_DROPS =
            ModContent.REGISTRAR.testFunction("ore_drops", helper -> {
                // Survival explicitly, because a creative player destroys blocks
                // without dropping anything -- which reads exactly like a loot
                // table that was never reached.
                //
                // makeMockServerPlayer and not makeMockServerPlayerInLevel:
                // the latter is deprecated for removal in 26.2, and it is the
                // one that leaves the mode to the level.
                ServerPlayer player =
                        (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
                BlockPos pos = helper.absolutePos(ORIGIN);

                helper.setBlock(ORIGIN, ModBlocks.RUBY_ORE.get());
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.gameMode.destroyBlock(pos);
                helper.assertItemEntityNotPresent(ModItems.RUBY.get(), ORIGIN, 3.0);

                helper.setBlock(ORIGIN, ModBlocks.RUBY_ORE.get());
                player.setItemInHand(InteractionHand.MAIN_HAND,
                        new ItemStack(Items.DIAMOND_PICKAXE));
                player.gameMode.destroyBlock(pos);
                // A dropped item is added to the level, not to the tick that
                // dropped it, so asking on the same tick asks too early.
                helper.succeedWhen(() ->
                        helper.assertItemEntityPresent(ModItems.RUBY.get(), ORIGIN, 3.0));
            });

    /**
     * The mod's door opens by hand.
     *
     * <p>This is the test the demo earned. Its doors were once redstone-only,
     * because their block set type was iron's and iron's {@code canOpenByHand}
     * is false — a fact visible nowhere in the mod's own source and reported by
     * nothing. Opening the door is the only thing that would have said so.
     */
    public static final Identifier DOOR_OPENS_BY_HAND =
            ModContent.REGISTRAR.testFunction("door_opens_by_hand", helper -> {
                helper.setBlock(ORIGIN, ModBlocks.RUBY_DOOR.get());
                helper.useBlock(ORIGIN, helper.makeMockPlayer(GameType.SURVIVAL));

                BlockState state = helper.getBlockState(ORIGIN);
                if (!state.getValue(BlockStateProperties.OPEN)) {
                    helper.fail("the door did not open when a player used it, which is what "
                            + "a block set type whose canOpenByHand is false looks like");
                }
                helper.succeed();
            });

    /**
     * The reforging station's block entity is really there.
     *
     * <p>A block entity type is registered against the blocks it belongs to,
     * and a type bound to the wrong block leaves the block standing with
     * nothing behind it. The block still places, still renders and still has
     * its name; only opening it shows that nothing is home, and nothing logs.
     */
    public static final Identifier BLOCK_ENTITY_IS_BOUND =
            ModContent.REGISTRAR.testFunction("block_entity_is_bound", helper -> {
                helper.setBlock(ORIGIN, ModBlocks.RUBY_REFORGING.get());
                // Throws if there is nothing there, or if what is there is
                // some other class -- both of which are the same mistake.
                helper.getBlockEntity(ORIGIN, RubyReforgingBlockEntity.class);
                helper.succeed();
            });

    /**
     * The mod's statistic counts, and survives being read back.
     *
     * <p>A statistic that is awarded but never registered is dropped from the
     * player's file when it is written — silently, and only noticed when
     * someone opens the statistics screen and finds nothing there. Awarding it
     * and reading it back is the only thing that says otherwise.
     */
    public static final Identifier STAT_COUNTS =
            ModContent.REGISTRAR.testFunction("stat_counts", helper -> {
                ServerPlayer player =
                        (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);

                // Asked first, because the failure otherwise arrives as a null
                // pointer from inside the stats code — true, but it names
                // nothing a reader can act on.
                if (!BuiltInRegistries.CUSTOM_STAT.containsKey(ModContent.HAMMER_SWINGS)) {
                    helper.fail("the statistic is not registered, so awarding it writes a "
                            + "number the player's file drops on save");
                }

                int before = player.getStats().getValue(
                        Stats.CUSTOM.get(ModContent.HAMMER_SWINGS));
                player.awardStat(ModContent.HAMMER_SWINGS, 3);
                int after = player.getStats().getValue(
                        Stats.CUSTOM.get(ModContent.HAMMER_SWINGS));

                if (after - before != 3) {
                    helper.fail("the statistic did not count: " + before + " then " + after
                            + ". A statistic that is awarded but not registered is dropped "
                            + "rather than rejected");
                }
                helper.succeed();
            });
}
