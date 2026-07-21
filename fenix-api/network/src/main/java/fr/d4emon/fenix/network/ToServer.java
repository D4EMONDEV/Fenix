package fr.d4emon.fenix.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * A payload a client sends to the server.
 *
 * <pre>{@code
 * public static final ToServer<OpenSafe> OPEN_SAFE =
 *         ToServer.of(Identifier.parse("mymod:open_safe"), OpenSafe.CODEC);
 *
 * // server, once
 * OPEN_SAFE.receive((open, player) -> open(player, open.pos()));
 *
 * // client, whenever
 * OPEN_SAFE.send(new OpenSafe(pos));
 * }</pre>
 *
 * <p>The handler is given the player who sent it, and runs on the server
 * thread — anything reached from it is safe to touch. Treat what arrives as
 * untrusted: a client can send anything at any time, so check that the player
 * is near the block they claim to be opening.
 *
 * @param <T> what is being sent
 */
public final class ToServer<T> extends Channel<T> {

    private volatile BiConsumer<T, ServerPlayer> handler;

    private ToServer(Identifier id, StreamCodec<FriendlyByteBuf, T> codec) {
        super(id, codec);
    }

    /**
     * Declares a channel towards the server.
     *
     * @param <T>   what is being sent
     * @param id    what it is called on the wire; namespace it with your mod id
     * @param codec how its payloads are written and read
     * @return the channel
     */
    public static <T> ToServer<T> of(Identifier id, StreamCodec<FriendlyByteBuf, T> codec) {
        return new ToServer<>(id, codec);
    }

    /**
     * Says what the server does when one arrives.
     *
     * @param handler given the payload and the player it came from; runs on the
     *                server thread
     */
    public void receive(BiConsumer<T, ServerPlayer> handler) {
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    /**
     * Sends one to the server. Client only.
     *
     * @param payload what to send
     * @throws IllegalStateException if there is no server connection
     */
    public void send(T payload) {
        Channels.sender().toServer(new Envelope(id(), encode(payload), Envelope.TO_SERVER));
    }

    @Override
    void deliver(byte[] data, Object context) {
        BiConsumer<T, ServerPlayer> current = handler;
        if (current != null) {
            current.accept(decode(data), (ServerPlayer) context);
        }
    }
}
