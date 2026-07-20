package fr.d4emon.fenix.loader.metadata;

import fr.d4emon.fenix.api.Side;

import java.util.Locale;

/**
 * Where a mod declares it is allowed to run, from the {@code side} field of
 * {@code fenix.mod.json}.
 *
 * <p>Not to be confused with {@link Side}, which is where the process actually
 * <em>is</em> and therefore only ever has two values. The two meet at
 * {@link #includes(Side)}.
 */
public enum ModSide {

    /** Loads on a client only, and is skipped on a dedicated server. */
    CLIENT,

    /** Loads on a server only, including the server inside a single-player game. */
    SERVER,

    /** Loads everywhere. The default when the field is absent. */
    BOTH;

    /**
     * Checks whether a mod declaring this should load on the given side.
     *
     * @param side the side the process is running on
     * @return whether the mod should load
     */
    public boolean includes(Side side) {
        return switch (this) {
            case BOTH -> true;
            case CLIENT -> side.isClient();
            case SERVER -> side.isServer();
        };
    }

    /**
     * Parses the {@code side} field.
     *
     * @param text one of {@code client}, {@code server} or {@code both}, in any case
     * @return the parsed value
     * @throws IllegalArgumentException if the text is none of those
     * @throws NullPointerException     if the text is {@code null}
     */
    public static ModSide parse(String text) {
        return switch (text.toLowerCase(Locale.ROOT).trim()) {
            case "client" -> CLIENT;
            case "server" -> SERVER;
            case "both" -> BOTH;
            default -> throw new IllegalArgumentException(
                    "Not a side: '" + text + "' (expected 'client', 'server' or 'both')");
        };
    }
}
