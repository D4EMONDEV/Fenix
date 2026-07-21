package fr.d4emon.fenix.probe;

import fr.d4emon.fenix.network.Channels;
import fr.d4emon.fenix.network.Envelope;
import fr.d4emon.fenix.network.ToClient;
import fr.d4emon.fenix.network.ToServer;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs as the game: sends a payload through the real wire format and checks it
 * arrives intact.
 *
 * <p>The mixin check proves the injections land. This proves the bytes are
 * right, which is the other half and the one that fails subtly — a codec whose
 * read and write halves disagree corrupts whatever is decoded next rather than
 * failing where the mistake is.
 */
public final class NetworkProbe {

    private record Greeting(String who, int times) {

        static final StreamCodec<FriendlyByteBuf, Greeting> CODEC =
                StreamCodec.of(
                        (buffer, value) -> {
                            buffer.writeUtf(value.who());
                            buffer.writeVarInt(value.times());
                        },
                        buffer -> new Greeting(buffer.readUtf(), buffer.readVarInt()));
    }

    private NetworkProbe() {
    }

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        ToServer<Greeting> upstream =
                ToServer.of(Identifier.parse("probemod:greeting"), Greeting.CODEC);
        ToClient<Greeting> downstream =
                ToClient.of(Identifier.parse("probemod:answer"), Greeting.CODEC);

        AtomicReference<Greeting> received = new AtomicReference<>();
        downstream.receive(received::set);

        Greeting sent = new Greeting("d4emon", 3);

        // Exactly what the wire does: wrap, encode through vanilla's codec,
        // decode it back, then dispatch.
        Envelope envelope = envelope(downstream, sent);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        Envelope.TO_CLIENT_CODEC.encode(buffer, envelope);
        Envelope back = Envelope.TO_CLIENT_CODEC.decode(buffer);

        require(!buffer.isReadable(), "the envelope codec should read back everything it wrote");
        require(back.channel().equals(Identifier.parse("probemod:answer")),
                "the channel should survive the round trip");
        require(Channels.deliver(back, null, Envelope.TO_CLIENT),
                "a registered channel should take its own payload");
        require(sent.equals(received.get()),
                "what the handler receives should equal what was sent, got " + received.get());

        // A channel travelling the wrong way is a peer lying or badly out of
        // date; delivering it would hand a handler a payload decoded by the
        // wrong codec.
        boolean refused = false;
        try {
            Channels.deliver(back, null, Envelope.TO_SERVER);
        } catch (IllegalStateException expected) {
            refused = true;
        }
        require(refused, "a payload arriving the wrong way should be refused");

        require(!Channels.deliver(new Envelope(Identifier.parse("probemod:nobody"),
                        new byte[0], Envelope.TO_CLIENT), null, Envelope.TO_CLIENT),
                "an unknown channel should be declined, not fatal — a server may run mods "
                        + "its players do not have");

        require(upstream.id().equals(Identifier.parse("probemod:greeting")), "ids are kept");

        System.out.println("network conformance: all checks passed");
    }

    /** Wraps a payload the way sending does, without needing a connection. */
    private static Envelope envelope(ToClient<Greeting> channel, Greeting payload) {
        FriendlyByteBuf body = new FriendlyByteBuf(Unpooled.buffer());
        Greeting.CODEC.encode(body, payload);
        byte[] data = new byte[body.readableBytes()];
        body.readBytes(data);
        return new Envelope(channel.id(), data, Envelope.TO_CLIENT);
    }

    private static void require(boolean condition, String what) {
        if (!condition) {
            throw new AssertionError("network conformance failed: " + what);
        }
    }
}
