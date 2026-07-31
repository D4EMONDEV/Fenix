package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.BlockInteractions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;

/**
 * Lets a mod's blocks weather, and an axe scrape them back.
 *
 * <p>Vanilla keeps the chain in a memoised {@code BiMap} on an interface, which
 * is harder to replace wholesale than the waxing table on a class — an
 * interface's fields are implicitly final and reassigning one is not something
 * to rely on. Both directions are read through two static methods here, though,
 * so answering ahead of those covers every caller: weathering over time, an axe
 * scraping a stage back, and lightning stripping a block to bare copper.
 */
@Mixin(WeatheringCopper.class)
public interface WeatheringCopperMixin {

    @Inject(method = "getNext(Lnet/minecraft/world/level/block/Block;)Ljava/util/Optional;",
            at = @At("HEAD"), cancellable = true)
    private static void fenix$modNext(Block block, CallbackInfoReturnable<Optional<Block>> info) {
        Block next = BlockInteractions.oxidations().get(block);
        if (next != null) {
            info.setReturnValue(Optional.of(next));
        }
    }

    @Inject(method = "getPrevious(Lnet/minecraft/world/level/block/Block;)Ljava/util/Optional;",
            at = @At("HEAD"), cancellable = true)
    private static void fenix$modPrevious(Block block, CallbackInfoReturnable<Optional<Block>> info) {
        for (Map.Entry<Block, Block> step : BlockInteractions.oxidations().entrySet()) {
            if (step.getValue() == block) {
                info.setReturnValue(Optional.of(step.getKey()));
                return;
            }
        }
    }
}
