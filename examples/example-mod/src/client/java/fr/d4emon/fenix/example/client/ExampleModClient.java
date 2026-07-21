package fr.d4emon.fenix.example.client;

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Mod;
import fr.d4emon.fenix.example.content.ModContent;
import fr.d4emon.fenix.registry.client.EntityRendering;
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
    }
}
