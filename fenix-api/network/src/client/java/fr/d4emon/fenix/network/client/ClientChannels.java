package fr.d4emon.fenix.network.client;

import fr.d4emon.fenix.network.Channels;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

/**
 * Teaches the common half how a client sends.
 *
 * <p>{@code ToServer.send} has to work from common code — a mod's send site is
 * usually right beside the button that triggers it — but reaching the
 * connection means naming {@code Minecraft}, which common code cannot. So the
 * client half installs the one method that does.
 */
public final class ClientChannels {

    private ClientChannels() {
    }

    /** Installs it. Called by this module's client entry point. */
    public static void install() {
        Channels.sender(envelope -> {
            var connection = Minecraft.getInstance().getConnection();
            if (connection == null) {
                throw new IllegalStateException(
                        "not connected to a server — nothing to send " + envelope.channel() + " to");
            }
            connection.send(new ServerboundCustomPayloadPacket(envelope));
        });
    }
}
