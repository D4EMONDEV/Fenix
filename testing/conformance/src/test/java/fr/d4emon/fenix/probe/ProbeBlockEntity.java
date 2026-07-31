package fr.d4emon.fenix.probe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The block entity the conformance check registers. */
public final class ProbeBlockEntity extends BlockEntity {

    /**
     * @param pos   where the block is
     * @param state what the block is
     */
    public ProbeBlockEntity(BlockPos pos, BlockState state) {
        super(ProbeContent.MACHINE_TYPE.get(), pos, state);
    }

    /**
     * Runs the block entity's own save path, which the attachment mixin injects
     * into. Public so the probe can drive it; it does not override
     * {@code saveAdditional}, so this reaches the merged vanilla method the
     * mixin actually touched.
     *
     * @param output where to write
     */
    public void probeSave(ValueOutput output) {
        saveAdditional(output);
    }

    /**
     * The load half of {@link #probeSave}.
     *
     * @param input where to read from
     */
    public void probeLoad(ValueInput input) {
        loadAdditional(input);
    }
}
