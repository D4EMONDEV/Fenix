package fr.d4emon.fenix.example.block.entity;

import fr.d4emon.fenix.example.registry.ModContent;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
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
        sync();
        return count;
    }

    /**
     * The count, without changing it.
     *
     * <p>Read by the renderer while it extracts a frame's state, which is the
     * only caller that wants to know without also wanting to add one.
     *
     * @return how many times this block has been hit
     */
    public int count() {
        return count;
    }

    /**
     * Puts the count back to zero.
     *
     * @return the new total, which is always zero
     */
    public int reset() {
        count = 0;
        sync();
        return count;
    }

    /**
     * Saves the new count, and tells the clients that can see it.
     *
     * <p>{@code setChanged} alone marks the chunk dirty so the number is
     * written to disk. It sends nothing. The client keeps the copy it was given
     * when the chunk loaded, so a renderer drawing the count draws the number
     * it had at load time for ever — while the server, and anything the server
     * prints to chat, is perfectly correct. That is what makes this the hardest
     * kind of block entity bug to place: both halves are right on their own
     * side.
     */
    private void sync() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_ALL);
        }
    }

    /**
     * What a client is given when the chunk arrives.
     *
     * <p>Without it the block entity reaches the client empty, so the count is
     * zero until the first change — and stays zero if there is never one.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    /** What a client is sent when {@link #sync} runs. */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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
