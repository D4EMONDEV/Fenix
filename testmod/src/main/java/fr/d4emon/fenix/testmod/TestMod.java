package fr.d4emon.fenix.testmod;

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Mod;
import fr.d4emon.fenix.event.BlockEvents;
import fr.d4emon.fenix.event.Flow;
import fr.d4emon.fenix.event.ServerEvents;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The in-repo mod that rides along with a manual run: one log line per
 * lifecycle phase, plus a listener on every kind of event, so a broken phase or
 * an event that stopped firing is visible at a glance.
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

        ServerEvents.STARTED.register(started -> fenix.logger().info("server started"));

        // Ticks are far too noisy to log one by one, so these count and report
        // occasionally — enough to prove the event is alive.
        AtomicLong serverTicks = new AtomicLong();
        ServerEvents.TICK_END.register(tick -> {
            long count = serverTicks.incrementAndGet();
            if (count % 100 == 0) {
                fenix.logger().info("server tick {}", count);
            }
        });

        // Observes without ever cancelling; the point is to see it fire.
        BlockEvents.BREAK.register(event -> {
            fenix.logger().info("{} broke a block at {}",
                    event.player().getName().getString(), event.pos());
            return Flow.CONTINUE;
        });

        if (fenix.side().isClient()) {
            // The client half lives in src/client/java, where it needs no
            // guard at all — see TestModClient.
            fenix.logger().info("client side");
        }
    }
}
