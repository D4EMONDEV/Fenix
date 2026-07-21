package fr.d4emon.fenix.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The default attributes mods have declared.
 *
 * <p>Kept beside vanilla's table rather than merged into it, for two reasons
 * that both come down to timing. Vanilla's is an {@code ImmutableMap} built in
 * a static initialiser that resolves every vanilla attribute — so merely
 * reading it, during {@code onRegister}, would build it before the attribute
 * registry is bound and fail there. And a mod's own attribute values are
 * written against those same unbound holders, so they cannot be computed then
 * either.
 *
 * <p>So nothing is resolved until something asks. The mixin on
 * {@code DefaultAttributes} looks here first, by which point the game is past
 * bootstrap and everything binds cleanly.
 */
public final class EntityAttributes {

    private static final Map<EntityType<?>, Supplier<AttributeSupplier.Builder>> DECLARED =
            new HashMap<>();
    private static final Map<EntityType<?>, AttributeSupplier> BUILT = new HashMap<>();

    private EntityAttributes() {
    }

    /**
     * Records what a mod declared. Called by {@link Registrar}.
     *
     * @param type       the entity type
     * @param attributes builds its attributes, later
     */
    static void declare(EntityType<?> type, Supplier<AttributeSupplier.Builder> attributes) {
        DECLARED.put(Objects.requireNonNull(type, "type"), attributes);
    }

    /**
     * {@return whether a mod declared attributes for this type}
     *
     * @param type the entity type
     */
    public static boolean has(EntityType<?> type) {
        return DECLARED.containsKey(type);
    }

    /**
     * {@return a mod's attributes for this type, or {@code null}}
     *
     * <p>Built on the first ask and kept, so the cost lands once and every
     * entity of the type shares one supplier, as vanilla's do.
     *
     * @param type the entity type
     */
    public static AttributeSupplier get(EntityType<?> type) {
        AttributeSupplier built = BUILT.get(type);
        if (built != null) {
            return built;
        }
        Supplier<AttributeSupplier.Builder> declared = DECLARED.get(type);
        if (declared == null) {
            return null;
        }
        built = declared.get().build();
        BUILT.put(type, built);
        return built;
    }
}
