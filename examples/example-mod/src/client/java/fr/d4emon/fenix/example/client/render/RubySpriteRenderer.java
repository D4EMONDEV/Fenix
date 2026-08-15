package fr.d4emon.fenix.example.client.render;

import fr.d4emon.fenix.example.entity.RubySprite;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws a ruby sprite.
 *
 * <p>The three type parameters are the entity, the render state, and the model.
 * The state is the middle step 26.x added: the renderer copies what it needs off
 * the entity on the game thread, then draws from the copy on the render thread,
 * so nothing reads a moving entity while it is being drawn.
 *
 * <p>Nothing here is Fenix's — this is the same class a Fabric or NeoForge mod
 * would write. What Fenix provides is a way to say the renderer exists, and a
 * layer for its model to bake from; without those a mod can only reuse a vanilla
 * renderer.
 */
public class RubySpriteRenderer extends MobRenderer<RubySprite, LivingEntityRenderState,
        RubySpriteModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("example-mod", "textures/entity/ruby_sprite.png");

    /** Built by the game, once, when it loads its renderers. */
    public RubySpriteRenderer(EntityRendererProvider.Context context) {
        // The shadow is a radius in blocks, and roughly half the entity's width
        // is what reads as sitting on the ground rather than floating.
        super(context, new RubySpriteModel(context.bakeLayer(ModEntityModels.RUBY_SPRITE)), 0.3f);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
