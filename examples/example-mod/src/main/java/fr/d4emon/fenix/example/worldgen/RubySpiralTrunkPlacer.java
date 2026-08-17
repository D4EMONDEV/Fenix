package fr.d4emon.fenix.example.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.d4emon.fenix.example.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * A trunk that leans a little as it rises, one block every third log.
 *
 * <p>The heavy half of a custom tree: a trunk placer decides where every log
 * goes and reports the positions the foliage should hang from. Getting the
 * second part wrong is a tree whose leaves float beside the trunk rather than
 * on it, which renders perfectly and looks like a bug in the foliage placer.
 */
public final class RubySpiralTrunkPlacer extends TrunkPlacer {

    /**
     * Built with {@code trunkPlacerParts}, which contributes the three height
     * fields every placer shares. Writing them by hand instead is a codec that
     * parses and a placer that ignores the height the tree asked for.
     */
    public static final MapCodec<RubySpiralTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
            instance -> trunkPlacerParts(instance).apply(instance, RubySpiralTrunkPlacer::new));

    /**
     * @param baseHeight  the shortest it can be
     * @param heightRandA one random part
     * @param heightRandB the other
     */
    public RubySpiralTrunkPlacer(int baseHeight, int heightRandA, int heightRandB) {
        super(baseHeight, heightRandA, heightRandB);
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return ModContent.RUBY_SPIRAL.get();
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(
            WorldGenLevel level, BiConsumer<BlockPos, BlockState> blocks,
            RandomSource random, int height, BlockPos origin, TreeConfiguration config) {
        // placeBelowTrunkBlock, not setDirtAt: the helper is named for what it
        // does rather than for dirt, because a tree may sit on anything the
        // configuration names.
        placeBelowTrunkBlock(level, blocks, random, origin.below(), config);

        BlockPos at = origin;
        for (int y = 0; y < height; y++) {
            placeLog(level, blocks, random, at.above(y), config);
            if (y % 3 == 2) {
                // The lean. Shifting the column rather than adding to it keeps
                // the trunk one block thick, which is what the foliage placer
                // expects to find under its leaves.
                at = at.east();
            }
        }
        List<FoliagePlacer.FoliageAttachment> attachments = new ArrayList<>();
        attachments.add(new FoliagePlacer.FoliageAttachment(at.above(height), 0, false));
        return attachments;
    }
}
