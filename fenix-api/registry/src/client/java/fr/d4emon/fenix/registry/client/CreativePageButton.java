package fr.d4emon.fenix.registry.client;

import fr.d4emon.fenix.registry.CreativePages;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * One of the two arrows that turn creative pages.
 *
 * <p>A real {@link Button} rather than something drawn by hand, which is what
 * buys the hover state, the click handling, the keyboard focus and the screen
 * reader narration without any of it being written here.
 *
 * <p>Its sprites are Fenix's own, drawn in the palette the creative panel
 * already uses — the same white bevel and grey face as the scroll bar directly
 * below it — so the pair reads as part of the screen rather than bolted onto
 * it.
 */
public final class CreativePageButton extends Button {

    /** Matching the scroll bar's width, which sits directly underneath. */
    public static final int WIDTH = 10;

    /** As tall as the strip above the item grid allows. */
    public static final int HEIGHT = 12;

    private static final Identifier FORWARD = sprite("page_forward");
    private static final Identifier FORWARD_HIGHLIGHTED = sprite("page_forward_highlighted");
    private static final Identifier BACKWARD = sprite("page_backward");
    private static final Identifier BACKWARD_HIGHLIGHTED = sprite("page_backward_highlighted");

    private final boolean forward;

    /**
     * Creates one arrow.
     *
     * @param x       left edge, in screen pixels
     * @param y       top edge, in screen pixels
     * @param forward whether this is the arrow that goes to the next page
     * @param onPress what to do when it is clicked
     */
    public CreativePageButton(int x, int y, boolean forward, OnPress onPress) {
        super(x, y, WIDTH, HEIGHT, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.forward = forward;
    }

    /**
     * Draws the arrow and nothing else.
     *
     * <p>This is the hook rather than {@code extractWidgetRenderState} because
     * that one is final; overriding here is also what skips the vanilla button
     * plate, which would show through underneath.
     */
    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   float partialTick) {
        boolean lit = isHoveredOrFocused();
        Identifier sprite = forward
                ? (lit ? FORWARD_HIGHLIGHTED : FORWARD)
                : (lit ? BACKWARD_HIGHLIGHTED : BACKWARD);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, getX(), getY(), WIDTH, HEIGHT);

        if (isHovered()) {
            // Built per frame rather than once: the page it names changes every
            // time either arrow is pressed, including this one.
            graphics.setTooltipForNextFrame(
                    Component.translatable("fenix.creative.page",
                            CreativePages.current() + 1, CreativePages.count()),
                    mouseX, mouseY);
        }
    }

    private static Identifier sprite(String name) {
        return Identifier.fromNamespaceAndPath("fenix", "creative/" + name);
    }
}
