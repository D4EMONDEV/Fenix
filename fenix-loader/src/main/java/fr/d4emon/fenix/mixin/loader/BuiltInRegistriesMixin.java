package fr.d4emon.fenix.mixin.loader;

import fr.d4emon.fenix.loader.launch.FenixHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@code onRegister} while the game's registries are still open.
 *
 * <p>{@code BuiltInRegistries.bootStrap()} populates every built-in registry and
 * then calls the private static {@code freeze()} to seal them. Injecting at the
 * head of {@code freeze} is the last moment a mod can add content, which is the
 * whole point of the register phase.
 *
 * <p>The target is named as a string so this class carries no compile-time
 * dependency on Minecraft, and the handler touches only Mixin types — the whole
 * reason the loader can build without the game on its classpath.
 */
@Mixin(targets = "net.minecraft.core.registries.BuiltInRegistries", remap = false)
public class BuiltInRegistriesMixin {

    /** Matched by Mixin from the config; not called directly. */
    public BuiltInRegistriesMixin() {
    }

    @Inject(method = "freeze", at = @At("HEAD"), require = 0)
    private static void fenix$onRegister(CallbackInfo ci) {
        FenixHooks.onGameRegister();
    }
}
