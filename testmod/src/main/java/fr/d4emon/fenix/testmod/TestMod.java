package fr.d4emon.fenix.testmod;

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Mod;

/**
 * The in-repo mod that rides along with {@code runDemo}: one log line per
 * lifecycle phase, so a broken phase is visible at a glance.
 */
@Mod("testmod")
public final class TestMod implements FenixMod {

    /** Instantiated by the loader from the compile-time index. */
    public TestMod() {
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
