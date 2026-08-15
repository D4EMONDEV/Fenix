package fr.d4emon.fenix.mixin.event;

import fr.d4emon.fenix.event.client.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link ClientEvents#SCREEN} whenever the client changes screen.
 *
 * <p>At HEAD, so a listener sees it before the screen is initialised — which is
 * when a mod adding a widget has to act, because initialisation is what builds
 * the list it wants to add to.
 */
@Mixin(Minecraft.class)
public class ScreenChangeMixin {

    /** Merged into Minecraft; never constructed directly. */
    public ScreenChangeMixin() {
    }

    @Inject(method = "setScreenAndShow", at = @At("HEAD"), remap = false)
    private void fenix$onScreen(Screen screen, CallbackInfo ci) {
        ClientEvents.SCREEN.fire(new ClientEvents.ScreenChange(screen));
    }
}
