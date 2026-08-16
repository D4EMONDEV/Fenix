package fr.d4emon.fenix.probe;

import fr.d4emon.fenix.registry.BlockInteractions;
import fr.d4emon.fenix.registry.Brewing;
import fr.d4emon.fenix.registry.Holder;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import fr.d4emon.fenix.registry.Registrar;
import fr.d4emon.fenix.registry.attachment.AttachmentType;
import net.minecraft.world.level.block.RotatedPillarBlock;
import fr.d4emon.fenix.registry.fluid.FluidResult;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Map;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

/** The content the registry conformance check registers. */
public final class ProbeContent {

    public static final Registrar REGISTRAR = Registrar.of("probemod");

    public static final Holder<Block> RUBY_BLOCK = REGISTRAR.newBlock("ruby_block")
            .strength(3f)
            .requiresTool()
            .withItem()
            .register();

    public static final Holder<Item> RUBY = REGISTRAR.newItem("ruby").register();

    /**
     * Two rules of the mod's own, one of each type Fenix can register.
     *
     * <p>Registered eagerly rather than deferred, because vanilla's rule
     * registry is built during bootstrap and frozen with everything else.
     */
    public static final GameRule<Boolean> PROBE_FLAG =
            REGISTRAR.gameRule("probe_flag", GameRuleCategory.MISC, false);

    public static final GameRule<Integer> PROBE_LIMIT =
            REGISTRAR.gameRule("probe_limit", GameRuleCategory.MISC, 7, 0, 64);

    /**
     * A tab of the mod's own. Registering one is enough to make vanilla's
     * bootstrap validation throw, so this field alone is half the check.
     */
    public static final ResourceKey<CreativeModeTab> TAB =
            REGISTRAR.creativeTab("probemod", RUBY);

    /**
     * A block set type of the mod's own, which needs a private vanilla method
     * widened to register at all.
     */
    public static final BlockSetType PROBE_SET = REGISTRAR.blockSetType(
            "probe", true, SoundType.METAL,
            SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN,
            SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN,
            SoundEvents.STONE_PRESSURE_PLATE_CLICK_OFF,
            SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON,
            SoundEvents.STONE_BUTTON_CLICK_OFF, SoundEvents.STONE_BUTTON_CLICK_ON);

    /**
     * The three registrations a brain-driven mob needs, and two smaller ones.
     *
     * <p>Two of the five are built from constructors the game keeps private,
     * so registering them at all is the check: it proves the manifest's
     * {@code accessible} entries reached the running game and not only the
     * compiler.
     */
    public static final Holder<MemoryModuleType<Integer>> PROBE_MEMORY =
            REGISTRAR.memoryModule("probe_memory", Codec.INT);

    public static final Holder<SensorType<ProbeSensor>> PROBE_SENSOR =
            REGISTRAR.sensor("probe_sensor", ProbeSensor::new);

    public static final Holder<Activity> PROBE_ACTIVITY = REGISTRAR.activity("probe_activity");

    public static final Holder<GameEvent> PROBE_EVENT = REGISTRAR.gameEvent("probe_event", 16);

    public static final Holder<DecoratedPotPattern> PROBE_SHERD =
            REGISTRAR.decoratedPotPattern("probe_sherd");

    /**
     * A loot condition of the mod's own.
     *
     * <p>The registry holds the codec, and the condition class points back at
     * the same one — which is the whole mechanism by which a name in a loot
     * table finds its way to a class.
     */
    public static final Holder<MapCodec<? extends LootItemCondition>> PROBE_CONDITION =
            REGISTRAR.lootCondition("probe_condition", ProbeLootCondition.CODEC);

    /**
     * An advancement trigger of the mod's own.
     *
     * <p>Registered eagerly: triggers are consulted while advancements load,
     * which happens before deferred content is bound.
     */
    public static final ProbeTrigger PROBE_TRIGGER =
            REGISTRAR.trigger("probe_trigger", new ProbeTrigger());

    /** A block that carries a block entity. */
    public static final Holder<Block> MACHINE =
            REGISTRAR.block("machine", ProbeMachineBlock::new);

    /**
     * The type behind it.
     *
     * <p>Registered in a pass of its own after every block exists, which is
     * what lets a mod declare the two in whichever order reads best.
     */
    public static final Holder<BlockEntityType<ProbeBlockEntity>> MACHINE_TYPE =
            REGISTRAR.blockEntity("machine", ProbeBlockEntity::new, MACHINE);

    /** A living entity, which needs attributes or it dies while being built. */
    public static final Holder<EntityType<ProbeCritter>> CRITTER = REGISTRAR.entity(
            "critter", ProbeCritter::new, MobCategory.CREATURE, builder -> builder.sized(0.6f, 0.9f));

    /**
     * A menu type.
     *
     * <p>Registering one at all is the check: {@code MenuType}'s constructor
     * and the interface it takes are both private in vanilla, so this line only
     * runs if the loader really widened them in the jar the game is using.
     */
    public static final Holder<MenuType<ProbeMenu>> CHEST_MENU =
            REGISTRAR.menu("chest", ProbeMenu::new);

    /** A spawn egg, which is an item whose entity travels as a component. */
    public static final Holder<Item> CRITTER_EGG = REGISTRAR.spawnEgg("critter_spawn_egg", CRITTER);

