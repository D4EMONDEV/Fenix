package fr.d4emon.fenix.registry.client;

import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Builds the provider that draws a particle, given its textures.
 *
 * <p>Fenix's own, and public, for the same reason {@code ScreenFactory} is:
 * vanilla's equivalent is a private nested interface, and a mod passing
 * {@code MyProvider::new} would otherwise have to name a type it cannot see.
 *
 * <p>The sprite set arrives already loaded from
 * {@code assets/<namespace>/particles/<name>.json}, which is what lists the
 * textures.
 */
@FunctionalInterface
public interface SpriteParticleFactory {

    /**
     * Builds one.
     *
     * @param sprites the textures named in the particle's definition file
     * @return the provider
     */
    ParticleProvider<SimpleParticleType> create(SpriteSet sprites);
}
