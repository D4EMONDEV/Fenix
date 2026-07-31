package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.BlockInteractions;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a mod's blocks burn.
 *
 * <p>Fire keeps two tables of odds, filled once in {@code FireBlock.bootStrap}
 * from a list of vanilla's own blocks. A block missing from them has odds of
 * zero, which is not an error: it simply never catches and never burns away. So
 * modded planks sit in a forest fire untouched, and nothing anywhere says why.
 *
 * <p>Consulted before vanilla's tables rather than added to them, so that a mod
 * declaring flammability before its blocks are registered still works — and so
 * that Fenix never has to guess when bootstrap has finished.
 */
@Mixin(FireBlock.class)
public class FireBlockMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    FireBlockMixin() {
    }

    @Inject(method = "getBurnOdds", at = @At("HEAD"), cancellable = true)
    private void fenix$modBurnOdds(BlockState state, CallbackInfoReturnable<Integer> info) {
        odds(state, info, false);
    }

    @Inject(method = "getIgniteOdds", at = @At("HEAD"), cancellable = true)
    private void fenix$modIgniteOdds(BlockState state, CallbackInfoReturnable<Integer> info) {
        odds(state, info, true);
    }

    private static void odds(BlockState state, CallbackInfoReturnable<Integer> info, boolean ignite) {
        BlockInteractions.Flammability declared =
                BlockInteractions.flammabilityOf(state.getBlock());
        if (declared == null) {
            return;
        }
        // Waterlogged blocks do not burn, which vanilla checks before reading its
        // own table. Answering ahead of it means answering that too, or a modded
        // waterlogged block would burn where a vanilla one would not.
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)
                && state.getValue(BlockStateProperties.WATERLOGGED)) {
            info.setReturnValue(0);
            return;
        }
        info.setReturnValue(ignite ? declared.igniteOdds() : declared.burnOdds());
    }
}
