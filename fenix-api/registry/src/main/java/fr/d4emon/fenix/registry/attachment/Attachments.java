package fr.d4emon.fenix.registry.attachment;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Reads and writes the data a mod has attached to entities and block entities.
 *
 * <pre>{@code
 * int mana = Attachments.get(player, ModAttachments.MANA);
 * Attachments.set(player, ModAttachments.MANA, mana - 10);
 * }</pre>
 *
 * <p>{@link #get} never writes: it answers the stored value or a fresh default,
 * and leaves nothing behind. So only what a mod explicitly {@link #set}s is kept
 * — and, for a persistent type, saved. That is deliberate: reading an
 * attachment off every zombie in a level should not quietly attach a default to
 * all of them and grow the save file.
 */
public final class Attachments {

    private static final Map<Identifier, AttachmentType<?>> BY_ID = new ConcurrentHashMap<>();

    // The persistent subset, iterated on load to find what to read back. A
    // copy-on-write list because it is written only during registration and
    // read on every entity and block-entity load.
    private static final List<AttachmentType<?>> PERSISTENT = new CopyOnWriteArrayList<>();

    private Attachments() {
    }

    /**
     * Registers an attachment type. Called by {@code Registrar.attachment}.
     *
     * @param <T>          the value type
     * @param id           its id, the key it saves under
     * @param defaultValue builds a fresh default
     * @param codec        how to save it, or {@code null} for a value that lives
     *                     only as long as the thing carrying it
     * @return the type
     * @throws IllegalStateException if the id is already taken
     */
    public static <T> AttachmentType<T> register(Identifier id, Supplier<T> defaultValue,
                                                 @Nullable Codec<T> codec) {
        AttachmentType<T> type = new AttachmentType<>(id, defaultValue, codec);
        if (BY_ID.putIfAbsent(id, type) != null) {
            throw new IllegalStateException("an attachment named " + id + " is already registered");
        }
        if (codec != null) {
            PERSISTENT.add(type);
        }
        return type;
    }

    /**
     * {@return an entity's value for an attachment, or a fresh default}
     *
     * @param <T>    the value type
     * @param entity the entity
     * @param type   the attachment
     */
    public static <T> T get(Entity entity, AttachmentType<T> type) {
        return get((AttachmentHolder) entity, type);
    }

    /**
     * {@return a block entity's value for an attachment, or a fresh default}
     *
     * @param <T>         the value type
     * @param blockEntity the block entity
     * @param type        the attachment
     */
    public static <T> T get(BlockEntity blockEntity, AttachmentType<T> type) {
        return get((AttachmentHolder) blockEntity, type);
    }

    /**
     * Sets an entity's value for an attachment.
     *
     * <p>For a persistent type this is what makes the value outlive the session:
     * the entity carries it, and it is written when the entity is saved.
     *
     * @param <T>    the value type
     * @param entity the entity
     * @param type   the attachment
     * @param value  the value
     */
    public static <T> void set(Entity entity, AttachmentType<T> type, T value) {
        set((AttachmentHolder) entity, type, value);
    }

    /**
     * Sets a block entity's value for an attachment.
     *
     * <p>A block entity has to be told it changed, or the change is never
     * written — call {@code setChanged()} after this, as you would after any
     * other edit to its state.
     *
     * @param <T>         the value type
     * @param blockEntity the block entity
     * @param type        the attachment
     * @param value       the value
     */
    public static <T> void set(BlockEntity blockEntity, AttachmentType<T> type, T value) {
        set((AttachmentHolder) blockEntity, type, value);
    }

    /**
     * {@return whether an entity has a value set for an attachment}
     *
     * @param entity the entity
     * @param type   the attachment
     */
    public static boolean has(Entity entity, AttachmentType<?> type) {
        return ((AttachmentHolder) entity).fenix$attachments().containsKey(type);
    }

    /**
     * {@return whether a block entity has a value set for an attachment}
     *
     * @param blockEntity the block entity
     * @param type        the attachment
     */
    public static boolean has(BlockEntity blockEntity, AttachmentType<?> type) {
        return ((AttachmentHolder) blockEntity).fenix$attachments().containsKey(type);
    }

    /** The persistent types, for the storage that reads them back on load. */
    static List<AttachmentType<?>> persistent() {
        return PERSISTENT;
    }

    private static <T> T get(AttachmentHolder holder, AttachmentType<T> type) {
        Objects.requireNonNull(type, "type");
        Object value = holder.fenix$attachments().get(type);
        return value != null ? type.cast(value) : type.createDefault();
    }

    private static <T> void set(AttachmentHolder holder, AttachmentType<T> type, T value) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
        holder.fenix$attachments().put(type, value);
    }
}
