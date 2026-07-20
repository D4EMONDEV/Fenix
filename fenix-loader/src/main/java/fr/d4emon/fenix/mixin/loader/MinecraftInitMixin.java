package fr.d4emon.fenix.mixin.loader;

import fr.d4emon.fenix.loader.launch.FenixHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@code onInit} once the client is constructed.
 *
 * <p>The tail of {@code Minecraft.<init>} is the point where the client object
 * is fully built — the client-side counterpart of the register hook. Client
 * only, so it lives in the config's {@code client} section.
 */
@Mixin(targets = "net.minecraft.client.Minecraft", remap = false)
public class MinecraftInitMixin {

    /** Matched by Mixin from the config; not called directly. */
    public MinecraftInitMixin() {
    }

    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void fenix$onInit(CallbackInfo ci) {
        FenixHooks.onGameInit();
    }
}
