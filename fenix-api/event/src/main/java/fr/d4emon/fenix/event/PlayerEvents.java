package fr.d4emon.fenix.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Players arriving, leaving and dying.
 *
 * <p>Server-side, all of them. A client knows when <em>it</em> joined and
 * nothing about anybody else's session, so anything that has to be true for
 * every player belongs here.
 */
public final class PlayerEvents {

    /**
     * A player who has just arrived and can be sent things.
     *
     * @param player who joined
     */
    public record Joined(ServerPlayer player) {
    }

    /**
     * A player on their way out.
     *
     * <p>Fired while they are still on the server, so their inventory and
     * position can still be read — a moment later there is nothing to read.
     *
     * @param player who is leaving
     */
    public record Left(ServerPlayer player) {
    }

    /**
     * A player who has died.
     *
     * @param player who died
     * @param cause  what killed them
     */
    public record Died(ServerPlayer player, DamageSource cause) {
    }

    /**
     * A player who has just respawned.
     *
     * <p>The player is a <em>new</em> object: respawning replaces it rather
     * than resetting it, which is why anything a mod attached to the old one is
     * gone and has to be put back here.
     *
     * @param player    the new player
     * @param endPortal whether they arrived through the end portal rather than
     *                  by dying
     */
    public record Respawned(ServerPlayer player, boolean endPortal) {
    }

    /** Fires once the player is in the world and their connection can carry payloads. */
    /**
     * A player right-clicked holding something, aimed at no block.
     *
     * <p>What eating, drinking, throwing and drawing a bow all start as. The
     * matching event for right-clicking a block is {@code BlockEvents.USE}.
     *
     * @param player the player
     * @param level  the world they are in
     * @param stack  what they are holding in that hand
     * @param hand   which hand
     */
    public record UseItem(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand) {
    }

    /**
     * A player picked an item up off the ground.
     *
     * @param player the player
     * @param stack  what was picked up, before it is merged into the inventory
     */
    public record PickedUp(Player player, ItemStack stack) {
    }

    /**
     * A player moved from one dimension to another.
     *
     * @param player the player, already in the new dimension
     * @param from   where they were
     * @param to     where they are
     */
    public record ChangedDimension(ServerPlayer player, ResourceKey<Level> from,
                                   ResourceKey<Level> to) {
    }

    public static final Event<Joined> JOINED = Event.create();

    /**
     * Fires when a player picks something up.
     *
     * <p>The stack is what was on the ground, read before the inventory takes
     * it — afterwards it has been merged into a stack that was already there and
     * the count no longer says what was collected.
     */
    public static final Event<PickedUp> PICKED_UP = Event.create();

    /**
     * Fires after a player has arrived in another dimension.
     *
     * <p>After, not before: the useful moment is the one where the player is
     * somewhere and can be told about it, and a listener firing beforehand would
     * be looking at a player who is about to stop being where they are.
     */
    public static final Event<ChangedDimension> CHANGED_DIMENSION = Event.create();

    /**
     * Fires when a player uses an item away from a block. Cancelling it stops
     * whatever the item would have done.
     */
    public static final CancellableEvent<UseItem> USE_ITEM = CancellableEvent.create();

    /** Fires while the player is still readable. */
    public static final Event<Left> LEFT = Event.create();

    /** Fires when a player dies, before their items are scattered. */
    public static final Event<Died> DIED = Event.create();

    /** Fires when a player comes back, on a new player object. */
    public static final Event<Respawned> RESPAWNED = Event.create();

    private PlayerEvents() {
    }
}
