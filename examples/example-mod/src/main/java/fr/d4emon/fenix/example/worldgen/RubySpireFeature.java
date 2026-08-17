package fr.d4emon.fenix.example.worldgen;

import fr.d4emon.fenix.example.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A spire of ruby, tapering as it rises, with a glowing block at the tip.
 *
 * <p>Everything else the demo adds to the world is one of vanilla's features
 * configured differently — the ore vein is {@code minecraft:ore} with a ruby in
 * it. This is the other kind: a shape nothing in vanilla describes, built by
 * code with the level in hand.
 */
public final class RubySpireFeature extends Feature<NoneFeatureConfiguration> {

    /** How tall a spire can be, before the random source narrows it down. */
    private static final int MAX_HEIGHT = 9;

    /** Takes no configuration, so the codec is the empty one. */
    public RubySpireFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        // Refusing on air is not a nicety. A feature reports whether it built
        // anything, and one that claims success without building is counted
        // against the biome's budget for features that did.
        if (level.isEmptyBlock(origin.below())) {
            return false;
        }

        int height = 4 + random.nextInt(MAX_HEIGHT - 4);
        for (int y = 0; y < height; y++) {
            // The base is three wide and the tip is one, so the radius falls
            // as the spire rises.
            int radius = (height - y) / 4;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z > radius * radius) {
                        continue;
                    }
                    level.setBlock(origin.offset(x, y, z),
                            ModBlocks.RUBY_BLOCK.get().defaultBlockState(), 2);
                }
            }
        }
        level.setBlock(origin.above(height),
                ModBlocks.GLOWING_RUBY_BLOCK.get().defaultBlockState(), 2);
        return true;
    }
}
