package fr.d4emon.fenix.mixin.event;

import fr.d4emon.fenix.event.EntityEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires {@link EntityEvents#INTERACT} when a player right-clicks an entity.
 *
 * <p>On {@code Player} rather than on the packet handler, so the client's own
 * prediction fires it too. A listener that only ever saw the server would let
 * the client open a screen the server then refuses, which reads as the screen
 * flickering shut.
 */
@Mixin(Player.class)
public class PlayerInteractMixin {

    /** Merged into Player; never constructed directly. */
    public PlayerInteractMixin() {
    }

    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true, remap = false)
    private void fenix$onInteract(Entity target, InteractionHand hand, Vec3 hit,
                                  CallbackInfoReturnable<InteractionResult> cir) {
        Player self = (Player) (Object) this;
        if (EntityEvents.INTERACT.fire(new EntityEvents.Interact(self, target, hand))
                .isCancelled()) {
            // FAIL, not PASS: PASS lets the game carry on and try the item in
            // hand instead, which is the opposite of cancelling.
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
