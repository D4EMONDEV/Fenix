package fr.d4emon.fenix.testmod.client;

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Mod;
import fr.d4emon.fenix.event.client.ClientEvents;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The client half of the test mod.
 *
 * <p>Nothing here is guarded, and nothing needs to be: everything under
 * {@code src/client/java} is indexed apart from the common half, so a dedicated
 * server is never told this class exists.
 */
@Mod("testmod")
public final class TestModClient implements FenixMod {

    /** Instantiated by the loader. */
    public TestModClient() {
    }

    @Override
    public void onInit(Fenix fenix) {
        AtomicLong clientTicks = new AtomicLong();
        ClientEvents.TICK_END.register(tick -> {
            long count = clientTicks.incrementAndGet();
            if (count % 600 == 0) {
                fenix.logger().info("client tick {}", count);
            }
        });
    }
}
