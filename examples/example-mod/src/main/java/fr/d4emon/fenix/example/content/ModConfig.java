package fr.d4emon.fenix.example.content;

/**
 * What this mod lets a player change.
 *
 * <p>The record is the schema, the defaults and the documentation at once: the
 * component names are the keys in `config.json`, and the instance in
 * {@link #DEFAULTS} is what a missing setting falls back to.
 */
public record ModConfig(boolean spawnWispsOnPeaceful, int maxWispsPerCommand, String greeting) {

    /** What a fresh install gets, and what any missing setting falls back to. */
    public static final ModConfig DEFAULTS =
            new ModConfig(false, 20, "This world runs the Fenix example mod.");

    /**
     * Checks the settings.
     *
     * <p>In the compact constructor because that is the one place a value
     * cannot get in without passing through — a check anywhere else is a check
     * somebody eventually routes around.
     */
    public ModConfig {
        if (maxWispsPerCommand < 1) {
            throw new IllegalArgumentException("maxWispsPerCommand must be at least 1");
        }
    }
}
