package fr.d4emon.fenix.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The one payload type Fenix registers with vanilla, in each direction.
 *
 * <p>Every mod payload travels inside one of these rather than as a vanilla
 * payload of its own, and that is a deliberate choice with a concrete reason.
 *
 * <p>Vanilla builds its payload dispatch table once, eagerly, from a list
 * captured when {@code ClientboundCustomPayloadPacket} is first loaded. A mod
 * wanting a type in that table would have to have registered it before that
 * moment — a moment decided by vanilla's own class-loading order, which is not
 * something a loader should bet on and which could change under it on any
 * Minecraft update. The failure would be silent, too: the packet decodes as a
 * discarded payload and nothing is ever heard from again.
 *
 * <p>These two types are constants, so the injection that adds them carries no
 * such bet — it runs when the class is transformed, which is always before its
 * static initialiser. Mods then register into Fenix's own table whenever they
 * like, and the ordering question disappears rather than being answered.
 *
 * <p>The cost is one identifier on the wire per packet. The gain is that an
 * unknown channel can be reported by name instead of vanishing.
 *
 * @param channel   which mod payload is inside
 * @param data      its encoded body
 * @param direction which way it is going; not written, since each packet class
 *                  already knows
 */
public record Envelope(Identifier channel, byte[] data,
                       CustomPacketPayload.Type<Envelope> direction) implements CustomPacketPayload {

    /** Client to server. */
    public static final CustomPacketPayload.Type<Envelope> TO_SERVER =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("fenix", "to_server"));

    /** Server to client. */
    public static final CustomPacketPayload.Type<Envelope> TO_CLIENT =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("fenix", "to_client"));

    /** Reads and writes an envelope travelling towards the server. */
    public static final StreamCodec<FriendlyByteBuf, Envelope> TO_SERVER_CODEC =
            CustomPacketPayload.codec(Envelope::write, buffer -> read(buffer, TO_SERVER));

    /** Reads and writes an envelope travelling towards a client. */
    public static final StreamCodec<FriendlyByteBuf, Envelope> TO_CLIENT_CODEC =
            CustomPacketPayload.codec(Envelope::write, buffer -> read(buffer, TO_CLIENT));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return direction;
    }

    private void write(FriendlyByteBuf out) {
        out.writeIdentifier(channel);
        out.writeByteArray(data);
    }

    private static Envelope read(FriendlyByteBuf in, CustomPacketPayload.Type<Envelope> direction) {
        return new Envelope(in.readIdentifier(), in.readByteArray(), direction);
    }
}
