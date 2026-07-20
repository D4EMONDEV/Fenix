package fr.d4emon.fenix.mixin.loader;

import fr.d4emon.fenix.loader.launch.FenixHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@code onInit} once a server is constructed.
 *
 * <p>On a dedicated server this is the only init hook; on a client with an
 * integrated server it also runs, but {@code onInit} is idempotent so the
 * earlier client hook wins and this is ignored. Common to both sides — the
 * server class is present in the client jar too.
 */
@Mixin(targets = "net.minecraft.server.MinecraftServer", remap = false)
public class MinecraftServerInitMixin {

    /** Matched by Mixin from the config; not called directly. */
    public MinecraftServerInitMixin() {
    }

    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void fenix$onInit(CallbackInfo ci) {
        FenixHooks.onGameInit();
    }
}
