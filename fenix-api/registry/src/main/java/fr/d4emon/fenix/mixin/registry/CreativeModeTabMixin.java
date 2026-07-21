package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.CreativeTabs;
import fr.d4emon.fenix.registry.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Adds a mod's content to the creative menu, once the tab has built its own.
 *
 * <p>Contents are rebuilt on every resource reload rather than once, so this
 * runs each time and the additions come back with them.
 */
@Mixin(CreativeModeTab.class)
public class CreativeModeTabMixin {

    /** What the tab shows. Vanilla fills it just before this runs. */
    @Shadow
    private Collection<ItemStack> displayItems;

    /** What the search tab shows, which aggregates every tab. */
    @Shadow
    private Set<ItemStack> displayItemsSearchTab;

    /** Matched by Mixin from the config; not called directly. */
    public CreativeModeTabMixin() {
    }

    @Inject(method = "buildContents", at = @At("TAIL"), remap = false)
    private void fenix$addModContent(CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo ci) {
        CreativeModeTab self = (CreativeModeTab) (Object) this;

        Optional<ResourceKey<CreativeModeTab>> key =
                BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(self);
        if (key.isEmpty()) {
            return;
        }
        List<Holder<?>> additions = CreativeTabs.additionsFor(key.get());
        for (Holder<?> holder : additions) {
            ItemStack stack = stackOf(holder);
            // An empty stack means the content never got its item mapping, and
            // vanilla would later die building the search tab with "Stack size
            // must be exactly 1" — far from the cause. Skip it instead.
            if (stack.isEmpty()) {
                continue;
            }
            displayItems.add(stack);
            displayItemsSearchTab.add(stack);
        }
    }

    private static ItemStack stackOf(Holder<?> holder) {
        Object value = holder.get();
        if (value instanceof Block block) {
            return new ItemStack(block);
        }
        if (value instanceof Item item) {
            return new ItemStack(item);
        }
        return ItemStack.EMPTY;
    }
}
