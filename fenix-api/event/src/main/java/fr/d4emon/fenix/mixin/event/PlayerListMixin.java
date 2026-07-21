package fr.d4emon.fenix.mixin.event;

import fr.d4emon.fenix.event.PlayerEvents;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Players arriving, leaving and coming back. */
@Mixin(PlayerList.class)
public class PlayerListMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    PlayerListMixin() {
    }

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void fenix$joined(Connection connection, ServerPlayer player,
                              CommonListenerCookie cookie, CallbackInfo info) {
        // At the tail: the player is in the world and their connection can
        // carry payloads, which is what a listener will want to do first.
        PlayerEvents.JOINED.fire(new PlayerEvents.Joined(player));
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void fenix$leaving(ServerPlayer player, CallbackInfo info) {
        // At the head, while the player is still readable. A moment later
        // there is no inventory and no position to look at.
        PlayerEvents.LEFT.fire(new PlayerEvents.Left(player));
    }

    @Inject(method = "respawn", at = @At("RETURN"))
    private void fenix$respawned(ServerPlayer previous, boolean endPortal,
                                 Entity.RemovalReason reason,
                                 CallbackInfoReturnable<ServerPlayer> info) {
        // The returned player, not the argument: respawning replaces the object
        // rather than resetting it, and the old one is already gone.
        PlayerEvents.RESPAWNED.fire(new PlayerEvents.Respawned(info.getReturnValue(), endPortal));
    }
}
