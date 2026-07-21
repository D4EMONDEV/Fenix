package fr.d4emon.fenix.mixin.event;

import fr.d4emon.fenix.event.BlockEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires the authoritative block events, and honours a cancellation.
 *
 * <p>This is where block protection actually works: the server decides, so a
 * modified client cannot route around it.
 */
@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {

    /** The player this game mode belongs to; supplied by the target class. */
    @Shadow
    @Final
    protected ServerPlayer player;

    /** The world the player is in; supplied by the target class. */
    @Shadow
    protected ServerLevel level;

    /** Matched by Mixin from the config; not called directly. */
    public ServerPlayerGameModeMixin() {
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private void fenix$onBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (BlockEvents.BREAK.fire(new BlockEvents.Break(player, level, pos)).isCancelled()) {
            // false means "the block was not destroyed", which is exactly what
            // vanilla returns when a break fails for its own reasons.
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true, remap = false)
    private void fenix$onUse(ServerPlayer serverPlayer, Level useLevel, ItemStack stack,
                             InteractionHand hand, BlockHitResult hit,
                             CallbackInfoReturnable<InteractionResult> cir) {
        if (BlockEvents.USE.fire(new BlockEvents.Use(serverPlayer, useLevel, hand, hit)).isCancelled()) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
