package fr.d4emon.fenix.ember;

/**
 * Describes what a mod's resource files should contain.
 *
 * <p>Implemented by a class marked {@link Generator}. It runs at build time,
 * inside a real game, so registered content can be referred to directly rather
 * than by repeating its name as a string.
 */
@FunctionalInterface
public interface EmberGenerator {

    /**
     * Describes this mod's assets and data.
     *
     * @param ember what to write them with
     */
    void collect(Ember ember);
}
