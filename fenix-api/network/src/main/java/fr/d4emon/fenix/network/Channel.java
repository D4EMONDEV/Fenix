package fr.d4emon.fenix.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * What a payload has in common whichever way it travels.
 *
 * <p>Sealed rather than extensible: a payload goes one way or the other, and
 * the two are not interchangeable. Which way it goes is in the type — a
 * {@link ToServer} cannot be sent to a client, and that is a compile error
 * rather than a packet nobody handles.
 *
 * @param <T> what is being sent
 */
public abstract sealed class Channel<T> permits ToServer, ToClient {

    private final Identifier id;
    private final StreamCodec<FriendlyByteBuf, T> codec;

    Channel(Identifier id, StreamCodec<FriendlyByteBuf, T> codec) {
        this.id = Objects.requireNonNull(id, "id");
        this.codec = Objects.requireNonNull(codec, "codec");
        Channels.register(this);
    }

    /**
     * {@return what this channel is called on the wire}
     */
    public final Identifier id() {
        return id;
    }

    /**
     * {@return how its payloads are written and read}
     */
    public final StreamCodec<FriendlyByteBuf, T> codec() {
        return codec;
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + id + "]";
    }

    /** Decodes and hands a payload to whoever registered for it. */
    abstract void deliver(byte[] data, Object context);

    /** Reads one payload out of an envelope's body. */
    final T decode(byte[] data) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(data));
        try {
            T payload = codec.decode(buffer);
            if (buffer.isReadable()) {
                // A codec that reads less than was written is a codec whose
                // read and write halves have drifted apart, which otherwise
                // shows up as garbage in the next field of the next packet.
                throw new IllegalStateException(buffer.readableBytes()
                        + " unread bytes — this channel's codec reads less than it writes");
            }
            return payload;
        } finally {
            buffer.release();
        }
    }

    /** Writes one payload into an envelope's body. */
    final byte[] encode(T payload) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            codec.encode(buffer, payload);
            byte[] data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
            return data;
        } finally {
            buffer.release();
        }
    }
}
