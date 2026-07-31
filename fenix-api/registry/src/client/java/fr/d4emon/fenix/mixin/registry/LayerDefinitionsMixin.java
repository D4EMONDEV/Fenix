package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.client.EntityModels;
import net.minecraft.client.model.geom.LayerDefinitions;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Lets a mod's entities have models of their own.
 *
 * <p>Vanilla builds the whole table of model layers in one method, from a fixed
 * list, and returns it as an immutable map. There is nothing to add to
 * afterwards, so a mod's layer never exists and the renderer that asks for it
 * throws {@code No model for layer} while the client is loading.
 *
 * <p>Merged at the return rather than by replacing the method: everything
 * vanilla built stays exactly as built, and a mod's entries go in beside it.
 * Rebuilt on every resource reload, which is when this runs again.
 */
@Mixin(LayerDefinitions.class)
public class LayerDefinitionsMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    LayerDefinitionsMixin() {
    }

    @Inject(method = "createRoots", at = @At("RETURN"), cancellable = true)
    private static void fenix$addModLayers(
            CallbackInfoReturnable<Map<ModelLayerLocation, LayerDefinition>> info) {
        Map<ModelLayerLocation, Supplier<LayerDefinition>> declared = EntityModels.declared();
        if (declared.isEmpty()) {
            return;
        }
        // Vanilla returns an ImmutableMap; copy before adding, or the put below
        // throws and takes every entity model down with it.
        Map<ModelLayerLocation, LayerDefinition> merged = new HashMap<>(info.getReturnValue());
        declared.forEach((layer, definition) -> merged.put(layer, definition.get()));
        info.setReturnValue(merged);
    }
}
