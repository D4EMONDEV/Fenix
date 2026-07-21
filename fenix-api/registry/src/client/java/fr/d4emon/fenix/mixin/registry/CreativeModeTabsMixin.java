package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.CreativePages;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Shows one page of creative tabs at a time.
 *
 * <p>The creative screen asks {@code tabs()} five separate times — to draw
 * them, to hit-test clicks, to release them, for tooltips, and while building
 * its render state. Narrowing the answer here rather than at each of those
 * sites means the screen cannot end up drawing one set and clicking another.
 *
 * <p>Client-only, declared in the config's {@code client} block: a server also
 * calls {@code tabs()}, and there paging would mean quietly dropping mod tabs
 * from the contents it builds.
 */
@Mixin(CreativeModeTabs.class)
public class CreativeModeTabsMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    CreativeModeTabsMixin() {
    }

    @Inject(method = "tabs", at = @At("RETURN"), cancellable = true)
    private static void fenix$onlyCurrentPage(CallbackInfoReturnable<List<CreativeModeTab>> info) {
        info.setReturnValue(CreativePages.onCurrentPage(info.getReturnValue()));
    }
}
