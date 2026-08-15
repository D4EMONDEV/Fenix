package fr.d4emon.fenix.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.core.registries.BuiltInRegistries;
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
     * {@return the game's own holder for an attribute this mod registered}
     *
     * <p>{@code AttributeSupplier.Builder.add} takes {@code net.minecraft.core.Holder},
     * and {@link Registrar#attribute} hands back Fenix's — the two are different
     * types doing the same job, and without this a mod has to write
     * {@code BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute.get())} in the
     * middle of a builder chain, which is the sort of ceremony Fenix exists to
     * absorb.
     *
     * <p>They cannot simply be the same type. Fenix's holder is handed back
     * before the attribute exists, so that content can be declared in a field;
     * the game's is a reference the registry creates at registration and there
     * is nothing to hand back until then. This bridges them once bound.
     *
     * <pre>{@code
     * Mob.createMobAttributes().add(EntityAttributes.holder(ModContent.RUBY_CHARGE), 3.0)
     * }</pre>
     *
     * @param attribute an attribute from {@link Registrar#attribute}
     * @throws IllegalStateException if it is not registered yet — which means
     *                               this was read before the registrar applied
     */
    public static net.minecraft.core.Holder<Attribute> holder(Holder<Attribute> attribute) {
        Objects.requireNonNull(attribute, "attribute");
        return BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute.get());
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
