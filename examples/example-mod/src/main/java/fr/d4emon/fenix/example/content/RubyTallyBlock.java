package fr.d4emon.fenix.example.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A block that counts how often it has been used, to show what a block entity
 * is for: state that belongs to one placed block and survives a reload.
 */
public final class RubyTallyBlock extends Block implements EntityBlock {

    /**
     * @param properties the properties, already carrying the block's id
     */
    public RubyTallyBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RubyTallyBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                // The client cannot reset anything itself — it asks, and the
                // server decides. Sending from here is fine: only the reply
                // needs the client, and that lives in the client source set.
                ModPayloads.RESET.send(new ModPayloads.Reset(pos));
            }
            // The count lives on the server. Answering SUCCESS makes the arm
            // swing without the client guessing at a number.
            return InteractionResult.SUCCESS;
        }
        if (!player.isShiftKeyDown()
                && level.getBlockEntity(pos) instanceof RubyTallyBlockEntity tally
                && player instanceof ServerPlayer serverPlayer) {
            ModPayloads.TALLY.send(serverPlayer, new ModPayloads.Tally(pos, tally.tally()));
        }
        return InteractionResult.SUCCESS;
    }
}
