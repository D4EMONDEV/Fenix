package fr.d4emon.fenix.event;

import net.minecraft.server.level.ServerPlayer;
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
    public static final Event<Joined> JOINED = Event.create();

    /** Fires while the player is still readable. */
    public static final Event<Left> LEFT = Event.create();

    /** Fires when a player dies, before their items are scattered. */
    public static final Event<Died> DIED = Event.create();

    /** Fires when a player comes back, on a new player object. */
    public static final Event<Respawned> RESPAWNED = Event.create();

    private PlayerEvents() {
    }
}
