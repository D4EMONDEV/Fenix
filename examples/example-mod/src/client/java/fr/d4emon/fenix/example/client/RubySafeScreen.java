package fr.d4emon.fenix.example.client;

import fr.d4emon.fenix.example.content.RubySafeMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * What the ruby safe looks like.
 *
 * <p>The menu says which slots exist and where; this says how the panel behind
 * them is drawn. Vanilla's chest texture is borrowed rather than copied: a
 * three-row container is exactly the shape it was drawn for, and a mod that
 * ships its own identical copy is a mod that stops matching the resource pack
 * the player chose.
 */
public final class RubySafeScreen extends AbstractContainerScreen<RubySafeMenu> {

    private static final Identifier BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

    /** Three rows of nine. */
    private static final int ROWS = 3;

    /**
     * Built by Fenix when the server opens the window.
     *
     * @param menu      the client's half of the window
     * @param inventory the player's inventory
     * @param title     the name the block gave itself
     */
    public RubySafeScreen(RubySafeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 114 + ROWS * 18);
        // "Inventory" sits just above the player's own slots, wherever the
        // container above it ends.
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        super.extractBackground(graphics, mouseX, mouseY, partial);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // The texture holds a six-row chest, so it is drawn in two pieces: the
        // rows wanted, then the player's inventory taken from further down it.
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y,
                0f, 0f, imageWidth, ROWS * 18 + 17, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y + ROWS * 18 + 17,
                0f, 126f, imageWidth, 96, 256, 256);
    }
}
