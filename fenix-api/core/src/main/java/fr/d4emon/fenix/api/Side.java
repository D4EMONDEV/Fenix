package fr.d4emon.fenix.api;

/**
 * Which side of the game the code is currently running on.
 *
 * <p>This is a fact about the running process, so it is always exactly one of
 * the two. It is not the same thing as the {@code side} field of
 * {@code fenix.mod.json}, which says where a mod is <em>allowed</em> to run and
 * therefore also accepts {@code both}.
 *
 * <p>A side check is not enough on its own to keep client-only code off a
 * server. Class loading resolves every type a method mentions, so the guarded
 * code has to live in a <em>separate method</em> — an {@code if} in the same
 * method still fails.
 */
public enum Side {

    /** The game client, with a window, rendering and input. */
    CLIENT,

    /** A dedicated server, with no client classes available at all. */
    SERVER;

    /**
     * {@return whether this is {@link #CLIENT}}
     */
    public boolean isClient() {
        return this == CLIENT;
    }

    /**
     * {@return whether this is {@link #SERVER}}
     */
    public boolean isServer() {
        return this == SERVER;
    }
}
