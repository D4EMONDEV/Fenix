package fr.d4emon.fenix.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Entities entering and leaving the world.
 *
 * <p>Server-side: the client is told about entities that already exist, and a
 * mod that acted on the client's copy would be acting on a shadow.
 */
public final class EntityEvents {

    /**
     * An entity about to be added to a level.
     *
     * <p>Cancelling stops it being added at all — which is how a mod refuses a
     * spawn rather than removing the entity a tick later, after it has already
     * been seen.
     *
     * @param entity what is being added
     * @param level  where
     */
    public record Spawning(Entity entity, ServerLevel level) {
    }

    /**
     * A living entity that has died. Players included.
     *
     * @param entity what died
     * @param cause  what killed it
     */
    public record Died(LivingEntity entity, DamageSource cause) {
    }

    /** Fires before an entity joins a level; cancelling keeps it out. */
    /**
     * Something living is about to take damage.
     *
     * <p>Fires before the hit is worked out, so cancelling it means the damage
     * never happens at all — no knockback, no hurt animation, no death. The
     * amount is what the source asked for, before armour and effects reduce it.
     *
     * @param entity the one being hurt
     * @param level  the world it is in
     * @param source what is hurting it, which says who and how
     * @param amount the damage asked for, before any reduction
     */
    public record Hurt(LivingEntity entity, ServerLevel level, DamageSource source, float amount) {
    }

    /**
     * A player right-clicked an entity.
     *
     * <p>Fires before the entity is asked what to do about it, so cancelling
     * stops the interaction entirely — no saddling, no naming, no trade screen.
     *
     * @param player the player
     * @param target what they clicked
     * @param hand   which hand
     */
    public record Interact(Player player, Entity target, InteractionHand hand) {
    }

    public static final CancellableEvent<Spawning> SPAWNING = CancellableEvent.create();

    /**
     * Fires when a player right-clicks an entity. Cancelling it stops whatever
     * the entity would have done.
     *
     * <p>Fires on both sides: the client runs it to predict the result, the
     * server to decide it. A listener that changes the world should check
     * {@code player.level().isClientSide} first, or it will do the thing twice
     * in single-player and disagree with the server in multiplayer.
     */
    public static final CancellableEvent<Interact> INTERACT = CancellableEvent.create();

    /**
     * Fires before anything living takes damage. Cancelling it stops the hit.
     *
     * <p>Server-side only: the client is told about damage, it does not decide
     * it, and a listener that ran there would disagree with the server the first
     * time it mattered.
     */
    public static final CancellableEvent<Hurt> HURT = CancellableEvent.create();

    /** Fires when anything living dies, before its drops are decided. */
    public static final Event<Died> DIED = Event.create();

    private EntityEvents() {
    }
}
