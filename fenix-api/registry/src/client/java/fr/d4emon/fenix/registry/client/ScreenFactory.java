package fr.d4emon.fenix.registry.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Builds the screen for a menu, when the server says a window has opened.
 *
 * <p>Fenix's own, and public, for the same reason {@code MenuFactory} is:
 * vanilla's equivalent is private, and the widening that lets this module reach
 * it stays this module's business. A mod passing {@code MyScreen::new} would
 * otherwise have to name a type it cannot see.
 *
 * @param <M> the menu class
 * @param <S> the screen class
 */
@FunctionalInterface
public interface ScreenFactory<M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> {

    /**
     * Builds one.
     *
     * @param menu      the client's half of the window
     * @param inventory the player's inventory
     * @param title     the name the menu opened under
     * @return the screen
     */
    S create(M menu, Inventory inventory, Component title);
}
