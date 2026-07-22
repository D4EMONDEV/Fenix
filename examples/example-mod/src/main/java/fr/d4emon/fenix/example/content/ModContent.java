package fr.d4emon.fenix.example.content;

import fr.d4emon.fenix.registry.CreativeTabs;
import fr.d4emon.fenix.registry.Holder;
import fr.d4emon.fenix.registry.Registrar;
import fr.d4emon.fenix.registry.worldgen.BiomeModifications;
import fr.d4emon.fenix.registry.worldgen.BiomeSelectors;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.ResourceKey;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
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

    /** The type behind {@link ModBlocks#RUBY_SAFE}. */
    public static final Holder<BlockEntityType<RubySafeBlockEntity>> RUBY_SAFE_ENTITY =
            REGISTRAR.blockEntity("ruby_safe", RubySafeBlockEntity::new, ModBlocks.RUBY_SAFE);

    /**
     * The window the safe opens.
     *
     * <p>The factory here is the client's: the server builds its menu from the
     * block entity, which already knows what it holds, while the client only
     * learns that a window of this type opened and builds an empty one to fill.
     */
    public static final Holder<MenuType<RubySafeMenu>> RUBY_SAFE_MENU =
            REGISTRAR.menu("ruby_safe", RubySafeMenu::new);

    /**
     * Hatches a wisp, so the entity can be met without a command.
     *
     * <p>Beside the entity rather than in {@link ModItems}, and not by taste:
     * this class initialises that one — {@link #TAB} names a ruby as its icon —
     * so a field there reading {@code ModContent.RUBY_WISP} would read it while
     * this class is still half-initialised, and get null. Java allows the cycle
     * and says nothing; the registrar reports a null entity, from a line that
     * looks correct.
     */
    public static final Holder<Item> RUBY_WISP_SPAWN_EGG =
            REGISTRAR.spawnEgg("ruby_wisp_spawn_egg", RUBY_WISP);

    /**
     * Sparks, drawn when the safe is opened.
     *
     * <p>Registering the type is the half both sides need. The client also has
     * to say what it looks like — {@code ParticleRendering} in the client half
     * — and the textures come from {@code particles/ruby_spark.json}.
     */
    public static final Holder<SimpleParticleType> RUBY_SPARK = REGISTRAR.particle("ruby_spark");

    /** A status effect, which is a class of the mod's own plus this line. */
    public static final Holder<RubyGlimmerEffect> RUBY_GLIMMER =
            REGISTRAR.effect("ruby_glimmer", new RubyGlimmerEffect());

    /**
     * How many times a hammer has been swung.
     *
     * <p>Persistent so it survives saving, and network-synchronised so the
     * client can put it in the tooltip. A component with neither would last
     * until the stack was next looked at.
     */
    public static final Holder<DataComponentType<Integer>> SWINGS =
            REGISTRAR.dataComponent("swings", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

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
        CreativeTabs.addTo(CreativeTabs.NATURAL_BLOCKS,
                ModBlocks.RUBY_ORE, ModBlocks.DEEPSLATE_RUBY_ORE);
        CreativeTabs.addTo(CreativeTabs.FUNCTIONAL_BLOCKS, ModBlocks.RUBY_TALLY, ModBlocks.RUBY_SAFE);
        ModPayloads.listen();

        // Two files say what the ore is and where it may go; this says which
        // biomes actually want it. Without it the feature exists and is never
        // run — no biome refers to it.
        BiomeModifications.addFeature(BiomeSelectors.overworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                REGISTRAR.placedFeature("ruby_ore"));

        CreativeTabs.addTo(CreativeTabs.INGREDIENTS, ModItems.RUBY);
        CreativeTabs.addTo(CreativeTabs.TOOLS_AND_UTILITIES, ModItems.RUBY_HAMMER);
        CreativeTabs.addTo(CreativeTabs.SPAWN_EGGS, RUBY_WISP_SPAWN_EGG);

        // And again in the mod's own tab, where a player looking for this mod
        // in particular will go. Content belongs in both.
        CreativeTabs.addTo(TAB, ModBlocks.RUBY_BLOCK, ModBlocks.GLOWING_RUBY_BLOCK,
                ModBlocks.RUBY_TALLY, ModBlocks.RUBY_SAFE, ModItems.RUBY, ModItems.RUBY_HAMMER,
                RUBY_WISP_SPAWN_EGG);
    }
}
