package fr.d4emon.fenix.registry.client;

import fr.d4emon.fenix.registry.Holder;
import net.minecraft.core.particles.SimpleParticleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Says what a particle looks like.
 *
 * <pre>{@code
 * ParticleRendering.register(ModContent.SPARK, GlowParticle.ElectricSparkProvider::new);
 * }</pre>
 *
 * <p>A particle type with no provider is spawned and never drawn: the server
 * sends it, the client looks up a provider, finds none, and returns. Nothing
 * crashes and nothing is logged — the effect simply does not happen, on some
 * machines and not others if a mod is only installed on one side.
 *
 * <p>The textures come from
 * {@code assets/<namespace>/particles/<name>.json}, a file listing sprite names;
 * without it the particle is drawn as the missing texture.
 *
 * <p>Client-only, and registered from {@code onRegister} — the game builds its
 * provider table once, while loading resources, and that is where these are
 * added.
 */
public final class ParticleRendering {

    /** What has been registered, in registration order. */
    private static final List<Registration> REGISTERED = new ArrayList<>();

    /**
     * One pending registration.
     *
     * @param type    the particle type
     * @param factory builds its provider
     */
    public record Registration(Holder<SimpleParticleType> type, SpriteParticleFactory factory) {
    }

    private ParticleRendering() {
    }

    /**
     * Registers the provider for a particle type.
     *
     * @param type    the type, already registered
     * @param factory builds the provider from the particle's textures
     * @throws NullPointerException if either argument is {@code null}
     */
    public static void register(Holder<SimpleParticleType> type, SpriteParticleFactory factory) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(factory, "factory");
        REGISTERED.add(new Registration(type, factory));
    }

    /**
     * {@return every registration made, in order}
     *
     * <p>For the mixin that adds them to the game's table. Not part of what a
     * mod is meant to call.
     */
    public static List<Registration> fenix$registered() {
        return List.copyOf(REGISTERED);
    }
}
