package fr.d4emon.fenix.registry;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Builds a menu on the client, when the server says a window has opened.
 *
 * <p>Fenix's own, and public, so that a mod never has to name vanilla's — which
 * it could not: {@code MenuType.MenuSupplier} is private, and the widening that
 * lets this module reach it stays this module's business.
 *
 * @param <T> the menu class
 */
@FunctionalInterface
public interface MenuFactory<T extends AbstractContainerMenu> {

    /**
     * Builds one.
     *
     * @param windowId  the id the server gave this window
     * @param inventory the player's inventory
     * @return the menu
     */
    T create(int windowId, Inventory inventory);
}
