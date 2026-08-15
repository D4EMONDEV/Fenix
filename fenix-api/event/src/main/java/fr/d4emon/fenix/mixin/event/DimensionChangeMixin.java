package fr.d4emon.fenix.mixin.event;

import fr.d4emon.fenix.event.PlayerEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link PlayerEvents#CHANGED_DIMENSION} once a player has arrived.
 *
 * <p>On the private method the game calls to award the advancements for
 * travelling, which is the one place that knows both ends of the trip: the
 * player is already in the new dimension and the old one is still in hand.
 *
 * <p>Injecting on {@code teleport} instead would fire for every teleport within
 * a dimension as well, and would have to work out whether the world actually
 * changed — which is the sort of guess that is right until somebody teleports
 * to the same coordinates in the same world.
 */
@Mixin(ServerPlayer.class)
public class DimensionChangeMixin {

    /** Merged into ServerPlayer; never constructed directly. */
    public DimensionChangeMixin() {
    }

    @Inject(method = "triggerDimensionChangeTriggers", at = @At("HEAD"), remap = false)
    private void fenix$onChangedDimension(ServerLevel oldLevel, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        PlayerEvents.CHANGED_DIMENSION.fire(new PlayerEvents.ChangedDimension(
                self, oldLevel.dimension(), self.level().dimension()));
    }
}
