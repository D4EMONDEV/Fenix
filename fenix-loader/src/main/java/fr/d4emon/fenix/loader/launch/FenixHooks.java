package fr.d4emon.fenix.loader.launch;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The game's way back into the loader.
 *
 * <p>The later lifecycle phases can only be fired from <em>inside</em> the game
 * — registration while the registries are still open, init once the game is up.
 * With real Minecraft the calls are injected by the loader's own mixins; the
 * test harness's fake game calls them directly, which is exactly the point of
 * having a fake game.
 *
 * <p>This class lives in the loader, so it is parent-only: game code compiled
 * against it resolves to the same class the loader holds, static state
 * included.
 */
public final class FenixHooks {

    private static volatile FenixRuntime runtime;

    private FenixHooks() {
    }

    /** Binds the runtime for this launch. Called by {@link Launch}; tests may rebind. */
    static void bind(FenixRuntime active) {
        runtime = active;
    }

    /**
     * Fires {@code onRegister} for every mod. The game calls this while its
     * registries are still open. Repeats are ignored.
     */
    public static void onGameRegister() {
        active().fireRegister();
    }

    /**
     * Fires {@code onInit} for every mod. The game calls this once it is up.
     * Repeats are ignored.
     */
    public static void onGameInit() {
        active().fireInit();
    }

    /**
     * {@return every loaded mod's jar, keyed by mod id, in load order}
     *
     * <p>For the parts of Fenix that have to treat a mod as a <em>file</em>
     * rather than as code — resource loading above all, which hands the jars to
     * the game as resource packs.
     *
     * <p>Empty when no game is running, rather than throwing: something asking
     * for mod files outside a launch should get "there are none", not a crash.
     */
    public static Map<String, Path> modJars() {
        FenixRuntime current = runtime;
        if (current == null) {
            return Map.of();
        }
        Map<String, Path> jars = new LinkedHashMap<>();
        for (LoadedMod mod : current.mods()) {
            jars.put(mod.id(), mod.path());
        }
        return Collections.unmodifiableMap(jars);
    }

    private static FenixRuntime active() {
        FenixRuntime active = runtime;
        if (active == null) {
            throw new IllegalStateException(
                    "no Fenix runtime is active — was the game launched through Fenix?");
        }
        return active;
    }
}
