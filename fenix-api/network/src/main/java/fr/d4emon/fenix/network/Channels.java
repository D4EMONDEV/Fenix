package fr.d4emon.fenix.network;

import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fenix's own payload table, and the dispatch into it.
 *
 * <p>This is the table vanilla's would have been, had adding to it been
 * possible — see {@link Envelope} for why it is not. Registration happens
 * whenever a channel is declared, with no ordering constraint at all.
 */
public final class Channels {

    private static final Map<Identifier, Channel<?>> CHANNELS = new ConcurrentHashMap<>();

    /** Installed by the client half of this module; absent on a server. */
    private static volatile ClientSender sender;

    private Channels() {
    }

    /** How the client reaches its connection, without common code naming it. */
    public interface ClientSender {

        /**
         * Sends an envelope to the server.
         *
         * @param envelope what to send
         */
        void toServer(Envelope envelope);
    }

    static void register(Channel<?> channel) {
        Channel<?> existing = CHANNELS.putIfAbsent(channel.id(), channel);
        if (existing != null && existing != channel) {
            // Two mods on one id would each receive the other's payloads and
            // decode them as their own, which produces nonsense rather than an
            // error. Namespacing makes this impossible by accident.
            throw new IllegalStateException("two channels share the id " + channel.id()
                    + " — namespace yours with your mod id");
        }
    }

    /**
     * Installs the client's way of sending. Called by the client half.
     *
     * @param clientSender the sender
     */
    public static void sender(ClientSender clientSender) {
        sender = Objects.requireNonNull(clientSender, "clientSender");
    }

    static ClientSender sender() {
        ClientSender current = sender;
        if (current == null) {
            throw new IllegalStateException(
                    "no client connection — a ToServer channel can only be sent from a client");
        }
        return current;
    }

    /**
     * Hands an arriving envelope to its channel.
     *
     * <p>An unknown channel is ignored rather than fatal: a server may well run
     * mods its players do not have, and the reverse. Being able to name the
     * channel in a log line is the whole reason Fenix wraps payloads at all —
     * vanilla would have discarded it without a word.
     *
     * @param envelope  what arrived
     * @param context   the sending player on the server, {@code null} on a client
     * @param direction which way it came, for the mismatch check
     * @return whether a channel took it
     */
    public static boolean deliver(Envelope envelope, Object context,
                                  net.minecraft.network.protocol.common.custom.CustomPacketPayload
                                          .Type<Envelope> direction) {
        Channel<?> channel = CHANNELS.get(envelope.channel());
        if (channel == null) {
            return false;
        }
        boolean towardsServer = direction == Envelope.TO_SERVER;
        if (towardsServer != channel instanceof ToServer<?>) {
            // Only reachable from a peer that is lying or badly out of date;
            // delivering it anyway would hand the handler a payload decoded by
            // the wrong codec.
            throw new IllegalStateException(envelope.channel() + " arrived travelling the wrong way");
        }
        channel.deliver(envelope.data(), context);
        return true;
    }
}
