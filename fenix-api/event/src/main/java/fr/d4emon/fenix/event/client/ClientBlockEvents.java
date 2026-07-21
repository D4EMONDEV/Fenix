package fr.d4emon.fenix.event.client;

import fr.d4emon.fenix.event.CancellableEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

/**
 * What the player is trying to do to a block, on their own machine.
 *
 * <p>These fire before the client tells the server anything, so cancelling
 * gives instant feedback and avoids the block visually breaking and snapping
 * back when the server refuses.
 *
 * <p><strong>Never the enforcement point.</strong> A modified client can skip
 * these entirely. Anything that must actually hold belongs in
 * {@code fr.d4emon.fenix.event.BlockEvents}, on the server; use these as well
 * only to make the refusal feel immediate.
 */
public final class ClientBlockEvents {

    /**
     * The player starting to break a block.
     *
     * @param player the player
     * @param pos    where the block is
     * @param face   the side being hit
     */
    public record Attack(LocalPlayer player, BlockPos pos, Direction face) {
    }

    /**
     * The player right-clicking a block.
     *
     * @param player the player
     * @param hand   the hand used
     * @param hit    exactly where and on which face
     */
    public record Use(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
    }

    /** Fires before the client starts breaking a block. */
    public static final CancellableEvent<Attack> ATTACK = CancellableEvent.create();

    /** Fires before the client uses a block. */
    public static final CancellableEvent<Use> USE = CancellableEvent.create();

    private ClientBlockEvents() {
    }
}
