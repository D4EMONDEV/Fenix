package fr.d4emon.fenix.example;

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Mod;

/**
 * The smallest useful Fenix mod: one class, one log line.
 *
 * <p>Nothing in {@code fenix.mod.json} points here — the {@link Mod} annotation
 * is the declaration, and the annotation processor records it while this
 * compiles. Rename or mistype it and the build fails, not the launch.
 */
@Mod("example-mod")
public final class ExampleMod implements FenixMod {

    /** Instantiated by the loader from the compile-time index. */
    public ExampleMod() {
    }

    @Override
    public void onInit(Fenix fenix) {
        fenix.logger().info("Example mod loaded — Fenix {}, {} side",
                fenix.loaderVersion(), fenix.side());
    }
}
