package fr.d4emon.fenix.example;

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Mod;
import fr.d4emon.fenix.config.Config;
import fr.d4emon.fenix.event.BlockEvents;
import fr.d4emon.fenix.event.EntityEvents;
import fr.d4emon.fenix.event.LevelEvents;
import fr.d4emon.fenix.event.PlayerEvents;
import fr.d4emon.fenix.example.command.ModCommands;
import fr.d4emon.fenix.example.config.ModConfig;
import fr.d4emon.fenix.example.registry.ModContent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import fr.d4emon.fenix.event.Flow;
import fr.d4emon.fenix.event.ServerEvents;
import fr.d4emon.fenix.example.registry.ModBlocks;

/**
 * The smallest useful Fenix mod: one class, a couple of listeners.
 *
 * <p>Nothing in {@code fenix.mod.json} points here — the {@link Mod} annotation
 * is the declaration, and the annotation processor records it while this
 * compiles. Rename or mistype it and the build fails, not the launch.
 */
@Mod("example-mod")
public final class ExampleMod implements FenixMod {

    /** Read once in onInit, and never null after it. */
    private Config<ModConfig> config;

    /** Instantiated by the loader from the compile-time index. */
    public ExampleMod() {
    }

    /**
     * Watches each level rather than the server as a whole.
     *
     * <p>A server has one lifecycle and several levels, and a mod keeping
     * per-world state cares about the second: the overworld, the nether and the
     * end are loaded and saved separately, and something saved on server stop
     * is saved once for all three.
     */
    private static void watchLevels(Fenix fenix) {
        LevelEvents.LOADED.register(loaded ->
                fenix.logger().info("level {} is loaded",
                        loaded.level().dimension().identifier()));

        LevelEvents.SAVING.register(saving ->
                fenix.logger().debug("level {} is being saved",
                        saving.level().dimension().identifier()));
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
        // Loaded once, here: the file exists by now and reading it is cheap
        // enough to do at startup rather than on every use.
        config = Config.of(fenix, ModConfig.DEFAULTS);

        ModCommands.register();

        fenix.logger().info("Example mod loaded - Fenix {}, {} side",
                fenix.loaderVersion(), fenix.side());

        ServerEvents.STARTED.register(started ->
                fenix.logger().info("the world is up: {}", started.server().getWorldData().getLevelName()));

        watchLevels(fenix);

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
                Component.literal(config.get().greeting())));

        // A player who died lost their tally blocks' contents to nobody in
        // particular; this is only here to show the event carries the cause.
        PlayerEvents.DIED.register(died -> fenix.logger().info("{} died: {}",
                died.player().getName().getString(),
                died.cause().getLocalizedDeathMessage(died.player()).getString()));

        // Cancelling a spawn keeps the entity out of the world entirely, rather
        // than removing it a tick later once everyone has seen it.
        EntityEvents.SPAWNING.register(spawning ->
                !config.get().spawnWispsOnPeaceful()
                        && spawning.entity().getType() == ModContent.RUBY_WISP.get()
                        && spawning.level().getDifficulty() == Difficulty.PEACEFUL
                        ? Flow.CANCEL
                        : Flow.CONTINUE);
    }
}
