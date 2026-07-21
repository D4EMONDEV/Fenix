package fr.d4emon.fenix.mixin.event;

import fr.d4emon.fenix.event.ServerEvents;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/**
 * Fires the server lifecycle and tick events.
 *
 * <p>{@code STARTED} rides on the first tick rather than the constructor: by
 * then the world is loaded and the server is really running, which is what a
 * listener means by "started". The flag is per instance, so loading a second
 * world in the same session fires it again for that server.
 */
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Unique
    private boolean fenix$started;

    /** Matched by Mixin from the config; not called directly. */
    public MinecraftServerMixin() {
    }

    @Inject(method = "tickServer", at = @At("HEAD"), remap = false)
    private void fenix$onTickStart(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        if (!fenix$started) {
            fenix$started = true;
            ServerEvents.STARTED.fire(new ServerEvents.Started(server));
        }
        ServerEvents.TICK_START.fire(new ServerEvents.Tick(server));
    }

    @Inject(method = "tickServer", at = @At("TAIL"), remap = false)
    private void fenix$onTickEnd(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ServerEvents.TICK_END.fire(new ServerEvents.Tick((MinecraftServer) (Object) this));
    }
}
