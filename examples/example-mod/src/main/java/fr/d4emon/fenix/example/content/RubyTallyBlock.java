package fr.d4emon.fenix.example.content;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
            // The count lives on the server. Answering SUCCESS here is what
            // makes the arm swing without the client guessing at a number.
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof RubyTallyBlockEntity tally
                && player instanceof ServerPlayer serverPlayer) {
            // true: shown above the hotbar rather than in chat, which is where
            // a running count belongs.
            serverPlayer.sendSystemMessage(
                    Component.literal("Used " + tally.tally() + " time(s)"), true);
        }
        return InteractionResult.SUCCESS;
    }
}
