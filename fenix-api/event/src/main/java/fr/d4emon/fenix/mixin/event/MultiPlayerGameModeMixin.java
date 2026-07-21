package fr.d4emon.fenix.mixin.event;

import fr.d4emon.fenix.event.client.ClientBlockEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires the client-side block events, so a refusal is felt immediately rather
 * than as a block breaking and snapping back when the server disagrees. Client
 * only, and never the enforcement point — see {@link ClientBlockEvents}.
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    /** Matched by Mixin from the config; not called directly. */
    public MultiPlayerGameModeMixin() {
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private void fenix$onAttack(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (ClientBlockEvents.ATTACK.fire(new ClientBlockEvents.Attack(player, pos, face)).isCancelled()) {
            // false means "not breaking", which is what vanilla returns when a
            // block cannot be attacked.
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true, remap = false)
    private void fenix$onUse(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
                             CallbackInfoReturnable<InteractionResult> cir) {
        if (ClientBlockEvents.USE.fire(new ClientBlockEvents.Use(player, hand, hit)).isCancelled()) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
