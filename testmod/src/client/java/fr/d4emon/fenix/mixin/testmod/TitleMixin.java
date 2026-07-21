package fr.d4emon.fenix.mixin.testmod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Appends {@code | Fenix Loader} to the game's window title.
 *
 * <p>The whole point of this mixin is to be <em>visible</em>: if the title bar
 * ends with "Fenix Loader", a mod reached into a real Minecraft method and
 * changed what it returned. It rewrites the return value of
 * {@code Minecraft.createTitle()}, so it needs neither the {@code Minecraft}
 * type (named as a string) nor anything but Mixin's own callback type — a mod
 * mixin that compiles without the game on its classpath.
 */
@Mixin(targets = "net.minecraft.client.Minecraft", remap = false)
public class TitleMixin {

    /** Matched by Mixin from the config; not called directly. */
    public TitleMixin() {
    }

    @Inject(method = "createTitle", at = @At("RETURN"), cancellable = true, require = 0)
    private void fenix$appendLoaderName(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(cir.getReturnValue() + " | Fenix Loader");
    }
}
