package fr.d4emon.fenix.example.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A safe: right-click it and it opens. */
public final class RubySafeBlock extends Block implements EntityBlock {

    /**
     * @param properties the properties, already carrying the block's id
     */
    public RubySafeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RubySafeBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            // The window is opened by the server, which then tells this client
            // about it. Opening one here would show contents nobody else has.
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof RubySafeBlockEntity safe) {
            // The block entity is already a MenuProvider, so there is nothing
            // to wrap: it knows its own name and how to build its menu.
            player.openMenu(safe);
        }
        return InteractionResult.SUCCESS;
    }
}
