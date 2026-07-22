package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.worldgen.BiomeModifications;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/**
 * Applies biome modifications once datapacks have loaded.
 *
 * <p>This moment and no other. Earlier, biome tags are not bound, so a selector
 * asking whether a biome is in the overworld gets the wrong answer. Later, a
 * chunk may already have generated from the unmodified biome, and a world with
 * an ore in some chunks and not others is worse than one without it.
 *
 * <p>Vanilla binds tags inside each registry's load task and freezes
 * afterwards, so by the time this future completes both are true.
 */
@Mixin(RegistryDataLoader.class)
public abstract class RegistryDataLoaderMixin {

    @Inject(method = "load(Lnet/minecraft/resources/RegistryDataLoader$LoaderFactory;"
            + "Ljava/util/List;Ljava/util/List;Ljava/util/concurrent/Executor;)"
            + "Ljava/util/concurrent/CompletableFuture;",
            at = @At("RETURN"), cancellable = true)
    private static void fenix$applyBiomeModifications(
            CallbackInfoReturnable<CompletableFuture<RegistryAccess.Frozen>> info) {
        // Chained rather than injected at the end of the lambda: the load is
        // asynchronous, and a return here means "the work is scheduled", not
        // "the registries exist".
        info.setReturnValue(info.getReturnValue().thenApply(registries -> {
            BiomeModifications.fenix$apply(registries);
            return registries;
        }));
    }
}
