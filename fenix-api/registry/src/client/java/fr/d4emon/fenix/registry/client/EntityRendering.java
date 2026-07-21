package fr.d4emon.fenix.registry.client;

import fr.d4emon.fenix.mixin.registry.EntityRenderersAccessor;
import fr.d4emon.fenix.registry.Holder;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Objects;

/**
 * Says how an entity looks.
 *
 * <pre>{@code
 * EntityRendering.register(ModEntities.WISP, ThrownItemRenderer::new);
 * }</pre>
 *
 * <p>An entity with no renderer is invisible: it is there, it ticks, it can be
 * hit, and nothing is drawn. Vanilla only warns about it once, in the log,
 * where it is easy to miss.
 *
 * <p>Client-only, so call it behind a side check — {@code fenix.side()} — or
 * from a class the server never loads. Registering renderers from
 * {@code onRegister} is early enough; the client builds its renderers later.
 */
public final class EntityRendering {

    private EntityRendering() {
    }

    /**
     * Registers the renderer for an entity type.
     *
     * @param <T>      the entity class
     * @param type     the type, already registered
     * @param renderer builds the renderer, given the client's render context
     */
    public static <T extends Entity> void register(Holder<EntityType<T>> type,
                                                   EntityRendererProvider<T> renderer) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(renderer, "renderer");
        // Vanilla's own register is private, and its table — unlike the
        // attribute one — is mutable, so this can simply add to it.
        EntityRenderersAccessor.fenix$providers().put(type.get(), renderer);
    }
}
