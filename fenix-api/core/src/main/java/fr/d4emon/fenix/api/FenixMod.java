package fr.d4emon.fenix.api;

/**
 * The lifecycle of a mod.
 *
 * <p>Every method has a default implementation, so a mod overrides only the
 * phases it cares about. The phases always run in the order below, and a mod is
 * never called before anything it declares in {@code depends}.
 *
 * <p>Each method receives its own {@link Fenix} context rather than reaching for
 * a global one. That is what lets every mod have its own logger and its own view
 * of what is loaded.
 *
 * @see Mod
 */
public interface FenixMod {

    /**
     * Runs before any game class is loaded.
     *
     * <p>The only phase where the game can still be influenced before it exists:
     * class transformers and network payload types have to be registered here,
     * because the classes that read them have not been initialised yet.
     *
     * <p>Touching game classes from this method defeats its purpose — loading one
     * freezes it before transformation.
     *
     * @param fenix this mod's view of the loader
     */
    default void onPreLaunch(Fenix fenix) {
    }

    /**
     * Runs while the game's registries are being populated, before they freeze.
     *
     * <p>Blocks, items and anything else that lives in a registry are added here.
     * This window exists because the game finalises registry contents in one pass
     * and refuses additions afterwards.
     *
     * @param fenix this mod's view of the loader
     */
    default void onRegister(Fenix fenix) {
    }

    /**
     * Runs once the game is up and its content is final.
     *
     * <p>The right place for everything that is not registration: listening for
     * events, reading configuration, wiring gameplay.
     *
     * @param fenix this mod's view of the loader
     */
    default void onInit(Fenix fenix) {
    }
}
