package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.client.ParticleRendering;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.core.particles.SimpleParticleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the mod's particle providers to the game's table.
 *
 * <p>Vanilla fills that table once, in a method naming its own particles one by
 * one, and nothing adds to it afterwards. A type missing from it is spawned and
 * never drawn: the lookup returns nothing and the effect simply does not
 * happen, with nothing logged.
 *
 * <p>Appended at the end of that method, which is also where the sprite sets it
 * creates are still being collected — registering later would leave the
 * particle with no textures.
 */
@Mixin(ParticleResources.class)
public abstract class ParticleResourcesMixin {

    /**
     * Vanilla's own registration, which both records the provider and creates
     * the sprite set its textures will be loaded into.
     */
    @Shadow
    protected abstract <T extends net.minecraft.core.particles.ParticleOptions> void register(
            net.minecraft.core.particles.ParticleType<T> type,
            ParticleResources.SpriteParticleRegistration<T> provider);

    @Inject(method = "registerProviders", at = @At("RETURN"))
    private void fenix$addModParticles(CallbackInfo info) {
        for (ParticleRendering.Registration registration : ParticleRendering.fenix$registered()) {
            SimpleParticleType type = registration.type().get();
            // Adapted here rather than taken directly: vanilla's registration
            // interface is private, and a mod passing a method reference for it
            // would not compile.
            ParticleResources.SpriteParticleRegistration<SimpleParticleType> vanilla =
                    sprites -> registration.factory().create(sprites);
            register(type, vanilla);
        }
    }
}
