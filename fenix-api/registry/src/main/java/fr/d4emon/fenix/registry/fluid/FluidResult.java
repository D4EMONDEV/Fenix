package fr.d4emon.fenix.registry.fluid;

import fr.d4emon.fenix.registry.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.Optional;

/**
 * The handles a registered fluid leaves behind — the four things one fluid
 * actually is.
 *
 * <p>A fluid is never a single registration: it is a still form and a moving
 * form in the fluid registry, a block in the block registry, and usually a
 * bucket in the item registry. Handing back all four is what lets the client
 * name the two fluids for rendering, and a mod reach for the bucket to put it in
 * a creative tab.
 *
 * @param source  the still fluid
 * @param flowing the moving fluid
 * @param block   the block the fluid is, in the world
 * @param bucket  the bucket item, or empty if none was asked for
 */
public record FluidResult(
        Holder<FlowingFluid> source,
        Holder<FlowingFluid> flowing,
        Holder<Block> block,
        Optional<Holder<Item>> bucket) {
}
