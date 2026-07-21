package fr.d4emon.fenix.mixin.network;

import fr.d4emon.fenix.network.client.ClientChannels;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Installs the client's way of sending, once the client exists.
 *
 * <p>An API module has no lifecycle of its own — it is not a mod with an entry
 * class — so this is where the client half gets a chance to run. Hanging it on
 * the client's own construction means it is done before anything could send.
 */
@Mixin(Minecraft.class)
public class ClientInstallMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    ClientInstallMixin() {
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void fenix$installSender(CallbackInfo info) {
        ClientChannels.install();
        // Loads the check so its handler is registered before any join.
        fr.d4emon.fenix.network.RegistryCheck.listen();
    }
}
