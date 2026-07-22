package fr.d4emon.fenix.probe;

import fr.d4emon.fenix.registry.CreativePages;
import fr.d4emon.fenix.registry.CreativeTabs;
import fr.d4emon.fenix.registry.Registrar;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;
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
        checkEntity();
        checkMenu();
        checkSpawning();
        checkSmallRegistries();

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

    /**
     * A living entity has to have attributes.
     *
     * <p>LivingEntity's constructor asks vanilla's table for them, and an
     * entity that is not in it dies right there with a null map — inside
     * vanilla, nowhere near the mod that registered it. So the check is not
     * that the table contains an entry but that one can actually be built.
     */
    private static void checkEntity() {
        EntityType<?> type =
                BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("probemod:critter"));
        require(type != null, "the entity type should be in the registry");
        require(type == ProbeContent.CRITTER.get(), "the handle should be bound to it");

        require(DefaultAttributes.hasSupplier(type),
                "a living entity without attributes cannot be constructed at all");
        require(DefaultAttributes.getSupplier(ProbeContent.CRITTER.get())
                        .getValue(Attributes.MAX_HEALTH) == 8,
                "the attributes registered should be the ones asked for");
    }

    /**
     * A menu type has to be registered, and reaching this far is most of it.
     *
     * <p>{@code MenuType}'s constructor is private and so is the interface it
     * takes, so a mod cannot build one — Fenix widens both, in the jar the game
     * actually loads. If that transformation ever stops firing, the failure is
     * an {@code IllegalAccessError} thrown out of the mod's field initialiser
     * during bootstrap, which is exactly the kind of thing that is easy to
     * break and impossible to notice until a player opens a chest.
     */
    private static void checkMenu() {
        MenuType<?> type = BuiltInRegistries.MENU.getValue(Identifier.parse("probemod:chest"));
        require(type != null, "the menu type should be in the registry");
        require(type == ProbeContent.CHEST_MENU.get(), "the handle should be bound to it");
    }

    /**
     * A spawn egg has to name its entity, and a mob has to have a placement.
     *
     * <p>Both fail quietly. An egg whose component is missing spawns nothing
     * when right-clicked; a mob with no placement can be summoned and hatched
     * and simply never appears in the world, which reads as a wrong spawn
     * weight rather than as a missing registration.
     */
    private static void checkSpawning() {
        Item egg = BuiltInRegistries.ITEM.getValue(Identifier.parse("probemod:critter_spawn_egg"));
        require(egg != null, "the spawn egg should be in the registry");
        require(egg == ProbeContent.CRITTER_EGG.get(), "the handle should be bound to it");
        require(egg instanceof SpawnEggItem, "and it should be a spawn egg");

        // Getting this far is the check that matters. An egg names its entity
        // by holding the type itself, not a promise of one, so it can only be
        // built once the entity exists — and the registrar's late pass is what
        // arranges that. Declared the other way round and without it, this line
        // is never reached: apply() throws while the handle is still unbound.
        //
        // What the stack actually carries cannot be read here. Components are
        // bound while the game loads datapacks, and a probe stops long before
        // that: until then vanilla's own Items.STONE cannot be made into a
        // stack either.

        // Vanilla answers NO_RESTRICTIONS for a type it has never heard of, so
        // asking for the placement is not enough — it has to be the one asked
        // for.
        require(SpawnPlacements.getPlacementType(ProbeContent.CRITTER.get())
                        == SpawnPlacementTypes.ON_GROUND,
                "the placement registered should be the one asked for");
        require(SpawnPlacements.getHeightmapType(ProbeContent.CRITTER.get())
                        == Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                "and so should the heightmap");
    }

    /** The registries that are one line each, and silent when missed. */
    private static void checkSmallRegistries() {
        require(BuiltInRegistries.PARTICLE_TYPE.getValue(Identifier.parse("probemod:spark"))
                        == ProbeContent.SPARK.get(),
                "the particle type should be in the registry, bound to its handle");

        require(BuiltInRegistries.MOB_EFFECT.getValue(Identifier.parse("probemod:glimmer"))
                        == ProbeContent.GLIMMER.get(),
                "the status effect should be in the registry, bound to its handle");

        DataComponentType<Integer> charge = ProbeContent.CHARGE.get();
        require(BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse("probemod:charge"))
                        == charge,
                "the data component type should be in the registry, bound to its handle");

        // A component registered without a codec cannot be saved, and that is
        // invisible until a world is reloaded and the state is simply gone.
        require(charge.codec() != null, "a persistent component should have a codec, or it never saves");
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
