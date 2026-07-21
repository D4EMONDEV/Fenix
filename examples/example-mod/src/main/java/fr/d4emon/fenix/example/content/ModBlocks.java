package fr.d4emon.fenix.example.content;

import fr.d4emon.fenix.registry.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/**
 * The mod's blocks.
 *
 * <p>Each is a {@code static final} field describing what it is; the actual
 * registration happens later, when {@link ModContent#register()} runs. That is
 * why the type is {@link Holder} rather than {@link Block}: the block does not
 * exist yet at the moment this class is initialised.
 */
public final class ModBlocks {

    /** A decorative ore-style block, as hard as iron and needing a pickaxe. */
    public static final Holder<Block> RUBY_BLOCK = ModContent.REGISTRAR.newBlock("ruby_block")
            .strength(3f, 6f)
            .requiresTool()
            .sound(SoundType.METAL)
            .withItem()
            .register();

    /** The same, but glowing — enough to show a second block works. */
    public static final Holder<Block> GLOWING_RUBY_BLOCK = ModContent.REGISTRAR.newBlock("glowing_ruby_block")
            .strength(3f, 6f)
            .requiresTool()
            .lightLevel(10)
            .sound(SoundType.METAL)
            .withItem()
            .register();

    private ModBlocks() {
    }

    /** Loads this class, which is what runs the declarations above. */
    static void load() {
    }
}
