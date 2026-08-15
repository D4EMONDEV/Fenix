package fr.d4emon.fenix.example.client.render;

import fr.d4emon.fenix.registry.client.EntityModels;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

/** The model layers this mod's renderers bake from. */
public final class ModEntityModels {

    /**
     * The sprite's own layer.
     *
     * <p>A layer is a name the game bakes a mesh under. Vanilla builds its table
     * from a fixed list of its own, so a mod's layer has to be added to it —
     * which is what {@link EntityModels#register} does. Without that, baking
     * throws {@code No model for layer} from inside the renderer's constructor.
     */
    public static final ModelLayerLocation RUBY_SPRITE =
            EntityModels.layer(Identifier.fromNamespaceAndPath("example-mod", "ruby_sprite"));

    private ModEntityModels() {
    }

    /** Declares every layer. Called from the client mod's onRegister. */
    public static void register() {
        EntityModels.register(RUBY_SPRITE, RubySpriteModel::createBodyLayer);
    }
}
