package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.CreativePages;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Draws the arrows that turn creative pages, at the top right of the panel.
 *
 * <p>The sprites are vanilla's own recipe-book arrows, so this needs no texture
 * of its own and matches whatever resource pack the player is using.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeScreenMixin
        extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

    /**
     * Never called — a mixin's constructors are discarded when it is merged.
     * It exists because the panel's position lives on the superclass, and
     * {@link Shadow} only looks at the target class itself, so this mixin has
     * to sit in the same place in the hierarchy to see those fields at all.
     */
    private CreativeScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu,
                                Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /** The recipe book's arrows are 12 by 17. */
    private static final int ARROW_WIDTH = 12;
    private static final int ARROW_HEIGHT = 17;

    /**
     * Where the pair sits, measured from the panel's top right corner.
     *
     * <p>These are not eyeballed. The panel is 195 by 136, its scrollbar sits
     * at {@code leftPos + 175} with its track starting at {@code topPos + 18},
     * and the search box — drawn only on the search tab, but drawn — ends at
     * {@code leftPos + 162}. That leaves exactly one free strip: 24 pixels
     * wide, 18 tall, above the scrollbar and right of the title. Two arrows
     * fill it precisely.
     */
    private static final int RIGHT_INSET = 20;
    private static final int TOP_INSET = 1;

    /** Flush, so the pair reads as one control rather than two. */
    private static final int SPACING = 12;

    private static final Identifier BACKWARD =
            Identifier.withDefaultNamespace("recipe_book/page_backward");
    private static final Identifier BACKWARD_HIGHLIGHTED =
            Identifier.withDefaultNamespace("recipe_book/page_backward_highlighted");
    private static final Identifier FORWARD =
            Identifier.withDefaultNamespace("recipe_book/page_forward");
    private static final Identifier FORWARD_HIGHLIGHTED =
            Identifier.withDefaultNamespace("recipe_book/page_forward_highlighted");

    // leftPos, topPos and imageWidth need no @Shadow: extending the real
    // superclass means they are simply inherited.

    /**
     * Declared on the target itself, so this one does have to be shadowed.
     *
     * @param tab the tab to open
     */
    @Shadow
    protected abstract void selectTab(CreativeModeTab tab);

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void fenix$drawArrows(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                  float partialTick, CallbackInfo info) {
        if (CreativePages.count() <= 1) {
            return;
        }
        int y = fenix$arrowY();
        int back = fenix$backwardX();
        int forward = fenix$forwardX();

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                fenix$over(back, mouseX, mouseY) ? BACKWARD_HIGHLIGHTED : BACKWARD,
                back, y, ARROW_WIDTH, ARROW_HEIGHT);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                fenix$over(forward, mouseX, mouseY) ? FORWARD_HIGHLIGHTED : FORWARD,
                forward, y, ARROW_WIDTH, ARROW_HEIGHT);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void fenix$clickArrows(MouseButtonEvent event, boolean doubled,
                                   CallbackInfoReturnable<Boolean> info) {
        if (CreativePages.count() <= 1) {
            return;
        }
        int x = (int) event.x();
        int y = (int) event.y();

        if (fenix$over(fenix$backwardX(), x, y)) {
            fenix$turn(-1);
        } else if (fenix$over(fenix$forwardX(), x, y)) {
            fenix$turn(1);
        } else {
            return;
        }
        // Swallowed, or the click also lands on whatever is behind the arrow.
        info.setReturnValue(true);
    }

    private void fenix$turn(int delta) {
        CreativePages.turn(delta);
        // The tab that was open is on the page we just left. Something has to
        // be selected, and the screen would otherwise keep drawing contents
        // belonging to a tab it no longer shows a button for.
        List<CreativeModeTab> tabs = CreativeModeTabs.tabs();
        if (!tabs.isEmpty()) {
            selectTab(tabs.getFirst());
        }
    }

    private int fenix$backwardX() {
        return leftPos + imageWidth - RIGHT_INSET - SPACING;
    }

    private int fenix$forwardX() {
        return leftPos + imageWidth - RIGHT_INSET;
    }

    private int fenix$arrowY() {
        return topPos + TOP_INSET;
    }

    private boolean fenix$over(int arrowX, int mouseX, int mouseY) {
        int y = fenix$arrowY();
        return mouseX >= arrowX && mouseX < arrowX + ARROW_WIDTH
                && mouseY >= y && mouseY < y + ARROW_HEIGHT;
    }
}
