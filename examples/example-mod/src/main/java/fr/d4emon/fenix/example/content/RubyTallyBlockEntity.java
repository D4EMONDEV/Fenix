package fr.d4emon.fenix.example.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Remembers how many times its block has been used. */
public final class RubyTallyBlockEntity extends BlockEntity {

    private static final String COUNT = "count";

    private int count;

    /**
     * Built by the game, through the type registered in {@link ModContent}.
     *
     * @param pos   where the block is
     * @param state what the block is
     */
    public RubyTallyBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.RUBY_TALLY.get(), pos, state);
    }

    /**
     * Counts one more use.
     *
     * @return the new total
     */
    public int tally() {
        count++;
        // Without this the chunk is never marked dirty, so the new count is
        // correct in memory and gone after a reload.
        setChanged();
        return count;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(COUNT, count);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        count = input.getIntOr(COUNT, 0);
    }
}
