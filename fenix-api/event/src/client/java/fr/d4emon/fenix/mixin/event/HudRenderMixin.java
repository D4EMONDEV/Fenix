package fr.d4emon.fenix.mixin.event;

import fr.d4emon.fenix.event.client.HudRenderEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link HudRenderEvents#RENDER} once vanilla's HUD is laid out.
 *
 * <p>Taken on {@code Hud} rather than on {@code Gui}, which is where the HUD
 * pass looks like it starts. {@code Gui.extractRenderState} builds the graphics
 * object as a local and hands it down, so reaching it there would mean capturing
 * a local; one level in, {@code Hud.extractRenderState} takes the very same
 * object as a parameter. Same moment, no capture.
 *
 * <p>At the tail, so a mod draws over the hotbar and health rather than under
 * them — and inside the branch vanilla only runs while a world is on screen, so
 * nothing fires behind a menu.
 */
@Mixin(Hud.class)
public class HudRenderMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    HudRenderMixin() {
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void fenix$hudRender(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
                                 CallbackInfo info) {
        HudRenderEvents.RENDER.fire(new HudRenderEvents.Render(graphics, deltaTracker));
    }
}
