package fr.d4emon.fenix.event;

import net.minecraft.server.level.ServerLevel;

/**
 * Worlds being loaded and written out.
 *
 * <p>A server has several — the overworld, the nether, the end, and any a mod
 * or datapack adds — so these fire once per level rather than once per server.
 */
public final class LevelEvents {

    /**
     * A level, in whatever way the event means it.
     *
     * @param level the level
     */
    public record Of(ServerLevel level) {
    }

    /**
     * Fires when a level is ready to be used.
     *
     * <p>Per level, not per server: a mod keeping per-world state wants this
     * rather than {@link ServerEvents#STARTED}, which fires once no matter how
     * many worlds there are.
     */
    public static final Event<Of> LOADED = Event.create();

    /**
     * Fires as a level is written to disk.
     *
     * <p>The moment to flush anything a mod keeps alongside the world, since
     * afterwards the two can disagree.
     */
    public static final Event<Of> SAVING = Event.create();

    private LevelEvents() {
    }
}
