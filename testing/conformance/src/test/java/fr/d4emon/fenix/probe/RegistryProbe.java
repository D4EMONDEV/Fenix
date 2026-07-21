package fr.d4emon.fenix.probe;

import fr.d4emon.fenix.registry.CreativePages;
import fr.d4emon.fenix.registry.CreativeTabs;
import fr.d4emon.fenix.registry.Registrar;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

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

        checkCreativePage();
        checkBlockEntity();
        checkSound();

        System.out.println("registry conformance: all checks passed");
    }

    /**
     * The mod's tab has to land somewhere reachable.
     *
     * <p>Getting this far already proves the harder half: vanilla's bootstrap
     * refuses to start when two tabs share a square, and a mod tab always does
     * — every one of vanilla's fourteen slots is taken.
     */
    private static void checkCreativePage() {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(ProbeContent.TAB);
        require(tab != null, "the mod's creative tab should be in the registry");

        require(CreativePages.pageOf(tab) == 1,
                "the first mod tab belongs on page 1, since vanilla fills page 0");
        require(CreativePages.count() == 2, "one mod tab means one page beyond vanilla's");

        // Nothing may share a square with a tab on the same page, which is the
        // rule vanilla enforces at bootstrap and Fenix widens rather than drops.
        require(tab.column() < 5,
                "mod tabs take columns 0 to 4; 5 and 6 belong to the tabs that follow the player");

        List<CreativeModeTab> all = BuiltInRegistries.CREATIVE_MODE_TAB.stream().toList();
        require(!CreativePages.onCurrentPage(all).contains(tab),
                "page 0 is vanilla's alone — a mod tab there would push one of vanilla's out");

        CreativePages.turn(1);
        List<CreativeModeTab> page = CreativePages.onCurrentPage(all);
        require(page.contains(tab), "page 1 should hold the mod tab");
        require(page.contains(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeTabs.SEARCH)),
                "search follows the player to every page — losing it to reach a mod's blocks "
                        + "is the whole reason paging feels bad elsewhere");
        require(page.contains(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeTabs.INVENTORY)),
                "so does the inventory");
        require(page.size() == 5, "page 1 is the mod's tab plus the four that always travel");
        CreativePages.turn(-1);
    }

    /**
     * A block entity type has to know which blocks carry it.
     *
     * <p>Get that set wrong and nothing complains: the type registers, the
     * block places, and the game silently never creates the block entity — so
     * whatever it was meant to store is simply never there.
     */
    private static void checkBlockEntity() {
        BlockEntityType<?> type =
                BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(Identifier.parse("probemod:machine"));
        require(type != null, "the block entity type should be in the registry");
        require(type == ProbeContent.MACHINE_TYPE.get(), "the handle should be bound to it");

        require(type.isValid(ProbeContent.MACHINE.get().defaultBlockState()),
                "the type should accept its own block — otherwise the block entity is never created");

        require(type.create(BlockPos.ZERO, ProbeContent.MACHINE.get().defaultBlockState()) != null,
                "the type should be able to build one");

        // A block that does not implement EntityBlock never creates its block
        // entity. That has to be refused out loud, at startup, rather than
        // discovered hours later by a player whose machine forgot everything.
        Registrar spare = Registrar.of("probemod");
        spare.blockEntity("nope", ProbeBlockEntity::new, ProbeContent.RUBY_BLOCK);
        boolean refused = false;
        try {
            spare.apply();
        } catch (IllegalArgumentException expected) {
            refused = true;
        }
        require(refused, "a block that is not an EntityBlock should be refused, loudly, "
                + "rather than never creating its block entity");
    }

    private static void checkSound() {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getValue(Identifier.parse("probemod:chime"));
        require(sound != null, "the sound event should be in the registry");
        require(sound == ProbeContent.CHIME.get(), "the handle should be bound to it");
    }

    private static void require(boolean condition, String what) {
        if (!condition) {
            throw new AssertionError("registry conformance failed: " + what);
        }
    }
}
