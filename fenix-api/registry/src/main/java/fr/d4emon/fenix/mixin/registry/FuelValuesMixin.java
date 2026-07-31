package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.BlockInteractions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FuelValues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a mod's items burn in a furnace.
 *
 * <p>Burn times are built once per server from a fixed list of vanilla's items
 * and tags. An item outside it burns for zero ticks, which a furnace reads as
 * "not fuel" — so a modded coal is simply refused by the fuel slot, with no
 * message and nothing to search for.
 *
 * <p>Answered ahead of vanilla's table rather than added to it, because the
 * table is rebuilt whenever datapacks reload and a mod's entries would be lost
 * each time.
 */
@Mixin(FuelValues.class)
public class FuelValuesMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    FuelValuesMixin() {
    }

    @Inject(method = "burnDuration", at = @At("HEAD"), cancellable = true)
    private void fenix$modBurnDuration(ItemStack stack, CallbackInfoReturnable<Integer> info) {
        Integer ticks = declaredFor(stack);
        if (ticks != null) {
            info.setReturnValue(ticks);
        }
    }

    @Inject(method = "isFuel", at = @At("HEAD"), cancellable = true)
    private void fenix$modIsFuel(ItemStack stack, CallbackInfoReturnable<Boolean> info) {
        if (declaredFor(stack) != null) {
            info.setReturnValue(true);
        }
    }

    private static Integer declaredFor(ItemStack stack) {
        if (stack.isEmpty()) {
            // Vanilla answers zero for an empty stack; a mod's table is keyed by
            // item and an empty stack has none worth asking about.
            return null;
        }
        return BlockInteractions.fuelOf(stack.getItem());
    }
}
