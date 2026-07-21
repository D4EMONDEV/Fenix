package fr.d4emon.fenix.mixin.registry;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Reaches vanilla's table of screens, whose own {@code register} is private.
 *
 * <p>Mutable, so it can simply be added to. A menu type with no screen is not
 * an error a mod hears about: the server opens the window, the client has
 * nothing to draw, and the player sees their inventory close again.
 */
@Mixin(MenuScreens.class)
public interface MenuScreensAccessor {

    /** {@return vanilla's table, which can be added to} */
    @Accessor("SCREENS")
    static Map<MenuType<?>, MenuScreens.ScreenConstructor<?, ?>> fenix$screens() {
        throw new AssertionError("replaced by Mixin");
    }
}
