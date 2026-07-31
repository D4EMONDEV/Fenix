package fr.d4emon.fenix.example.content;

import fr.d4emon.fenix.event.LootEvents;
import fr.d4emon.fenix.registry.BlockInteractions;
import fr.d4emon.fenix.registry.Brewing;
import fr.d4emon.fenix.registry.CreativeTabs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import fr.d4emon.fenix.registry.Holder;
import fr.d4emon.fenix.registry.Registrar;
import fr.d4emon.fenix.registry.attachment.AttachmentType;
import fr.d4emon.fenix.registry.fluid.FluidResult;
import fr.d4emon.fenix.registry.worldgen.BiomeModifications;
import fr.d4emon.fenix.registry.worldgen.BiomeSelectors;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.levelgen.GenerationStep;

import java.util.Map;
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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

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

    /** The type behind {@link ModBlocks#RUBY_REFORGING}. */
    public static final Holder<BlockEntityType<RubyReforgingBlockEntity>> RUBY_REFORGING_ENTITY =
            REGISTRAR.blockEntity("ruby_reforging", RubyReforgingBlockEntity::new, ModBlocks.RUBY_REFORGING);

    /** The window the reforging table opens: an input slot and an output slot. */
    public static final Holder<MenuType<RubyReforgingMenu>> RUBY_REFORGING_MENU =
            REGISTRAR.menu("ruby_reforging", RubyReforgingMenu::new);

    /**
     * The reforging table's recipe type — the kind of recipe it runs.
     *
     * <p>The table finds a recipe by asking the recipe manager for this type, so
     * it and the recipe have to name the same one. This is that one.
     */
    public static final Holder<RecipeType<RubyReforgingRecipe>> REFORGING_TYPE =
            REGISTRAR.recipeType("reforging");

    /** How a reforging recipe is read from a datapack and sent to the client. */
    public static final Holder<RecipeSerializer<RubyReforgingRecipe>> REFORGING_SERIALIZER =
            REGISTRAR.recipeSerializer("reforging", new RecipeSerializer<>(
                    RubyReforgingRecipe.MAP_CODEC, RubyReforgingRecipe.STREAM_CODEC));

    /**
     * A command argument of the mod's own — see {@link ModCommands}.
     *
     * <p>The half that is easy to miss is not the registry entry but the table
     * vanilla keys by Brigadier class, which it reads while describing commands
     * to a joining player. Skip it and the command works alone and nobody can
     * connect. {@code Registrar.commandArgument} does both.
     */
    public static final Holder<ArgumentTypeInfo<?, ?>> ORE_ARGUMENT =
            REGISTRAR.commandArgument("ore", OreArgument.class,
                    SingletonArgumentInfo.contextFree(OreArgument::ore));

    /**
     * The job site a jeweller villager claims: the reforging table.
     *
     * <p>One block, two features — a machine the player uses and a point of
     * interest a villager works at. Registering the point of interest is only
     * half of it; the other half, the block-state bookkeeping, is what the
     * registrar does so the villager AI actually recognises the block.
     */
    public static final Holder<PoiType> JEWELLER_POI =
            REGISTRAR.poiType("jeweller_poi", ModBlocks.RUBY_REFORGING);

    /**
     * A villager profession that trades in rubies.
     *
     * <p>A villager takes it by claiming a reforging table nearby. Its trades
     * are datapack data — {@code data/example-mod/trade_set/jeweller.json} and
     * the {@code villager_trade} entries it names — because 26.2 made trades data
     * rather than code; the profession only points at the set, by level.
     */
    public static final Holder<VillagerProfession> JEWELLER = REGISTRAR.villagerProfession(
            "jeweller", JEWELLER_POI, SoundEvents.VILLAGER_WORK_TOOLSMITH,
            Map.of(1, REGISTRAR.tradeSet("jeweller")));

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
     * A potion carrying that effect.
     *
     * <p>Registered late by the registrar, because an effect instance holds the
     * effect itself rather than a promise of one — so the effect above has to
     * exist first. Declaring the two in this order is not a requirement; the
     * registrar arranges it.
     */
    public static final Holder<Potion> GLIMMERING_POTION =
            REGISTRAR.potion("glimmering", RUBY_GLIMMER, 20 * 45);

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

    /**
     * A fluid — a still form, a flowing form, the block it becomes, and a
     * bucket, from one call.
     *
     * <p>It reuses vanilla's water sprites tinted red rather than shipping its
     * own textures (see {@code ExampleModClient}), so it is a complete, drawn
     * fluid with no art to add.
     */
    public static final FluidResult RUBY_BRINE = REGISTRAR.newFluid("ruby_brine")
            .bucket()
            .register();

    /**
     * Every hammer swing a player has ever made, kept on the player.
     *
     * <p>The counterpart to {@link #SWINGS}, on purpose: that component lives on
     * one hammer and is gone when the hammer is, while this attachment lives on
     * the player and outlasts every hammer they wear out. Persistent, so it
     * survives logging out — the whole point of an attachment over a field.
     */
    public static final AttachmentType<Integer> TOTAL_SWINGS =
            REGISTRAR.attachment("total_swings", () -> 0, Codec.INT);

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
                ModBlocks.RUBY_BLOCK, ModBlocks.GLOWING_RUBY_BLOCK,
                ModBlocks.RUBY_LOG, ModBlocks.STRIPPED_RUBY_LOG);
        CreativeTabs.addTo(CreativeTabs.NATURAL_BLOCKS,
                ModBlocks.RUBY_ORE, ModBlocks.DEEPSLATE_RUBY_ORE);
        CreativeTabs.addTo(CreativeTabs.FUNCTIONAL_BLOCKS,
                ModBlocks.RUBY_TALLY, ModBlocks.RUBY_SAFE, ModBlocks.RUBY_REFORGING);
        ModPayloads.listen();

        // The behaviour vanilla keeps in tables rather than on the block. Each
        // of these is one line and, left out, fails without a word: a log that
        // will not burn, an axe that does nothing to it, a furnace that refuses
        // a ruby. Declared after apply() only because it reads well here — the
        // blocks are resolved when the game first asks, not now.
        BlockInteractions.flammable(ModBlocks.RUBY_LOG, 5, 5);
        BlockInteractions.strippable(ModBlocks.RUBY_LOG, ModBlocks.STRIPPED_RUBY_LOG);
        BlockInteractions.compostable(ModItems.RUBY, 0.5f);
        BlockInteractions.fuel(ModItems.RUBY, 1600);

        // A potion nothing brews into can only be given by command. Vanilla
        // builds its brewing table once per server and throws the builder away,
        // so this is recorded and handed over while that builder is still open.
        Brewing.mix(Potions.AWKWARD, ModItems.RUBY, GLIMMERING_POTION);

        // A rare ruby from ordinary stone. Added as a pool rather than by
        // overriding the file: two mods both dropping something from stone is
        // exactly what a datapack override cannot do — the second copy wins and
        // the first mod's drop is gone with nothing said.
        LootEvents.LOADING.register(loot -> {
            if (loot.id().equals(Identifier.parse("minecraft:blocks/stone"))) {
                loot.addPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.02f))
                        .add(LootItem.lootTableItem(ModItems.RUBY.get()))
                        .build());
            }
        });

        // Two files say what the ore is and where it may go; this says which
        // biomes actually want it. Without it the feature exists and is never
        // run — no biome refers to it.
        BiomeModifications.addFeature(BiomeSelectors.overworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                REGISTRAR.placedFeature("ruby_ore"));

        CreativeTabs.addTo(CreativeTabs.INGREDIENTS, ModItems.RUBY);
        CreativeTabs.addTo(CreativeTabs.TOOLS_AND_UTILITIES,
                ModItems.RUBY_HAMMER, RUBY_BRINE.bucket().orElseThrow());
        CreativeTabs.addTo(CreativeTabs.SPAWN_EGGS, RUBY_WISP_SPAWN_EGG);

        // And again in the mod's own tab, where a player looking for this mod
        // in particular will go. Content belongs in both.
        CreativeTabs.addTo(TAB, ModBlocks.RUBY_BLOCK, ModBlocks.GLOWING_RUBY_BLOCK,
                ModBlocks.RUBY_TALLY, ModBlocks.RUBY_SAFE, ModBlocks.RUBY_REFORGING,
                ModItems.RUBY, ModItems.RUBY_HAMMER, RUBY_WISP_SPAWN_EGG);
    }
}
