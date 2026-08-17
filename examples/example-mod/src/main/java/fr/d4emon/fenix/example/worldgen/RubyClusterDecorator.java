package fr.d4emon.fenix.example.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.d4emon.fenix.example.registry.ModBlocks;
import fr.d4emon.fenix.example.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

/**
 * Hangs glowing ruby under some of a tree's leaves.
 *
 * <p>A tree's trunk and leaves are already placed when a decorator runs, and
 * the positions of both are handed to it — which is why a decorator is by far
 * the cheapest way to make a tree the mod's own. A trunk placer decides where
 * every log goes; this only decides what to hang off what is already there.
 */
public final class RubyClusterDecorator extends TreeDecorator {

    /**
     * What a tree's decorator entry may configure.
     *
     * <p>The codec produces the decorator, so this is what the registrar is
     * given — a decorator type is a codec with a name.
     */
    public static final MapCodec<RubyClusterDecorator> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.INT.fieldOf("chance").forGetter(decorator -> decorator.chance)
            ).apply(instance, RubyClusterDecorator::new));

    /** One in how many leaves get a cluster. */
    private final int chance;

    /**
     * @param chance one in how many leaves get one
     */
    public RubyClusterDecorator(int chance) {
        this.chance = chance;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        // The very object the registrar handed back, not one that is merely
        // equal to it. The game finds the name by looking this instance up in
        // the registry, so a different instance writes a decorator that cannot
        // be read again — and the tree then loads without it.
        return ModContent.RUBY_CLUSTERS.get();
    }

    @Override
    public void place(Context context) {
        for (BlockPos leaf : context.leaves()) {
            if (context.random().nextInt(chance) != 0) {
                continue;
            }
            BlockPos below = leaf.below();
            if (context.isAir(below)) {
                context.setBlock(below,
                        ModBlocks.GLOWING_RUBY_BLOCK.get().defaultBlockState());
            }
        }
    }
}
