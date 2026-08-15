package fr.d4emon.fenix.mixin.network;

import fr.d4emon.fenix.network.Channels;
import fr.d4emon.fenix.network.Envelope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands an arriving envelope to the channel that asked for it, on the client
 * thread.
 *
 * <p>The injection is at HEAD, which is before vanilla's own
 * {@code ensureRunningOnSameThread} a few instructions later — so this method
 * runs on a Netty thread and the handler must not. It is scheduled onto
 * {@code Minecraft}, which is the client's event loop.
 */
@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientPayloadMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    ClientPayloadMixin() {
    }

    @Inject(method = "handleCustomPayload(Lnet/minecraft/network/protocol/common/ClientboundCustomPayloadPacket;)V",
            at = @At("HEAD"), cancellable = true)
    private void fenix$deliver(ClientboundCustomPayloadPacket packet, CallbackInfo info) {
        if (packet.payload() instanceof Envelope envelope
                && Channels.deliver(envelope, null, Envelope.TO_CLIENT,
                        Minecraft.getInstance())) {
            info.cancel();
        }
    }
}
