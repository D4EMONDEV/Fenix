package fr.d4emon.fenix.example.content;

import fr.d4emon.fenix.network.ToClient;
import fr.d4emon.fenix.network.ToServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * What this mod says over the wire.
 *
 * <p>Two channels, one each way, so the tally block can report a count the
 * client has no way to know and take a reset the server has no way to guess.
 */
public final class ModPayloads {

    /** The server telling a client what a tally block now reads. */
    public record Tally(BlockPos pos, int count) {

        static final StreamCodec<FriendlyByteBuf, Tally> CODEC = StreamCodec.of(
                (buffer, value) -> {
                    buffer.writeBlockPos(value.pos());
                    buffer.writeVarInt(value.count());
                },
                buffer -> new Tally(buffer.readBlockPos(), buffer.readVarInt()));
    }

    /** A client asking for a tally block to be put back to zero. */
    public record Reset(BlockPos pos) {

        static final StreamCodec<FriendlyByteBuf, Reset> CODEC = StreamCodec.of(
                (buffer, value) -> buffer.writeBlockPos(value.pos()),
                buffer -> new Reset(buffer.readBlockPos()));
    }

    /** Server to client: here is the count. */
    public static final ToClient<Tally> TALLY =
            ToClient.of(Identifier.fromNamespaceAndPath("example-mod", "tally"), Tally.CODEC);

    /** Client to server: please reset this one. */
    public static final ToServer<Reset> RESET =
            ToServer.of(Identifier.fromNamespaceAndPath("example-mod", "reset"), Reset.CODEC);

    private ModPayloads() {
    }

    /** Says what the server does with a reset. Called from {@code onRegister}. */
    public static void listen() {
        RESET.receive((reset, player) -> {
            // Never trust what arrives. A client can send any position at any
            // time, so a mod that skipped this would let anyone reset a block
            // across the world — or in a chunk they cannot even see.
            if (!player.blockPosition().closerThan(reset.pos(), 8)) {
                return;
            }
            if (player.level().getBlockEntity(reset.pos()) instanceof RubyTallyBlockEntity tally) {
                TALLY.send(player, new Tally(reset.pos(), tally.reset()));
            }
        });
    }
}
