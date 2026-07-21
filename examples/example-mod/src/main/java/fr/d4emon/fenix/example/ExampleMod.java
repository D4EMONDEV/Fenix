package fr.d4emon.fenix.example;

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Mod;
import fr.d4emon.fenix.event.BlockEvents;
import fr.d4emon.fenix.event.EntityEvents;
import fr.d4emon.fenix.event.PlayerEvents;
import fr.d4emon.fenix.example.content.ModCommands;
import fr.d4emon.fenix.example.content.ModContent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import fr.d4emon.fenix.event.Flow;
import fr.d4emon.fenix.event.ServerEvents;
import fr.d4emon.fenix.example.content.ModBlocks;
import fr.d4emon.fenix.example.content.ModContent;

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
    public void onRegister(Fenix fenix) {
        ModContent.register();
        fenix.logger().info("registered {} and friends", ModBlocks.RUBY_BLOCK.id());
    }

    @Override
    public void onInit(Fenix fenix) {
        // Commands are announced through the event bus, so registering the
        // listener once here is enough — the server fires it on start and on
        // every datapack reload.
        ModCommands.register();

        fenix.logger().info("Example mod loaded — Fenix {}, {} side",
                fenix.loaderVersion(), fenix.side());

        ServerEvents.STARTED.register(started ->
                fenix.logger().info("the world is up: {}", started.server().getWorldData().getLevelName()));

        // This mod's own ruby blocks cannot be broken. On the server, so it
        // actually holds: a modified client cannot route around this.
        BlockEvents.BREAK.register(event -> {
            if (event.level().getBlockState(event.pos()).is(ModBlocks.RUBY_BLOCK.get())) {
                fenix.logger().info("{} tried to break a ruby block at {}",
                        event.player().getName().getString(), event.pos());
                return Flow.CANCEL;
            }
            return Flow.CONTINUE;
        });

        // Greeting a player needs the moment they can actually be sent
        // something, which is what JOINED means and why it is not the same as
        // the server having started.
        PlayerEvents.JOINED.register(joined -> joined.player().sendSystemMessage(
                Component.literal("This world runs the Fenix example mod.")));

        // A player who died lost their tally blocks' contents to nobody in
        // particular; this is only here to show the event carries the cause.
        PlayerEvents.DIED.register(died -> fenix.logger().info("{} died: {}",
                died.player().getName().getString(),
                died.cause().getLocalizedDeathMessage(died.player()).getString()));

        // Cancelling a spawn keeps the entity out of the world entirely, rather
        // than removing it a tick later once everyone has seen it.
        EntityEvents.SPAWNING.register(spawning ->
                spawning.entity().getType() == ModContent.RUBY_WISP.get()
                        && spawning.level().getDifficulty() == Difficulty.PEACEFUL
                        ? Flow.CANCEL
                        : Flow.CONTINUE);
    }
}
