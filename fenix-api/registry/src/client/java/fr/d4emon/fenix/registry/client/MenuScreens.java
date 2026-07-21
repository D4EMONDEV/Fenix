package fr.d4emon.fenix.registry.client;

import fr.d4emon.fenix.mixin.registry.MenuScreensAccessor;
import fr.d4emon.fenix.registry.Holder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.Objects;

/**
 * Says what a menu looks like.
 *
 * <pre>{@code
 * MenuScreens.register(ModContent.SAFE_MENU, SafeScreen::new);
 * }</pre>
 *
 * <p>A menu type with no screen fails quietly and confusingly: the server opens
 * the window, the client finds nothing to draw, and the player watches their
 * inventory close again with no error anywhere.
 *
 * <p>Client-only, so call it from a {@code @Mod} class in {@code src/client}.
 */
public final class MenuScreens {

    private MenuScreens() {
    }

    /**
     * Registers the screen for a menu type.
     *
     * @param <M>    the menu class
     * @param <S>    the screen class
     * @param type   the type, already registered
     * @param screen builds the screen from the menu, the inventory and a title
     */
    public static <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void register(
            Holder<MenuType<M>> type, ScreenFactory<M, S> screen) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(screen, "screen");

        // Adapted here rather than taken directly: vanilla's ScreenConstructor
        // is private, and a mod passing a method reference for it would not
        // compile. Naming it is this module's privilege, not a mod's problem.
        net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor<M, S> vanilla = screen::create;
        MenuScreensAccessor.fenix$screens().put(type.get(), vanilla);
    }
}
