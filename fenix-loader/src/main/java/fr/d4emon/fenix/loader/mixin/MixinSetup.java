package fr.d4emon.fenix.loader.mixin;

import fr.d4emon.fenix.api.Side;
import fr.d4emon.fenix.loader.classloader.FenixClassLoader;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

/**
 * Brings Mixin up, wires it into the {@link FenixClassLoader}, and registers
 * every mixin configuration — the loader's own and the mods'.
 *
 * <p>Called once, before any game class is loaded. After it returns, a game
 * class passing through the loader is transformed on the way in.
 */
public final class MixinSetup {

    private MixinSetup() {
    }

    /**
     * Initialises Mixin for one launch.
     *
     * @param loader  the loader the game and mods live in
     * @param side    the side being launched
     * @param configs the mixin configuration resource names, loader first
     * @throws NullPointerException if any argument is {@code null}
     */
    @SuppressWarnings("deprecation") // setCompatibilityLevel is the only way to lift the JAVA_13 ceiling
    public static void bootstrap(FenixClassLoader loader, Side side, List<String> configs) {
        Objects.requireNonNull(loader, "loader");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(configs, "configs");

        // The service is instantiated by Mixin, so it takes the classloader
        // statically — and the property makes Mixin pick ours directly rather
        // than iterating a ServiceLoader that would try the absent built-ins.
        FenixMixinService.bindClassLoader(loader);
        System.setProperty("mixin.service", FenixMixinService.class.getName());

        MixinBootstrap.init();

        // The game is compiled for Java 25, and so are the mixins. This fork
        // ships with MAX_SUPPORTED hard-coded to JAVA_13, so the active level
        // must be raised explicitly or every JAVA_25 config is silently
        // rejected — the whole environment then applies nothing at all.
        MixinEnvironment.setCompatibilityLevel(MixinEnvironment.CompatibilityLevel.JAVA_25);

        MixinEnvironment.getDefaultEnvironment().setSide(
                side == Side.CLIENT ? MixinEnvironment.Side.CLIENT : MixinEnvironment.Side.SERVER);

        for (String config : configs) {
            Mixins.addConfiguration(config);
        }

        FenixMixinService service = FenixMixinService.active();
        loader.setClassGenerator(service::generateClass);
        loader.addTransformer(service::transformClass);

        // Advance out of the setup phases so the configs are selected and mixins
        // apply as targets load. The public MixinEnvironment.init(Phase) only
        // registers an environment; the transition that actually calls
        // beginPhase() — and so visits the configs — is the private gotoPhase,
        // which is exactly what Fabric drives by reflection too.
        gotoPhase(MixinEnvironment.Phase.INIT);
        gotoPhase(MixinEnvironment.Phase.DEFAULT);

        // Force the transformer to exist now, so a wiring error surfaces here,
        // not deep inside the first game class.
        service.primeTransformer();
    }

    private static void gotoPhase(MixinEnvironment.Phase phase) {
        try {
            Method gotoPhase = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
            gotoPhase.setAccessible(true);
            gotoPhase.invoke(null, phase);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot advance Mixin to the " + phase + " phase — "
                    + "the Mixin version may be incompatible", e);
        }
    }
}
