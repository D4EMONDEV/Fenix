package fr.d4emon.fenix.probe;

import fr.d4emon.fenix.registry.Holder;
import fr.d4emon.fenix.registry.Registrar;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.mojang.serialization.Codec;
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
     * A tab of the mod's own. Registering one is enough to make vanilla's
     * bootstrap validation throw, so this field alone is half the check.
     */
    public static final ResourceKey<CreativeModeTab> TAB =
            REGISTRAR.creativeTab("probemod", RUBY);

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
