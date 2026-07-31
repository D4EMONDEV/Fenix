package fr.d4emon.fenix.mixin.registry;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

/**
 * Reaches the private pass that maps a block state to a point of interest.
 *
 * <p>{@code PoiTypes} keeps a {@code Map<BlockState, Holder<PoiType>>} and fills
 * it, once, at bootstrap — every registration goes through the private
 * {@code registerBlockStates} to add its states to that map, and {@code forState}
 * reads only from it. A mod that registers a job-site type after bootstrap adds
 * the type to the registry but not to the map, and the villager AI, which asks
 * {@code forState}, never sees the job site. This lets the registrar redo that
 * one pass for a mod's own type.
 */
@Mixin(PoiTypes.class)
public interface PoiTypesInvoker {

    /**
     * Adds each block state to the point-of-interest map, pointed at the type.
     *
     * @param type          the registered point-of-interest type, as a holder
     * @param matchingStates the block states that are this job site
     */
    @Invoker("registerBlockStates")
    static void fenix$registerBlockStates(Holder<PoiType> type, Set<BlockState> matchingStates) {
        throw new AssertionError("mixin did not apply");
    }
}
