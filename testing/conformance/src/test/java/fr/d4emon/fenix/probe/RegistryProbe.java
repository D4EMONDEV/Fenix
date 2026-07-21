package fr.d4emon.fenix.probe;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Runs as the game: boots the registries, which fires onRegister, then checks
 * that every pass vanilla performs around its own registration also happened
 * for the mod's content.
 *
 * <p>Failure is an exception, which the loader propagates out of
 * {@code Launch.run} and into the test.
 */
public final class RegistryProbe {

    private RegistryProbe() {
    }

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        Identifier blockId = Identifier.parse("probemod:ruby_block");
        Identifier itemId = Identifier.parse("probemod:ruby");

        Block block = BuiltInRegistries.BLOCK.getValue(blockId);
        require(block != null && block != Blocks.AIR, "the block should be in the registry");
        require(BuiltInRegistries.ITEM.getValue(itemId) != null, "the item should be in the registry");

        // Vanilla assigns these in one pass in Blocks' static initialiser, long
        // finished by the time a mod registers. Missing them kicks the player
        // with "Can't find id for Block{...}" and breaks rendering.
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            require(Block.BLOCK_STATE_REGISTRY.getId(state) >= 0,
                    "every block state should have a network id");
        }

        // Without the Item.BY_BLOCK mapping, asItem() answers air and caches it,
        // so ItemStacks of the block are empty and the creative tab dies.
        require(block.asItem() != Items.AIR, "asItem() should not be air");
        require(block.asItem() == BuiltInRegistries.ITEM.getValue(blockId),
                "asItem() should be the block's own item");

        System.out.println("registry conformance: all checks passed");
    }

    private static void require(boolean condition, String what) {
        if (!condition) {
            throw new AssertionError("registry conformance failed: " + what);
        }
    }
}
