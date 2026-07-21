package fr.d4emon.fenix.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
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
    public static final CancellableEvent<Spawning> SPAWNING = CancellableEvent.create();

    /** Fires when anything living dies, before its drops are decided. */
    public static final Event<Died> DIED = Event.create();

    private EntityEvents() {
    }
}
