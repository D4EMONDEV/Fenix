package fr.d4emon.fenix.ember;

/**
 * Something that writes part of a mod's resource files.
 *
 * <p>Normally implemented by extending one of the providers —
 * {@link EmberLanguageProvider}, {@link EmberModelProvider} and the rest —
 * rather than directly. Implement this only for output none of them covers.
 */
@FunctionalInterface
public interface EmberGenerator {

    /**
     * Writes this generator's files.
     *
     * @param output where they go
     */
    void generate(EmberOutput output);
}
