package fr.d4emon.fenix.probe;

import fr.d4emon.fenix.registry.Holder;
import fr.d4emon.fenix.registry.Registrar;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

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

    private ProbeContent() {
    }
}
