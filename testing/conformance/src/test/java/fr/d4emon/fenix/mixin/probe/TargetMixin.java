package fr.d4emon.fenix.mixin.probe;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixture for {@code MixinApplicationTest}: rewrites the return value of a
 * synthetic target's {@code greeting()} method.
 *
 * <p>Compiled by the module but not on any run's <em>child</em> scope — the test
 * copies its bytecode into a jar the {@code FenixClassLoader} loads, exactly as
 * a mod would ship it. {@code require = 1} makes the injection mandatory, so a
 * pipeline that silently stops applying mixins fails the test loudly.
 */
@Mixin(targets = "fr.d4emon.fenix.probe.GreetingTarget", remap = false)
public class TargetMixin {

    /** Matched by Mixin from the config; not called directly. */
    public TargetMixin() {
    }

    @Inject(method = "greeting", at = @At("RETURN"), cancellable = true, require = 1)
    private void probe$rewrite(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("mixed");
    }
}
