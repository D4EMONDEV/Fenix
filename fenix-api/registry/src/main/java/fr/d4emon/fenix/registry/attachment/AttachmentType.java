package fr.d4emon.fenix.registry.attachment;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A named piece of data a mod attaches to something the game already owns —
 * an entity or a block entity — without editing that class.
 *
 * <p>This is the identity and the rules of one such piece: what it is called,
 * what value it starts at, and how it is written to disk. Get one from
 * {@code Registrar.attachment} and keep it in a {@code static final} field, the
 * way a {@link fr.d4emon.fenix.registry.Holder} is kept:
 *
 * <pre>{@code
 * public static final AttachmentType<Integer> MANA =
 *         REGISTRAR.attachment("mana", () -> 0, Codec.INT);
 * }</pre>
 *
 * <p>Then reach for it through {@link Attachments}.
 *
 * @param <T> the type of the attached value
 */
public final class AttachmentType<T> {

    private final Identifier id;
    private final Supplier<T> defaultValue;
    private final @Nullable Codec<T> codec;

    AttachmentType(Identifier id, Supplier<T> defaultValue, @Nullable Codec<T> codec) {
        this.id = Objects.requireNonNull(id, "id");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.codec = codec;
    }

    /** {@return the id this attachment is registered and saved under} */
    public Identifier id() {
        return id;
    }

    /**
     * {@return a fresh default value}
     *
     * <p>Called for the answer when nothing has been attached yet. It builds a
     * new value each time rather than sharing one, so a mutable default handed
     * to two entities does not become shared state between them.
     */
    public T createDefault() {
        return defaultValue.get();
    }

    /** {@return the codec that saves this, or {@code null} if it does not persist} */
    public @Nullable Codec<T> codec() {
        return codec;
    }

    /** {@return whether this attachment survives saving and loading} */
    public boolean persistent() {
        return codec != null;
    }

    @SuppressWarnings("unchecked")
    T cast(Object value) {
        return (T) value;
    }

    @Override
    public String toString() {
        return "AttachmentType[" + id + (codec == null ? ", transient]" : "]");
    }
}
