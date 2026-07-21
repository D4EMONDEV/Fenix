package fr.d4emon.fenix.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * What players do to blocks, decided by the server.
 *
 * <p>These are the events to use for anything that must actually hold — block
 * protection, claims, permissions. The server has the final say, so cancelling
 * here really stops the action, for every player, including someone using a
 * modified client.
 *
 * <p>Their client-side counterparts in
 * {@code fr.d4emon.fenix.event.client.ClientBlockEvents} exist to give instant
 * feedback and avoid the visual rubber-band of a block breaking and coming
 * back — never as the enforcement point.
 */
public final class BlockEvents {

    /**
     * A player about to break a block.
     *
     * @param player the player
     * @param level  the world it is in
     * @param pos    where the block is
     */
    public record Break(ServerPlayer player, ServerLevel level, BlockPos pos) {
    }

    /**
     * A player about to right-click a block.
     *
     * @param player the player
     * @param level  the world it is in
     * @param hand   the hand used
     * @param hit    exactly where and on which face
     */
    public record Use(ServerPlayer player, Level level, InteractionHand hand, BlockHitResult hit) {
    }

    /** Fires before a block is broken. Cancelling leaves the block standing. */
    public static final CancellableEvent<Break> BREAK = CancellableEvent.create();

    /** Fires before a block is used. Cancelling stops the interaction. */
    public static final CancellableEvent<Use> USE = CancellableEvent.create();

    private BlockEvents() {
    }
}
