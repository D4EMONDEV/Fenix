package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.CreativePages;
import fr.d4emon.fenix.registry.client.CreativePageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
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
 * Puts the page arrows on the creative screen.
 *
 * <p>They live in the strip above the item grid, right of the title and above
 * the scroll bar: the panel is 195 by 136, the scroll bar sits at
 * {@code leftPos + 175} with its track starting at {@code topPos + 18}, and the
 * search box — drawn only on the search tab, but drawn — ends at
 * {@code leftPos + 162}. That leaves one free strip, and the pair fits it.
 *
 * <p>They are added as real widgets rather than drawn by hand, so hovering,
 * clicking, focus and narration are the screen's job rather than this class's.
 * Page Up and Page Down do the same thing, because reaching for the mouse to
 * change page is the kind of thing that becomes tiring by the tenth time.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeScreenMixin
        extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

    /** GLFW's page keys, the same pair Fabric bound and players already try. */
    private static final int KEY_PAGE_UP = 266;
    private static final int KEY_PAGE_DOWN = 267;

    /** Distance from the panel's right edge to the forward arrow. */
    private static final int RIGHT_INSET = 20;

    /** Distance from the panel's top edge, leaving the arrows flush with the grid. */
    private static final int TOP_INSET = 4;

    /** Gap between the two, enough that the pair reads as two controls. */
    private static final int SPACING = 12;

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

    /**
     * Declared on the target itself, so this one does have to be shadowed.
     *
     * @param tab the tab to open
     */
    @Shadow
    protected abstract void selectTab(CreativeModeTab tab);

    @Inject(method = "init", at = @At("TAIL"))
    private void fenix$addPageButtons(CallbackInfo info) {
        if (CreativePages.count() <= 1) {
            // One page is the normal case, with no mods adding tabs. Nothing
            // should appear that the player then has to wonder about.
            return;
        }
        int forwardX = leftPos + imageWidth - RIGHT_INSET;
        int y = topPos + TOP_INSET;

        addRenderableWidget(new CreativePageButton(
                forwardX - SPACING, y, false, button -> fenix$turn(-1)));
        addRenderableWidget(new CreativePageButton(
                forwardX, y, true, button -> fenix$turn(1)));
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void fenix$pageKeys(KeyEvent event, CallbackInfoReturnable<Boolean> info) {
        if (CreativePages.count() <= 1) {
            return;
        }
        if (event.key() == KEY_PAGE_UP) {
            fenix$turn(-1);
        } else if (event.key() == KEY_PAGE_DOWN) {
            fenix$turn(1);
        } else {
            return;
        }
        info.setReturnValue(true);
    }

    private void fenix$turn(int delta) {
        CreativePages.turn(delta);

        // The tab that was open may belong to the page we just left, and the
        // screen would otherwise keep drawing its contents with no button to
        // match. Search and the inventory follow the player across pages, so
        // prefer a tab that is actually new here.
        List<CreativeModeTab> tabs = CreativeModeTabs.tabs();
        if (!tabs.isEmpty()) {
            selectTab(tabs.stream()
                    .filter(tab -> CreativePages.pageOf(tab) == CreativePages.current())
                    .findFirst()
                    .orElse(tabs.getFirst()));
        }
    }
}
