package fr.d4emon.fenix.registry.fluid;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The knobs a {@link FenixFlowingFluid} reads at run time, and the four
 * cross-references that make a fluid whole.
 *
 * <p>The four references are suppliers rather than the things themselves
 * because a fluid, its flowing form, its block and its bucket each need the
 * others, and something has to be built first. Every one of these is read only
 * once the game is running — never during registration — so by the time any
 * {@code get()} fires, all four are bound.
 *
 * @param source              the still fluid
 * @param flowing             the moving fluid
 * @param block               the block the fluid is, in the world
 * @param bucket              the bucket that carries it, or empty for a fluid
 *                            that can only be placed by code
 * @param slopeFindDistance   how far it looks for a downward slope to run to;
 *                            water is 4, lava 2
 * @param dropOff             how many levels it loses per block travelled; water
 *                            is 1, lava 2, so lava spreads less far
 * @param tickDelay           ticks between spread steps; water is 5, lava 30
 * @param explosionResistance blast resistance of the fluid itself
 * @param canConvertToSource  whether two flowing sources can make a new still
 *                            one, as water does when the game rule allows
 * @param dripParticle        what drips from it through a block above, if
 *                            anything
 * @param pickupSound         the sound a bucket makes filling from it
 */
public record FluidType(
        Supplier<? extends Fluid> source,
        Supplier<? extends Fluid> flowing,
        Supplier<Block> block,
        Optional<Supplier<Item>> bucket,
        int slopeFindDistance,
        int dropOff,
        int tickDelay,
        float explosionResistance,
        boolean canConvertToSource,
        Optional<ParticleOptions> dripParticle,
        Optional<SoundEvent> pickupSound) {
}
