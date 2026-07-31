package fr.d4emon.fenix.mixin.event;

import fr.d4emon.fenix.event.client.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link ClientEvents#CONNECTED} and {@link ClientEvents#DISCONNECTED}.
 *
 * <p>Joining is taken at the tail of the login handler rather than at its head:
 * that method is what builds the level and the player, so firing before it would
 * hand a listener a world that does not exist yet.
 *
 * <p>Leaving is taken at {@code close}, which runs for every way out — quitting
 * to the menu, being kicked, the connection dropping. Hooking the polite exit
 * alone would leave a mod's per-world state behind on exactly the occasions it
 * matters most.
 */
@Mixin(ClientPacketListener.class)
public class ClientConnectionMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    ClientConnectionMixin() {
    }

    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void fenix$connected(ClientboundLoginPacket packet, CallbackInfo info) {
        ClientEvents.CONNECTED.fire(new ClientEvents.Connected(
                Minecraft.getInstance(), (ClientPacketListener) (Object) this));
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void fenix$disconnected(CallbackInfo info) {
        ClientEvents.DISCONNECTED.fire(new ClientEvents.Disconnected(Minecraft.getInstance()));
    }
}
