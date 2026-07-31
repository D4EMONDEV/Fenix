package fr.d4emon.fenix.mixin.registry;

import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import fr.d4emon.fenix.registry.BlockInteractions;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Lets honeycomb wax a mod's blocks, and an axe scrape them again.
 *
 * <p>Vanilla's table has no single reader to answer ahead of: four different
 * places read {@code WAX_OFF_BY_BLOCK} inline — the axe, a carved pumpkin, a
 * copper chest, a lightning bolt — and each would need its own injection.
 * Instead the table itself is replaced, once, with one that carries a mod's
 * entries as well. Every reader then finds them, in both directions, with no
 * further help.
 *
 * <p>Safe because the supplier is memoised and nothing resolves it during class
 * initialisation: the first real {@code get()} happens in game, long after a mod
 * has declared what it wants. The inverse table is derived from this one by a
 * lambda that reads the field when it runs, so it picks up the replacement too.
 */
@Mixin(HoneycombItem.class)
public class HoneycombItemMixin {

    @Shadow
    @Final
    @Mutable
    private static Supplier<BiMap<Block, Block>> WAXABLES;

    /** Never called — a mixin's constructors are discarded when it is merged. */
    HoneycombItemMixin() {
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void fenix$addModWaxables(CallbackInfo info) {
        Supplier<BiMap<Block, Block>> vanilla = WAXABLES;
        WAXABLES = Suppliers.memoize(() -> {
            ImmutableBiMap.Builder<Block, Block> merged = ImmutableBiMap.builder();
            merged.putAll(vanilla.get());
            for (Map.Entry<Block, Block> entry : BlockInteractions.waxables().entrySet()) {
                merged.put(entry.getKey(), entry.getValue());
            }
            return merged.build();
        });
    }
}
