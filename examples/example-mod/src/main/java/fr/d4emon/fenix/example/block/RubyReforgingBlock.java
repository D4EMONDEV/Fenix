package fr.d4emon.fenix.example.block;

import fr.d4emon.fenix.example.block.entity.RubyReforgingBlockEntity;
import fr.d4emon.fenix.example.registry.ModContent;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** A reforging table: right-click to open it, and it works away on its own. */
public final class RubyReforgingBlock extends Block implements EntityBlock {

    /**
     * @param properties the properties, already carrying the block's id
     */
    public RubyReforgingBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RubyReforgingBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof RubyReforgingBlockEntity table) {
            player.openMenu(table);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        // Server only — the table does its work there, and the result is synced
        // like any other container change. A client ticker would reforge twice.
        if (level.isClientSide() || type != ModContent.RUBY_REFORGING_ENTITY.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<RubyReforgingBlockEntity>)
                RubyReforgingBlockEntity::serverTick;
    }
}
