package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.CreativePages;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Lets two tabs share a square, as long as they are on different pages.
 *
 * <p>Vanilla refuses outright: it walks every registered tab during bootstrap
 * and throws on the first repeated row and column. That is the right rule when
 * there is one screenful of tabs, and it is exactly the rule paging breaks —
 * a mod's tab sits on the same square as Building Blocks and the two are never
 * visible at once.
 *
 * <p>So the check is not dropped, only widened: the page becomes part of the
 * position. Two tabs genuinely drawn on top of each other still refuse to load,
 * which is the failure worth keeping — it is silent otherwise.
 *
 * <p>Common, not client-only: bootstrap validates on a dedicated server too,
 * and a server that crashes on a tab nobody there will ever look at is worse
 * than useless.
 */
@Mixin(CreativeModeTabs.class)
public class CreativeTabValidationMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    CreativeTabValidationMixin() {
    }

    @Inject(method = "validate", at = @At("HEAD"), cancellable = true)
    private static void fenix$validatePerPage(CallbackInfo info) {
        Map<String, String> positions = new HashMap<>();

        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            String position = CreativePages.pageOf(tab) + "/" + tab.row() + "/" + tab.column();
            String name = tab.getDisplayName().getString();
            String previous = positions.put(position, name);
            if (previous != null) {
                throw new IllegalArgumentException(
                        "Duplicate position: " + name + " vs. " + previous);
            }
        }
        info.cancel();
    }
}
