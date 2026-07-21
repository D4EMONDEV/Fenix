package fr.d4emon.fenix.probe;

import fr.d4emon.fenix.network.Channels;
import fr.d4emon.fenix.network.Envelope;
import fr.d4emon.fenix.network.RegistrySummary;
import fr.d4emon.fenix.network.ToClient;
import fr.d4emon.fenix.network.ToServer;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;

import java.util.List;
import java.util.Map;
import java.util.Set;
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

        checkRegistrySummary();

        System.out.println("network conformance: all checks passed");
    }

    /**
     * Two sides that disagree have to be told what is wrong, by name.
     *
     * <p>Without this a client missing a mod is admitted and then falls apart:
     * one absent block shifts every network id after it, so the player sees
     * the wrong blocks or is kicked by vanilla naming a block it cannot find.
     * Nothing in that mentions the mod actually missing.
     */
    private static void checkRegistrySummary() {
        RegistrySummary mine = RegistrySummary.local();
        require(mine.differencesFrom(mine).isEmpty(), "a side should agree with itself");

        // This run loads only the network module, so nothing modded is
        // registered and the summary is empty. That is the vanilla-shaped end
        // of the range, and it still has to compare cleanly.
        require(mine.namespaces().isEmpty(),
                "nothing modded is registered here, got " + mine.namespaces());

        RegistrySummary theirs = new RegistrySummary(mine.digests(), Set.of("some-other-mod"));
        List<String> problems = mine.differencesFrom(theirs);
        require(problems.size() == 1 && problems.getFirst().contains("some-other-mod"),
                "a missing mod should be named, got " + problems);

        // And the other way round: a client with a mod the server lacks is
        // just as broken, and just as worth naming.
        List<String> extra = theirs.differencesFrom(mine);
        require(extra.size() == 1 && extra.getFirst().contains("not on the other side"),
                "an extra mod should be named too, got " + extra);

        // Same mods, different content: the versions differ, which is a
        // different sentence and needs the digests rather than the names.
        RegistrySummary drifted = new RegistrySummary(
                Map.of("blocks", "0000000000000000", "items", "0000000000000000",
                        "entities", "0000000000000000", "block entities", "0000000000000000"),
                mine.namespaces());
        List<String> drift = mine.differencesFrom(drifted);
        require(drift.size() == 1 && drift.getFirst().contains("versions differ"),
                "same mods with different content should say so, got " + drift);

        // The summary has to survive the wire, or the comparison compares
        // something other than what the other side actually has.
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        RegistrySummary.CODEC.encode(buffer, mine);
        RegistrySummary back = RegistrySummary.CODEC.decode(buffer);
        require(!buffer.isReadable(), "the summary codec should read back everything it wrote");
        require(mine.differencesFrom(back).isEmpty(), "a summary should survive the round trip");
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
