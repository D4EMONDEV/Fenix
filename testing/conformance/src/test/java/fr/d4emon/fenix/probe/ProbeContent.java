package fr.d4emon.fenix.probe;

import fr.d4emon.fenix.registry.Holder;
import fr.d4emon.fenix.registry.Registrar;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
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

    /** A sound event, which is half of a sound; sounds.json is the other half. */
    public static final Holder<SoundEvent> CHIME = REGISTRAR.sound("chime");

    static {
        // Not optional: a LivingEntity asks vanilla for its attributes while it
        // is being constructed, and one that is missing dies there.
        REGISTRAR.attributes(CRITTER, () -> Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 8)
                .add(Attributes.MOVEMENT_SPEED, 0.25));
    }

    private ProbeContent() {
    }
}
