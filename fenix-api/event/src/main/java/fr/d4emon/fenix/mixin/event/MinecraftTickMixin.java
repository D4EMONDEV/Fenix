package fr.d4emon.fenix.mixin.event;

import fr.d4emon.fenix.event.client.ClientEvents;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires the client tick events. Client only.
 */
@Mixin(Minecraft.class)
public class MinecraftTickMixin {

    /** Matched by Mixin from the config; not called directly. */
    public MinecraftTickMixin() {
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void fenix$onTickStart(CallbackInfo ci) {
        ClientEvents.TICK_START.fire(new ClientEvents.Tick((Minecraft) (Object) this));
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void fenix$onTickEnd(CallbackInfo ci) {
        ClientEvents.TICK_END.fire(new ClientEvents.Tick((Minecraft) (Object) this));
    }
}
