package fr.d4emon.fenix.registry.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * A fluid that spreads like water, configured rather than subclassed.
 *
 * <p>Vanilla writes one of these per fluid — {@code WaterFluid},
 * {@code LavaFluid} — as an abstract class with {@code Source} and
 * {@code Flowing} inner classes, because the two share almost everything and
 * differ only in whether they are a full block and how they define their state.
 * This is the same shape, with the handful of numbers that actually differ
 * between fluids lifted out into a {@link FluidType} so a mod supplies values
 * instead of a subclass.
 *
 * <p>Every method here mirrors {@code WaterFluid}. The parts left out —
 * {@code getFlow}, {@code getShape}, {@code getHeight} — are the parts
 * {@link FlowingFluid} already implements the same way for every fluid, so
 * there is nothing to configure.
 */
public abstract class FenixFlowingFluid extends FlowingFluid {

    private final FluidType type;

    FenixFlowingFluid(FluidType type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    /** {@return the configuration this fluid was built from} */
    public FluidType fenixType() {
        return type;
    }

    @Override
    public Fluid getFlowing() {
        return type.flowing().get();
    }

    @Override
    public Fluid getSource() {
        return type.source().get();
    }

    @Override
    public Item getBucket() {
        // Air, not empty-optional: vanilla reads this bare and a fluid with no
        // bucket simply has nothing to hand back when one is asked for.
        return type.bucket().map(bucket -> bucket.get()).orElse(Items.AIR);
    }

    @Override
    protected boolean canConvertToSource(final ServerLevel level) {
        return type.canConvertToSource();
    }

    @Override
    protected void beforeDestroyingBlock(final LevelAccessor level, final BlockPos pos, final BlockState state) {
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        Block.dropResources(state, level, pos, blockEntity);
    }

    @Override
    public int getSlopeFindDistance(final LevelReader level) {
        return type.slopeFindDistance();
    }

    @Override
    public int getDropOff(final LevelReader level) {
        return type.dropOff();
    }

    @Override
    public int getTickDelay(final LevelReader level) {
        return type.tickDelay();
    }

    @Override
    protected float getExplosionResistance() {
        return type.explosionResistance();
    }

    @Override
    public BlockState createLegacyBlock(final FluidState fluidState) {
        // The block form of a flowing fluid carries its level in a block state,
        // which is how the world stores fluids without a fluid layer of its own.
        return type.block().get().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(fluidState));
    }

    @Override
    public boolean isSame(final Fluid other) {
        return other == getSource() || other == getFlowing();
    }

    @Override
    public boolean canBeReplacedWith(final FluidState state, final BlockGetter level, final BlockPos pos,
                                     final Fluid other, final Direction direction) {
        // A different fluid falling from directly above washes this one out, the
        // way water is displaced from below. Same-fluid meetings are handled by
        // the spread rules, not here.
        return direction == Direction.DOWN && !isSame(other);
    }

    @Nullable
    @Override
    public ParticleOptions getDripParticle() {
        return type.dripParticle().orElse(null);
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return type.pickupSound();
    }

    /** The still form: a full block that never runs anywhere. */
    public static final class Source extends FenixFlowingFluid {

        public Source(FluidType type) {
            super(type);
        }

        @Override
        public int getAmount(final FluidState fluidState) {
            return 8;
        }

        @Override
        public boolean isSource(final FluidState fluidState) {
            return true;
        }
    }

    /** The moving form: carries a level, and is never a source. */
    public static final class Flowing extends FenixFlowingFluid {

        public Flowing(FluidType type) {
            super(type);
        }

        @Override
        protected void createFluidStateDefinition(final StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(final FluidState fluidState) {
            return fluidState.getValue(LEVEL);
        }

        @Override
        public boolean isSource(final FluidState fluidState) {
            return false;
        }
    }
}
