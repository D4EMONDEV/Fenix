package fr.d4emon.fenix.example.content.client;

import fr.d4emon.fenix.example.content.ModContent;
import fr.d4emon.fenix.registry.client.EntityRendering;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/**
 * How this mod's entities are drawn.
 *
 * <p>In a {@code .client} package, and only ever called from behind a side
 * check: naming a {@code net.minecraft.client} type is what makes a class
 * unloadable on a dedicated server, and the guard has to be a method boundary
 * for that to hold.
 */
public final class ModRendering {

    private ModRendering() {
    }

    /** Registers the renderers. Client only. */
    public static void register() {
        // Vanilla's item renderer, which is all a wisp needs — no model file.
        EntityRendering.register(ModContent.RUBY_WISP, ThrownItemRenderer::new);
    }
}
