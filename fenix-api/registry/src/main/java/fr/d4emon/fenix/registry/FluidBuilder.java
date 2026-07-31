package fr.d4emon.fenix.registry;

import fr.d4emon.fenix.registry.fluid.FluidResult;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Describes a fluid, then registers the four things a fluid is.
 *
 * <pre>{@code
 * public static final FluidResult ACID = REGISTRAR.newFluid("acid")
 *         .tickDelay(10)
 *         .bucket()
 *         .register();
 * }</pre>
 *
 * <p>That one call registers a still fluid, a flowing fluid, the block the fluid
 * becomes in the world, and — because {@link #bucket()} was asked for — the
 * bucket that carries it. What it looks like is the client's business; see
 * {@code FluidRendering}.
 *
 * <p>The defaults describe a water-like fluid. The numbers that make a fluid
 * feel like lava instead — slower ticks, a shorter reach — are the ones with
 * setters here; the rest of vanilla's fluid behaviour is shared by every
 * flowing fluid and is not configurable because it is not per-fluid.
 */
public final class FluidBuilder {

    private final Registrar registrar;
    private final String name;

    private UnaryOperator<BlockBehaviour.Properties> blockProperties = UnaryOperator.identity();
    private boolean withBucket;
    private UnaryOperator<Item.Properties> bucketProperties = UnaryOperator.identity();
    private int slopeFindDistance = 4;
    private int dropOff = 1;
    private int tickDelay = 5;
    private float explosionResistance = 100f;
    private boolean canConvertToSource;
    private Optional<ParticleOptions> dripParticle = Optional.empty();
    private Optional<SoundEvent> pickupSound = Optional.of(SoundEvents.BUCKET_FILL);

    FluidBuilder(Registrar registrar, String name) {
        this.registrar = registrar;
        this.name = name;
    }

    /**
     * Shapes the block the fluid becomes in the world.
     *
     * <p>The defaults already make it a proper liquid — no collision, no drops,
     * destroyed by pistons. Reach for this to change its explosion resistance in
     * block form, its map colour, or anything else on vanilla's block builder.
     *
     * @param step what to do to the block properties
     * @return this builder
     */
    public FluidBuilder blockProperties(UnaryOperator<BlockBehaviour.Properties> step) {
        Objects.requireNonNull(step, "step");
        UnaryOperator<BlockBehaviour.Properties> previous = blockProperties;
        blockProperties = props -> step.apply(previous.apply(props));
        return this;
    }

    /**
     * Also registers a bucket for the fluid — what you want unless it is only
     * ever placed by code.
     *
     * @return this builder
     */
    public FluidBuilder bucket() {
        this.withBucket = true;
        return this;
    }

    /**
     * Also registers a bucket, and shapes its item properties.
     *
     * @param step what to do to the bucket's item properties
     * @return this builder
     */
    public FluidBuilder bucket(UnaryOperator<Item.Properties> step) {
        this.withBucket = true;
        this.bucketProperties = Objects.requireNonNull(step, "step");
        return this;
    }

    /**
     * Sets how far the fluid looks for a downward slope to run to. Water is 4,
     * lava is 2.
     *
     * @param blocks the distance
     * @return this builder
     */
    public FluidBuilder slopeFindDistance(int blocks) {
        this.slopeFindDistance = blocks;
        return this;
    }

    /**
     * Sets how many levels the fluid loses per block it travels. Water is 1,
     * lava 2 — which is why lava spreads less far.
     *
     * @param levels the drop-off
     * @return this builder
     */
    public FluidBuilder dropOff(int levels) {
        this.dropOff = levels;
        return this;
    }

    /**
     * Sets how many ticks pass between spread steps. Water is 5, lava 30.
     *
     * @param ticks the delay
     * @return this builder
     */
    public FluidBuilder tickDelay(int ticks) {
        this.tickDelay = ticks;
        return this;
    }

    /**
     * Sets the blast resistance of the fluid itself.
     *
     * @param resistance the resistance
     * @return this builder
     */
    public FluidBuilder explosionResistance(float resistance) {
        this.explosionResistance = resistance;
        return this;
    }

    /**
     * Lets two flowing sources form a new still source between them, the way
     * water does when the game rule allows. Off by default, as it is for lava.
     *
     * @return this builder
     */
    public FluidBuilder convertsToSource() {
        this.canConvertToSource = true;
        return this;
    }

    /**
     * Sets what drips from the fluid through a block below it.
     *
     * @param particle the drip particle
     * @return this builder
     */
    public FluidBuilder dripParticle(ParticleOptions particle) {
        this.dripParticle = Optional.of(Objects.requireNonNull(particle, "particle"));
        return this;
    }

    /**
     * Sets the sound a bucket makes filling from the fluid. Defaults to the
     * ordinary bucket-fill sound.
     *
     * @param sound the pickup sound
     * @return this builder
     */
    public FluidBuilder pickupSound(SoundEvent sound) {
        this.pickupSound = Optional.of(Objects.requireNonNull(sound, "sound"));
        return this;
    }

    /**
     * Declares the fluid. Registration itself happens at {@code apply()}.
     *
     * @return handles on the still and flowing fluids, the block and the bucket
     */
    public FluidResult register() {
        return registrar.fluid(name, new FluidSettings(
                blockProperties, withBucket, bucketProperties,
                slopeFindDistance, dropOff, tickDelay, explosionResistance,
                canConvertToSource, dripParticle, pickupSound));
    }
}
