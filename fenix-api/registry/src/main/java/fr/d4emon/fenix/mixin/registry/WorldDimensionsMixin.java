package fr.d4emon.fenix.mixin.registry;

import com.mojang.serialization.Lifecycle;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops a mod's dimension from marking every world "experimental".
 *
 * <p>Vanilla's rule is {@code isVanillaLike(key, stem) ? stable : experimental}
 * — the overworld, the nether and the end generated the way vanilla generates
 * them are stable, and everything else is not. World creation adds that up and
 * warns on anything that is not stable, so <em>one</em> dimension from
 * <em>one</em> installed mod is enough to put "Experimental Features" and "Here
 * be dragons!" in front of a player who only wanted to make a world.
 *
 * <p>The rule is right for a hand-written datapack, which is what it was
 * written for: Mojang cannot promise that a datapack's worldgen will keep
 * loading. It is wrong for a mod. A mod's dimension is code and data shipped
 * together, versioned together, and the mod is what promises to keep them
 * working — the same promise Fabric and NeoForge make by suppressing this
 * warning too.
 *
 * <p>So: dimensions in the {@code minecraft} namespace keep vanilla's answer
 * exactly, and only those a mod brought are called stable. A datapack that
 * defines {@code minecraft:overworld} oddly still warns, which is the case the
 * rule exists for.
 */
@Mixin(WorldDimensions.class)
public class WorldDimensionsMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    WorldDimensionsMixin() {
    }

    @Inject(method = "checkStability", at = @At("HEAD"), cancellable = true)
    private static void fenix$modDimensionsAreStable(
            ResourceKey<LevelStem> key, LevelStem stem,
            CallbackInfoReturnable<Lifecycle> info) {
        if (!key.identifier().getNamespace().equals("minecraft")) {
            info.setReturnValue(Lifecycle.stable());
        }
    }
}
