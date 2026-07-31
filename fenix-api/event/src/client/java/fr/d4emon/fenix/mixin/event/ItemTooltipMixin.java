package fr.d4emon.fenix.mixin.event;

import fr.d4emon.fenix.event.client.ItemTooltipEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Fires {@link ItemTooltipEvents#BUILD} as vanilla finishes a tooltip.
 *
 * <p>At the return rather than anywhere earlier, so a mod sees the whole
 * tooltip — name, components, durability, the lot — and can put a line in
 * relation to what is already there.
 *
 * <p>The list is copied before being handed out. Vanilla's may be immutable
 * depending on the path taken, and a listener that tried to add to one would
 * throw from inside a render, which is a miserable place to debug.
 */
@Mixin(ItemStack.class)
public class ItemTooltipMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    ItemTooltipMixin() {
    }

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void fenix$tooltip(Item.TooltipContext context, Player player, TooltipFlag flag,
                               CallbackInfoReturnable<List<Component>> info) {
        if (!ItemTooltipEvents.BUILD.hasListeners()) {
            // The common case, and this runs for every stack under the pointer
            // every frame — so it costs a field read and nothing else.
            return;
        }
        List<Component> lines = new ArrayList<>(info.getReturnValue());
        ItemTooltipEvents.BUILD.fire(new ItemTooltipEvents.Build(
                (ItemStack) (Object) this, player, flag, lines));
        info.setReturnValue(lines);
    }
}
