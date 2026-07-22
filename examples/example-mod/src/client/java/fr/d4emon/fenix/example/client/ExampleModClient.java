package fr.d4emon.fenix.example.client;

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Mod;
import fr.d4emon.fenix.example.content.ModContent;
import fr.d4emon.fenix.example.content.ModPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import fr.d4emon.fenix.registry.client.EntityRendering;
import fr.d4emon.fenix.registry.client.MenuScreens;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/**
 * The client half of the mod.
 *
 * <p>Same {@code @Mod} annotation and same interface as the common half — what
 * makes this one client-only is where it lives. Everything under
 * {@code src/client/java} is indexed apart, and a dedicated server is never
 * told the class exists.
 *
 * <p>This half may use the common half; the reverse is a compile error. That is
 * the right way round: a mod is written in {@code src/main} and reached into
 * from here, mostly to say how it looks.
 */
@Mod("example-mod")
public final class ExampleModClient implements FenixMod {

    /** Instantiated by the loader. */
    public ExampleModClient() {
    }

    @Override
    public void onRegister(Fenix fenix) {
        // Runs after the common half, so the entity type is already bound.
        // Vanilla's item renderer is all a wisp needs — no model file.
        EntityRendering.register(ModContent.RUBY_WISP, ThrownItemRenderer::new);

        // Keys are client-only, and registered here rather than beside the
        // content: onRegister runs before the game builds its options, which
        // is exactly when the list has to be complete.
        ModKeys.listen();

        // The other half of a menu: the server opens the window, this says what
        // the player sees when it opens.
        MenuScreens.register(ModContent.RUBY_SAFE_MENU, RubySafeScreen::new);

        // The other half of the tally block. Showing it needs the client, so
        // the handler belongs here rather than beside the channel.
        ModPayloads.TALLY.receive(tally -> {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.sendSystemMessage(Component.literal(
                        "Tally at " + tally.pos().toShortString() + ": " + tally.count()));
            }
        });
    }
}
