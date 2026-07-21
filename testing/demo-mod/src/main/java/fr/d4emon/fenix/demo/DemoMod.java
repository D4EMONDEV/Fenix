package fr.d4emon.fenix.demo;

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Mod;

/**
 * The mod the fake game loads.
 *
 * <p>Deliberately free of any Minecraft reference: it exists so the smoke test
 * can prove discovery, resolution, classloading and the whole lifecycle without
 * a game to download. Anything that needs real Minecraft belongs in
 * {@code testmod} or {@code examples/example-mod} instead.
 */
@Mod("demo-mod")
public final class DemoMod implements FenixMod {

    /** Instantiated by the loader from the compile-time index. */
    public DemoMod() {
    }

    @Override
    public void onPreLaunch(Fenix fenix) {
        fenix.logger().info("onPreLaunch — the game does not exist yet");
    }

    @Override
    public void onRegister(Fenix fenix) {
        fenix.logger().info("onRegister — the registries are open");
    }

    @Override
    public void onInit(Fenix fenix) {
        fenix.logger().info("Hello from {}! (Fenix {}, {} mod(s) loaded, side {})",
                fenix.mod().name(), fenix.loaderVersion(), fenix.mods().size(), fenix.side());
    }
}
