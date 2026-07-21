package fr.d4emon.fenix.mixin.network;

import fr.d4emon.fenix.network.Channels;
import fr.d4emon.fenix.network.Envelope;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands an arriving envelope to the channel that asked for it.
 *
 * <p>On the game listener rather than the common one, because that is where
 * there is a player to hand the handler — and a payload from nobody in
 * particular is not something a mod can answer sensibly.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPayloadMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    ServerPayloadMixin() {
    }

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void fenix$deliver(ServerboundCustomPayloadPacket packet, CallbackInfo info) {
        if (!(packet.payload() instanceof Envelope envelope)) {
            return;
        }
        ServerPlayer player = ((ServerGamePacketListenerImpl) (Object) this).getPlayer();
        // Already on the server thread: vanilla schedules packet handling there
        // before this runs, so a handler can touch the world directly.
        if (Channels.deliver(envelope, player, Envelope.TO_SERVER)) {
            info.cancel();
        }
    }
}
