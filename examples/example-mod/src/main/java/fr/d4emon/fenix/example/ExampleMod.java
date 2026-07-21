package fr.d4emon.fenix.example;

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Mod;
import fr.d4emon.fenix.event.BlockEvents;
import fr.d4emon.fenix.event.Flow;
import fr.d4emon.fenix.event.ServerEvents;
import net.minecraft.world.level.block.Blocks;

/**
 * The smallest useful Fenix mod: one class, a couple of listeners.
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

        ServerEvents.STARTED.register(started ->
                fenix.logger().info("the world is up: {}", started.server().getWorldData().getLevelName()));

        // Diamond blocks cannot be broken. On the server, so it actually holds:
        // a modified client cannot route around this.
        BlockEvents.BREAK.register(event -> {
            if (event.level().getBlockState(event.pos()).is(Blocks.DIAMOND_BLOCK)) {
                fenix.logger().info("{} tried to break a diamond block at {}",
                        event.player().getName().getString(), event.pos());
                return Flow.CANCEL;
            }
            return Flow.CONTINUE;
        });
    }
}
