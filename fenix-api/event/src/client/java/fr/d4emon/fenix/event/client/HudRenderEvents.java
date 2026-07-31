package fr.d4emon.fenix.event.client;

import fr.d4emon.fenix.event.Event;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Drawing on top of the heads-up display.
 *
 * <pre>{@code
 * HudRenderEvents.RENDER.register(hud -> {
 *     hud.graphics().text(font, "mana: " + mana, 4, 4, 0xFFFFFF);
 * });
 * }</pre>
 *
 * <p>Fires after vanilla has laid out the hotbar, health, effects and the rest,
 * so a mod's drawing lands on top of them rather than under.
 *
 * <p>Client only, and only while a world is on screen: this does not fire behind
 * a menu, which is what a mod drawing a status overlay wants.
 */
public final class HudRenderEvents {

    /**
     * The HUD, drawn and ready to be added to.
     *
     * <p>26.2 draws the interface in two passes — one that records what to draw
     * and one that draws it — so what arrives here is the recorder rather than a
     * canvas. It is used the same way; the difference is that nothing happens at
     * the moment a call is made.
     *
     * @param graphics what to record drawing into
     * @param delta    how far between ticks this frame is, for anything animated
     */
    public record Render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
    }

    /** Fires once a frame, after vanilla's own HUD. */
    public static final Event<Render> RENDER = Event.create();

    private HudRenderEvents() {
    }
}
