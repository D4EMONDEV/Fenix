package fr.d4emon.fenix.probe;

import fr.d4emon.fenix.registry.Holder;
import fr.d4emon.fenix.registry.Registrar;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
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

    /** A sound event, which is half of a sound; sounds.json is the other half. */
    public static final Holder<SoundEvent> CHIME = REGISTRAR.sound("chime");

    private ProbeContent() {
    }
}
