package fr.d4emon.fenix.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Tells a joining player when their game and the server's do not match.
 *
 * <p>Without this, a client missing one of the server's mods is not refused —
 * it is admitted, and then falls apart in ways that name nothing useful.
 * Network ids are assigned per registry, so one absent block shifts every id
 * after it: the player sees the wrong blocks, or is kicked by vanilla with
 * "Can't find id for Block{…}", or watches chunks arrive as nonsense. None of
 * those mention the mod that is actually missing.
 *
 * <p>So the server states what it has on join, the client compares and reports
 * back, and the server refuses with a sentence naming what is wrong. Detection
 * and a clear refusal — never quietly remapping ids to paper over it, which
 * trades a confusing disconnect for a world that corrupts slowly.
 *
 * <p>A client without Fenix answers nothing and is left alone. It is no worse
 * off than before, and a server that wants to insist can say so itself.
 */
public final class RegistryCheck {

    /** What the server has, sent to every joining player. */
    public static final ToClient<RegistrySummary> SERVER_SUMMARY = ToClient.of(
            Identifier.fromNamespaceAndPath("fenix", "registry_summary"), RegistrySummary.CODEC);

    /** What the client found wrong with it, if anything. */
    public static final ToServer<List<String>> MISMATCH = ToServer.of(
            Identifier.fromNamespaceAndPath("fenix", "registry_mismatch"), sentences());

    static {
        // Registered on both sides. The client's never fires on a server and
        // the server's never fires on a client, so there is nothing to guard.
        SERVER_SUMMARY.receive(RegistryCheck::compare);
        MISMATCH.receive(RegistryCheck::refuse);
    }

    private RegistryCheck() {
    }

    /**
     * Tells a joining player what this server has. Called from the join mixin.
     *
     * @param player who just joined
     */
    public static void greet(ServerPlayer player) {
        SERVER_SUMMARY.send(player, RegistrySummary.local());
    }

    /** Loads this class on a client, so its handlers are registered. */
    public static void listen() {
        // The static initialiser above is the work; this is how the client half
        // asks for it without appearing to do nothing.
    }

    /** Client side: does what arrived match what we have? */
    private static void compare(RegistrySummary theirs) {
        List<String> problems = RegistrySummary.local().differencesFrom(theirs);
        if (!problems.isEmpty()) {
            // Reported rather than acted on: the server is the one that can
            // disconnect with a reason the player actually sees, and its log is
            // where an operator will look.
            MISMATCH.send(problems);
        }
    }

    /** Server side: the client disagrees, so refuse it. */
    private static void refuse(List<String> problems, ServerPlayer player) {
        String detail = String.join("; ", problems);
        player.connection.disconnect(Component.literal(
                "This server and your game do not have the same mods — " + detail));
    }

    /** A plain list of sentences on the wire. */
    private static StreamCodec<FriendlyByteBuf, List<String>> sentences() {
        return StreamCodec.of(
                (buffer, list) -> {
                    buffer.writeVarInt(list.size());
                    list.forEach(buffer::writeUtf);
                },
                buffer -> {
                    List<String> list = new ArrayList<>();
                    for (int i = buffer.readVarInt(); i > 0; i--) {
                        list.add(buffer.readUtf());
                    }
                    return List.copyOf(list);
                });
    }
}
