package fr.d4emon.fenix.example.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.d4emon.fenix.example.block.entity.RubyTallyBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the tally's count on the top face of the block.
 *
 * <p>The one client API a mod can register that the demo never used. Everything
 * else it draws — the sprite, the particle, the fluid, the screens — goes
 * through a different door, and a block entity that wants to draw something of
 * its own goes through this one.
 *
 * <p>The number is on the block's face rather than floating and turning to
 * follow the player. Billboarding needs the camera, and this is a demonstration
 * of where a renderer plugs in, not of how to do trigonometry.
 */
public final class RubyTallyRenderer
        implements BlockEntityRenderer<RubyTallyBlockEntity, RubyTallyRenderer.State> {

    /**
     * What one frame needs to know.
     *
     * <p>Separate from the block entity on purpose: 26.2 extracts a state on
     * the client thread and draws from it later, so a renderer that read the
     * block entity while drawing would be reading something another thread is
     * writing.
     */
    public static final class State extends BlockEntityRenderState {

        /** How many times the block has been hit. */
        public int count;

        /** Built by the renderer, once, and refilled each frame. */
        public State() {
        }
    }

    private final Font font;

    /**
     * @param context the client's rendering context, which owns the font
     */
    public RubyTallyRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RubyTallyBlockEntity tally, State state, float partialTick,
                                   Vec3 camera, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderState.extractBase(tally, state, overlay);
        state.count = tally.count();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        String text = String.valueOf(state.count);

        pose.pushPose();
        // Just above the top face: at exactly 1.0 the text fights the block for
        // the same depth and flickers.
        pose.translate(0.5f, 1.002f, 0.5f);
        pose.mulPose(Axis.XP.rotationDegrees(90f));
        // Negative, because text is drawn with y growing downwards and the
        // block's own axes are the other way up.
        pose.scale(-0.025f, -0.025f, 0.025f);

        collector.submitText(pose, -font.width(text) / 2f, -4f,
                Component.literal(text).getVisualOrderText(), false,
                Font.DisplayMode.POLYGON_OFFSET, state.lightCoords,
                0xFFE86C88, 0, 0);
        pose.popPose();
    }
}
