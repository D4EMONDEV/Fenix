package fr.d4emon.fenix.registry.client;

import fr.d4emon.fenix.mixin.registry.BlockEntityRenderersAccessor;
import fr.d4emon.fenix.registry.Holder;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Objects;

/**
 * Says how a block entity draws itself.
 *
 * <pre>{@code
 * BlockEntityRendering.register(ModContent.RUBY_SAFE_ENTITY, SafeRenderer::new);
 * }</pre>
 *
 * <p>For the part of a block that a block model cannot express: a chest's lid
 * opening, a sign's text, an item spinning above a pedestal. The block itself
 * still renders from its model — this is what draws on top of it, every frame,
 * with the block entity's own state to hand.
 *
 * <p>A block entity with no renderer is not an error and says nothing: the block
 * is there, its model draws, and whatever the block entity was meant to show
 * simply never appears.
 *
 * <p>Client-only, so call it behind a side check — {@code fenix.side()} — or
 * from a class the dedicated server never loads.
 */
public final class BlockEntityRendering {

    private BlockEntityRendering() {
    }

    /**
     * Registers the renderer for a block entity type.
     *
     * @param <T>      the block entity class
     * @param <S>      the render state it builds each frame
     * @param type     the type, already registered
     * @param renderer builds the renderer, given the client's render context
     */
    public static <T extends BlockEntity, S extends BlockEntityRenderState> void register(
            Holder<BlockEntityType<T>> type, BlockEntityRendererProvider<T, S> renderer) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(renderer, "renderer");
        BlockEntityRenderersAccessor.fenix$providers().put(type.get(), renderer);
    }
}
