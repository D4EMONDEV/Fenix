package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.BlockInteractions;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Lets an axe strip a mod's logs.
 *
 * <p>Vanilla's table of what strips into what is an immutable map built in a
 * static initialiser, so nothing can be added to it. A log outside it is one an
 * axe does nothing to — no sound, no particle, no change — which reads as a
 * broken axe rather than as a missing table entry.
 */
@Mixin(AxeItem.class)
public class AxeItemMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    AxeItemMixin() {
    }

    @Inject(method = "getStripped", at = @At("HEAD"), cancellable = true)
    private void fenix$modStripped(BlockState state, CallbackInfoReturnable<Optional<BlockState>> info) {
        Block stripped = BlockInteractions.strippedOf(state.getBlock());
        if (stripped == null) {
            return;
        }
        // The axis carries across, as it does for vanilla's logs, so a sideways
        // log stays sideways. withPropertiesOf would copy every shared property;
        // vanilla names this one, and matching it keeps a mod's stripped block
        // free to differ in anything else.
        BlockState result = stripped.defaultBlockState();
        if (state.hasProperty(RotatedPillarBlock.AXIS)
                && result.hasProperty(RotatedPillarBlock.AXIS)) {
            result = result.setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
        }
        info.setReturnValue(Optional.of(result));
    }
}
