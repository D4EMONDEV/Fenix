package fr.d4emon.fenix.mixin.event;

import fr.d4emon.fenix.event.PlayerEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link PlayerEvents#PICKED_UP} when a player walks over an item.
 *
 * <p>At HEAD, so the stack is still what was lying on the ground. By the time
 * the method returns it has been merged into whatever the player already had,
 * and its count no longer says what was collected.
 */
@Mixin(ItemEntity.class)
public class ItemPickupMixin {

    /** Merged into ItemEntity; never constructed directly. */
    public ItemPickupMixin() {
    }

    @Inject(method = "playerTouch", at = @At("HEAD"), remap = false)
    private void fenix$onPickup(Player player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        // Both sides call this; only the server decides it, and firing on the
        // client would double every count a listener keeps.
        if (!player.level().isClientSide()) {
            PlayerEvents.PICKED_UP.fire(
                    new PlayerEvents.PickedUp(player, self.getItem().copy()));
        }
    }
}
