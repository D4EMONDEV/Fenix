package fr.d4emon.fenix.example.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * The shape of a ruby sprite: a body and a head, and nothing else.
 *
 * <p>Geometry written by hand rather than exported from a modelling tool,
 * because the point here is to show what a mod has to provide — a mesh, a layer
 * definition, and the texture coordinates that go with them — not to be a
 * handsome creature.
 *
 * <p>The numbers are the same units vanilla uses: sixteen to a block, measured
 * from the model's origin at the entity's feet.
 */
public class RubySpriteModel extends EntityModel<LivingEntityRenderState> {

    private final ModelPart head;

    /** Built by the renderer from the layer the game baked. */
    public RubySpriteModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
    }

    /**
     * {@return the mesh the game bakes into model parts}
     *
     * <p>Registered against a layer with {@code EntityModels.register}. A layer
     * the game has no definition for fails from inside the renderer's
     * constructor, while the client is loading and a long way from anything a
     * stack trace would name.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // The texture offsets say where on the 32×32 sheet each cube's faces
        // are unwrapped. Getting them wrong is not an error: the cube renders
        // with whatever happens to be at those coordinates.
        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3.0f, 0.0f, -3.0f, 6.0f, 6.0f, 6.0f),
                PartPose.ZERO);

        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 12)
                        .addBox(-2.0f, -4.0f, -2.0f, 4.0f, 4.0f, 4.0f),
                PartPose.offset(0.0f, 6.0f, 0.0f));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        // The head follows where the sprite is looking. Everything else about
        // this model is still; a sprite that bobbed would need a walk cycle,
        // which is a modelling job rather than a loader one.
        head.yRot = state.yRot * ((float) Math.PI / 180.0f);
        head.xRot = state.xRot * ((float) Math.PI / 180.0f);
    }
}
