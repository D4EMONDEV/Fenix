package fr.d4emon.fenix.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A payload the server sends to a client.
 *
 * <pre>{@code
 * public static final ToClient<SafeContents> CONTENTS =
 *         ToClient.of(Identifier.parse("mymod:contents"), SafeContents.CODEC);
 *
 * // client, once
 * CONTENTS.receive(contents -> show(contents));
 *
 * // server, whenever
 * CONTENTS.send(player, new SafeContents(items));
 * }</pre>
 *
 * <p>The handler runs on the client thread. A client that has no handler for
 * the channel simply drops it, which is what lets a server run a mod its
 * players do not have.
 *
 * @param <T> what is being sent
 */
public final class ToClient<T> extends Channel<T> {

    private volatile Consumer<T> handler;

    private ToClient(Identifier id, StreamCodec<FriendlyByteBuf, T> codec) {
        super(id, codec);
    }

    /**
     * Declares a channel towards clients.
     *
     * @param <T>   what is being sent
     * @param id    what it is called on the wire; namespace it with your mod id
     * @param codec how its payloads are written and read
     * @return the channel
     */
    public static <T> ToClient<T> of(Identifier id, StreamCodec<FriendlyByteBuf, T> codec) {
        return new ToClient<>(id, codec);
    }

    /**
     * Says what the client does when one arrives. Client only.
     *
     * @param handler given the payload; runs on the client thread
     */
    public void receive(Consumer<T> handler) {
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    /**
     * Sends one to a player.
     *
     * @param player who to send it to
     * @param payload what to send
     */
    public void send(ServerPlayer player, T payload) {
        Objects.requireNonNull(player, "player");
        player.connection.send(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(
                new Envelope(id(), encode(payload), Envelope.TO_CLIENT)));
    }

    /**
     * Sends one to everybody on the server.
     *
     * @param server  the running server
     * @param payload what to send
     */
    public void sendAll(MinecraftServer server, T payload) {
        Objects.requireNonNull(server, "server");
        // Encoded once rather than per player: the bytes are the same, and a
        // full server makes that difference worth having.
        Envelope envelope = new Envelope(id(), encode(payload), Envelope.TO_CLIENT);
        var packet = new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(envelope);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(packet);
        }
    }

    @Override
    void deliver(byte[] data, Object context) {
        Consumer<T> current = handler;
        if (current != null) {
            current.accept(decode(data));
        }
    }
}