    /** A particle, whose type vanilla will not let a mod construct unwidened. */
    public static final Holder<SimpleParticleType> SPARK = REGISTRAR.particle("spark");

    /** A status effect. */
    public static final Holder<ProbeEffect> GLIMMER = REGISTRAR.effect("glimmer", new ProbeEffect());

    /** A data component, the way 26.x carries state on a stack. */
    public static final Holder<DataComponentType<Integer>> CHARGE =
            REGISTRAR.dataComponent("charge", builder -> builder.persistent(Codec.INT));

    /** A sound event, which is half of a sound; sounds.json is the other half. */
    public static final Holder<SoundEvent> CHIME = REGISTRAR.sound("chime");

    /**
     * A fluid — four registrations in one: a still form, a flowing form, the
     * block it becomes, and the bucket that carries it.
     */
    public static final FluidResult ACID = REGISTRAR.newFluid("acid")
            .bucket()
            .tickDelay(10)
            .register();

    /** A persistent attachment: an integer that has to survive save and load. */
    public static final AttachmentType<Integer> MANA =
            REGISTRAR.attachment("mana", () -> 0, Codec.INT);

    /** A transient attachment: a flag that must never be written to disk. */
    public static final AttachmentType<Boolean> WARMED =
            REGISTRAR.attachment("warmed", () -> false);

    /** A recipe type of the mod's own — the kind a custom station runs. */
    public static final Holder<RecipeType<ProbeReforgingRecipe>> REFORGING_TYPE =
            REGISTRAR.recipeType("reforging");

    /** How that recipe is read from a datapack and sent over the wire. */
    public static final Holder<RecipeSerializer<ProbeReforgingRecipe>> REFORGING_SERIALIZER =
            REGISTRAR.recipeSerializer("reforging", new RecipeSerializer<>(
                    ProbeReforgingRecipe.MAP_CODEC, ProbeReforgingRecipe.STREAM_CODEC));

    /** A job-site point of interest, on the ruby block, for a profession to claim. */
    public static final Holder<PoiType> RUBY_STALL_POI = REGISTRAR.poiType("ruby_stall", RUBY_BLOCK);

    /** A villager profession, tied to the job site above. */
    public static final Holder<VillagerProfession> JEWELLER = REGISTRAR.villagerProfession(
            "jeweller", RUBY_STALL_POI, null, Map.of(1, REGISTRAR.tradeSet("jeweller_novice")));

    /** A log-shaped block, so an axe has something to strip. */
    public static final Holder<Block> RUBY_LOG =
            REGISTRAR.block("ruby_log", RotatedPillarBlock::new);

    /** What it strips into. */
    public static final Holder<Block> STRIPPED_RUBY_LOG =
            REGISTRAR.block("stripped_ruby_log", RotatedPillarBlock::new);

    /**
     * A potion of the mod's own, which nothing brews into until Brewing says so.
     *
     * <p>Named against the effect above by handle, so this line runs before that
     * effect is registered and still works.
     */
    public static final Holder<Potion> GLIMMERING =
            REGISTRAR.potion("glimmering_potion", GLIMMER, 600);

    /**
     * A command argument of the mod's own.
     *
     * <p>Half of registering one is a table keyed by class that vanilla reads
     * while describing commands to a joining player — see the probe.
     */
    public static final Holder<ArgumentTypeInfo<?, ?>> ORE_ARGUMENT =
            REGISTRAR.commandArgument("ore", ProbeArgument.class,
                    SingletonArgumentInfo.contextFree(ProbeArgument::ore));

    /** A block that waxes, and oxidises. */
    public static final Holder<Block> RUBY_COPPER = REGISTRAR.block("ruby_copper");

    /** Its waxed form. */
    public static final Holder<Block> WAXED_RUBY_COPPER = REGISTRAR.block("waxed_ruby_copper");

    /** The stage it weathers into. */
    public static final Holder<Block> EXPOSED_RUBY_COPPER = REGISTRAR.block("exposed_ruby_copper");

    static {
        // The tables vanilla fills once at bootstrap and a mod is never in.
        BlockInteractions.flammable(RUBY_LOG, 5, 5);
        BlockInteractions.compostable(RUBY, 0.5f);
        BlockInteractions.strippable(RUBY_LOG, STRIPPED_RUBY_LOG);
        BlockInteractions.waxable(RUBY_COPPER, WAXED_RUBY_COPPER);
        BlockInteractions.oxidation(RUBY_COPPER, EXPOSED_RUBY_COPPER);
        BlockInteractions.fuel(RUBY, 1600);

        // A potion nothing brews into can only be given by command. Declared
        // before the potion is registered on purpose: the builder resolves it,
        // not this line.
        Brewing.mix(Potions.AWKWARD, RUBY, GLIMMERING);
    }

    static {
        // Not optional: a LivingEntity asks vanilla for its attributes while it
        // is being constructed, and one that is missing dies there.
        REGISTRAR.attributes(CRITTER, () -> Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 8)
                .add(Attributes.MOVEMENT_SPEED, 0.25));

        // Without this the critter can be summoned and hatched from its egg,
        // and never appears on its own anywhere.
        REGISTRAR.spawnRule(CRITTER, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
    }

    private ProbeContent() {
    }
}
