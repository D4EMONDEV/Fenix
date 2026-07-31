package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.client.FluidRendering;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Lets a mod's fluids be drawn.
 *
 * <p>26.2 stopped hardcoding water and lava sprites inside the fluid renderer
 * and moved them into a small map, {@code Fluid -> FluidModel}, baked once when
 * the atlases are — a real improvement, except the map is built by
 * {@code Map.of(WATER, …, LAVA, …)} and holds exactly those two. A fluid not in
 * it falls back to the missing-texture model, which is why a modded fluid
 * without this shows as a checkerboard.
 *
 * <p>So this bakes every fluid a mod registered — with the same
 * {@code MaterialBaker} vanilla just used, so the sprites come off the same
 * atlas — and returns a map that carries them alongside water and lava.
 */
@Mixin(FluidStateModelSet.class)
public class FluidStateModelSetMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    FluidStateModelSetMixin() {
    }

    @Inject(method = "bake", at = @At("RETURN"), cancellable = true)
    private static void fenix$bakeModdedFluids(MaterialBaker materials,
                                               CallbackInfoReturnable<Map<Fluid, FluidModel>> info) {
        Map<Fluid, FluidModel.Unbaked> declared = FluidRendering.models();
        if (declared.isEmpty()) {
            return;
        }
        // Vanilla returns an immutable Map.of; copy it before adding, or the put
        // below throws and takes fluid rendering down with it.
        Map<Fluid, FluidModel> merged = new HashMap<>(info.getReturnValue());
        declared.forEach((fluid, unbaked) ->
                merged.put(fluid, unbaked.bake(materials, fluid::toString)));
        info.setReturnValue(merged);
    }
}
