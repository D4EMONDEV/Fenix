package fr.d4emon.fenix.probe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** A block that carries a block entity, for the conformance check. */
public final class ProbeMachineBlock extends Block implements EntityBlock {

    /**
     * @param properties the properties, already carrying the block's id
     */
    public ProbeMachineBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProbeBlockEntity(pos, state);
    }
}
