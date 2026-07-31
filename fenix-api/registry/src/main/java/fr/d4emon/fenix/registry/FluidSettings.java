package fr.d4emon.fenix.registry;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Everything {@link FluidBuilder} collected, on its way to {@link Registrar}.
 *
 * <p>Package-private and plain: the builder is the API a mod sees, and this is
 * just the shape the registrar reads it back out of. The suppliers that tie the
 * fluid to its own block and bucket are built inside the registrar, because that
 * is where the handles they close over are created.
 */
record FluidSettings(
        UnaryOperator<BlockBehaviour.Properties> blockProperties,
        boolean withBucket,
        UnaryOperator<Item.Properties> bucketProperties,
        int slopeFindDistance,
        int dropOff,
        int tickDelay,
        float explosionResistance,
        boolean canConvertToSource,
        Optional<ParticleOptions> dripParticle,
        Optional<SoundEvent> pickupSound) {
}
