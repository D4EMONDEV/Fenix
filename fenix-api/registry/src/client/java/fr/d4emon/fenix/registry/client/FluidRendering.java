package fr.d4emon.fenix.registry.client;

import fr.d4emon.fenix.registry.fluid.FluidResult;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Says what a fluid looks like.
 *
 * <pre>{@code
 * FluidRendering.register(ModFluids.ACID,
 *         Identifier.parse("mymod:block/acid_still"),
 *         Identifier.parse("mymod:block/acid_flow"));
 * }</pre>
 *
 * <p>Both textures name sprites on the <em>block</em> atlas, the same one water
 * and lava use — so the two files belong under {@code textures/block/} and have
 * to be stitched into that atlas, which a mod does with an
 * {@code atlases/blocks.json} that includes them.
 *
 * <p>A fluid with no rendering registered draws with the missing-texture
 * checkerboard rather than vanishing, which at least says out loud that this
 * step was skipped. Client-only: call it behind a side check, or from a class
 * the dedicated server never loads.
 *
 * <p>The other half of what 26.2 hardcodes for water and lava — that both the
 * still and flowing forms share one model — is handled here: registering the
 * result registers both.
 */
public final class FluidRendering {

    // Read on the render thread while the atlas bakes, written from onRegister;
    // a concurrent map keeps that honest without a lock the callers would have
    // to know about.
    private static final Map<Fluid, FluidModel.Unbaked> MODELS = new ConcurrentHashMap<>();

    private FluidRendering() {
    }

    /**
     * Registers a plain fluid: a still texture and a flowing one, no tint.
     *
     * @param fluid        the fluid, already registered
     * @param stillTexture the sprite for the still fluid, on the block atlas
     * @param flowTexture  the sprite for the flowing fluid, on the block atlas
     */
    public static void register(FluidResult fluid, Identifier stillTexture, Identifier flowTexture) {
        register(fluid, stillTexture, flowTexture, null, null);
    }

    /**
     * Registers a fluid with an edge overlay and a flat tint.
     *
     * @param fluid        the fluid, already registered
     * @param stillTexture the sprite for the still fluid
     * @param flowTexture  the sprite for the flowing fluid
     * @param overlay      the sprite shown where the fluid meets a solid face
     *                     from the side, as water has behind glass; {@code null}
     *                     for none
     * @param tint         an ARGB colour multiplied into every sprite, the way
     *                     water is tinted by biome; {@code null} to leave the
     *                     textures their own colour
     */
    public static void register(FluidResult fluid, Identifier stillTexture, Identifier flowTexture,
                                @Nullable Identifier overlay, @Nullable Integer tint) {
        Objects.requireNonNull(fluid, "fluid");
        Objects.requireNonNull(stillTexture, "stillTexture");
        Objects.requireNonNull(flowTexture, "flowTexture");

        FluidModel.Unbaked model = new FluidModel.Unbaked(
                new Material(stillTexture),
                new Material(flowTexture),
                overlay != null ? new Material(overlay) : null,
                tint != null ? BlockTintSources.constant(tint) : null);

        // Both forms share one model, exactly as vanilla maps WATER and
        // FLOWING_WATER to the same one. Resolving the handles is safe here:
        // rendering is registered from onRegister, by which point the fluids are
        // bound.
        MODELS.put(fluid.source().get(), model);
        MODELS.put(fluid.flowing().get(), model);
    }

    /** {@return the models mods have registered — for the mixin that bakes them} */
    public static Map<Fluid, FluidModel.Unbaked> models() {
        return MODELS;
    }
}
