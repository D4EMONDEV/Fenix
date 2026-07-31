package fr.d4emon.fenix.registry.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Says what shape a mod's entity is.
 *
 * <pre>{@code
 * public static final ModelLayerLocation WISP =
 *         EntityModels.layer(Identifier.parse("example-mod:wisp"));
 *
 * EntityModels.register(WISP, WispModel::createBodyLayer);
 * }</pre>
 *
 * <p>An entity renderer that builds its own model asks the game to bake a layer,
 * and the game bakes from a table built once, from a fixed list of vanilla's
 * own. A layer that is not in it fails outright —
 * {@code IllegalArgumentException: No model for layer} — from inside the
 * renderer's constructor, which is to say while the client is loading, not while
 * a mod is doing anything a stack trace would name.
 *
 * <p>Until this existed a mod could only reuse a vanilla renderer, because there
 * was no way to add a layer at all.
 *
 * <p>Client-only, and read while the client bakes models, so register from
 * {@code onRegister} — that runs first.
 */
public final class EntityModels {

    // Ordered so a mod's layers bake in the order they were declared, which is
    // the order a mod author reads them in when one of them fails.
    private static final Map<ModelLayerLocation, Supplier<LayerDefinition>> LAYERS =
            new LinkedHashMap<>();

    private EntityModels() {
    }

    /**
     * {@return a layer name for an entity's main body}
     *
     * <p>Vanilla names most of its own {@code "main"}, and only entities with
     * several parts — an outer skin, a saddle — use anything else.
     *
     * @param entity the entity's id
     */
    public static ModelLayerLocation layer(Identifier entity) {
        return layer(entity, "main");
    }

    /**
     * {@return a layer name}
     *
     * @param entity the entity's id
     * @param layer  which layer of it, for an entity that has several
     */
    public static ModelLayerLocation layer(Identifier entity, String layer) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(layer, "layer");
        return new ModelLayerLocation(entity, layer);
    }

    /**
     * Registers the shape behind a layer.
     *
     * @param layer      the layer name, from {@link #layer(Identifier)}
     * @param definition builds the geometry; called once, when the client bakes
     *                   its models, and again on every resource reload
     */
    public static void register(ModelLayerLocation layer, Supplier<LayerDefinition> definition) {
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(definition, "definition");
        synchronized (LAYERS) {
            LAYERS.put(layer, definition);
        }
    }

    /** {@return the layers mods declared — for the mixin that bakes them} */
    public static Map<ModelLayerLocation, Supplier<LayerDefinition>> declared() {
        synchronized (LAYERS) {
            return Map.copyOf(LAYERS);
        }
    }
}
