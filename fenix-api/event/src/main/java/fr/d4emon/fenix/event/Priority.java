package fr.d4emon.fenix.event;

/**
 * Conventional listener priorities.
 *
 * <p>A <strong>higher</strong> value runs earlier, which is the way round most
 * people guess. Any {@code int} works; these constants are spaced a thousand
 * apart so a mod can slot itself between two of them without inventing a scale.
 *
 * <p>Listeners registered at the same priority run in registration order, so a
 * mod that registers twice keeps its own ordering.
 */
public final class Priority {

    /** Runs before everything else. For observing an event before anyone edits it. */
    public static final int HIGHEST = 2000;

    /** Runs early. */
    public static final int HIGH = 1000;

    /** The default. */
    public static final int NORMAL = 0;

    /** Runs late. */
    public static final int LOW = -1000;

    /** Runs after everything else. For seeing the final outcome. */
    public static final int LOWEST = -2000;

    private Priority() {
    }
}
