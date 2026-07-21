package fr.d4emon.fenix.probe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The block entity the conformance check registers. */
public final class ProbeBlockEntity extends BlockEntity {

    /**
     * @param pos   where the block is
     * @param state what the block is
     */
    public ProbeBlockEntity(BlockPos pos, BlockState state) {
        super(ProbeContent.MACHINE_TYPE.get(), pos, state);
    }
}
