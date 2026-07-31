package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.Brewing;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a mod's potions be brewed.
 *
 * <p>Vanilla's brewing table is built once per server from a fixed list and the
 * builder is thrown away, so there is nothing to add to afterwards — a potion a
 * mod registered can be given by command and made by nothing.
 *
 * <p>Caught here, at the last moment the builder is still open, and filled
 * through the very methods vanilla just used on it: {@code addMix} and its
 * neighbours are public. So this adds recipes exactly as vanilla adds its own,
 * rather than reaching into the finished table.
 *
 * <p>Runs on every rebuild, which is what makes a mod's mixes survive a datapack
 * reload as vanilla's do.
 */
@Mixin(PotionBrewing.Builder.class)
public class PotionBrewingBuilderMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    PotionBrewingBuilderMixin() {
    }

    @Inject(method = "build", at = @At("HEAD"))
    private void fenix$addModMixes(CallbackInfoReturnable<PotionBrewing> info) {
        Brewing.applyTo((PotionBrewing.Builder) (Object) this);
    }
}
