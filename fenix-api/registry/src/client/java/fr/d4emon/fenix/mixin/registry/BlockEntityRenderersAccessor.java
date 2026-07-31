package fr.d4emon.fenix.mixin.registry;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Reaches the table of block entity renderers, whose own {@code register} is
 * private.
 *
 * <p>The map itself is mutable — like the entity renderer table and unlike the
 * attribute one — so this only has to hand it over.
 */
@Mixin(BlockEntityRenderers.class)
public interface BlockEntityRenderersAccessor {

    /** {@return the live provider table} */
    @Accessor("PROVIDERS")
    static Map<BlockEntityType<?>, BlockEntityRendererProvider<?, ?>> fenix$providers() {
        throw new AssertionError("mixin did not apply");
    }
}
