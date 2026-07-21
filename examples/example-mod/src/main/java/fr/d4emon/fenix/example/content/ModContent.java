package fr.d4emon.fenix.example.content;

import fr.d4emon.fenix.registry.CreativeTabs;
import fr.d4emon.fenix.registry.Holder;
import fr.d4emon.fenix.registry.Registrar;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

/**
 * The mod's one registrar, shared by {@link ModBlocks} and {@link ModItems}.
 *
 * <p>Keeping it here rather than in either of them means neither has to know
 * about the other, and {@link #register()} is a single call the mod class makes
 * without caring how the content is split up.
 */
public final class ModContent {

    /** Everything this mod adds is namespaced under its id. */
    public static final Registrar REGISTRAR = Registrar.of("example-mod");

    /**
     * A tab of the mod's own, on its own page of the creative menu.
     *
     * <p>Declared here rather than in {@link ModBlocks} or {@link ModItems}
     * because both put content in it.
     */
    public static final ResourceKey<CreativeModeTab> TAB =
            REGISTRAR.creativeTab("example_mod", ModItems.RUBY);

    /**
     * The type behind {@link ModBlocks#RUBY_TALLY}.
     *
     * <p>Declared here even though the block is in {@link ModBlocks}: the order
     * does not matter, because block entity types are registered in a pass of
     * their own once every block exists.
     */
    public static final Holder<BlockEntityType<RubyTallyBlockEntity>> RUBY_TALLY =
            REGISTRAR.blockEntity("ruby_tally", RubyTallyBlockEntity::new, ModBlocks.RUBY_TALLY);

    /** A drifting mote, to show an entity registered and drawn. */
    public static final Holder<EntityType<RubyWisp>> RUBY_WISP = REGISTRAR.entity(
            "ruby_wisp", RubyWisp::new, MobCategory.MISC, builder -> builder.sized(0.25f, 0.25f));

    private ModContent() {
    }

    /**
     * Registers everything. Called from {@code onRegister}.
     *
     * <p>Touching both classes is what runs their field initialisers — content
     * declared in a class nobody loads is content that never appears.
     */
    public static void register() {
        ModBlocks.load();
        ModItems.load();
        REGISTRAR.apply();

        // Without this the content exists but is unreachable in game except
        // through /give.
        CreativeTabs.addTo(CreativeTabs.BUILDING_BLOCKS,
                ModBlocks.RUBY_BLOCK, ModBlocks.GLOWING_RUBY_BLOCK);
        CreativeTabs.addTo(CreativeTabs.FUNCTIONAL_BLOCKS, ModBlocks.RUBY_TALLY);
        CreativeTabs.addTo(CreativeTabs.INGREDIENTS, ModItems.RUBY);
        CreativeTabs.addTo(CreativeTabs.TOOLS_AND_UTILITIES, ModItems.RUBY_HAMMER);

        // And again in the mod's own tab, where a player looking for this mod
        // in particular will go. Content belongs in both.
        CreativeTabs.addTo(TAB, ModBlocks.RUBY_BLOCK, ModBlocks.GLOWING_RUBY_BLOCK,
                ModBlocks.RUBY_TALLY, ModItems.RUBY, ModItems.RUBY_HAMMER);
    }
}
