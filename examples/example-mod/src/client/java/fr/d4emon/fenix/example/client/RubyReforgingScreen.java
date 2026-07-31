package fr.d4emon.fenix.example.client;

import fr.d4emon.fenix.example.content.RubyReforgingMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * What the reforging table looks like.
 *
 * <p>Borrows the furnace panel: a small machine with an input high on the left
 * and an output on the right is the shape it was drawn for, and the menu placed
 * its two slots to line up with it. The flame, arrow and lower slot the texture
 * also draws are vestigial here — a reforging table burns nothing — and are the
 * price of reusing a texture rather than shipping one that the player's resource
 * pack would then stop theming.
 */
public final class RubyReforgingScreen extends AbstractContainerScreen<RubyReforgingMenu> {

    private static final Identifier BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/furnace.png");

    /**
     * Built by Fenix when the server opens the window.
     *
     * @param menu      the client's half of the window
     * @param inventory the player's inventory
     * @param title     the name the block gave itself
     */
    public RubyReforgingScreen(RubyReforgingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        super.extractBackground(graphics, mouseX, mouseY, partial);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 0f, 0f,
                imageWidth, imageHeight, 256, 256);
    }
}
