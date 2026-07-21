package fr.d4emon.fenix.mixin.registry;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Reaches vanilla's table of entity renderers, whose own {@code register} is
 * private.
 *
 * <p>Unlike the attribute table this one is mutable, so the map itself can be
 * added to. An entity missing from it is invisible in game, and vanilla only
 * warns about it in the log.
 */
@Mixin(EntityRenderers.class)
public interface EntityRenderersAccessor {

    /** {@return vanilla's table, which can be added to} */
    @Accessor("PROVIDERS")
    static Map<EntityType<?>, EntityRendererProvider<?>> fenix$providers() {
        throw new AssertionError("replaced by Mixin");
    }
}
