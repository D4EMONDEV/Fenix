package fr.d4emon.fenix.registry.attachment;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Map;

/**
 * Writes attachments into a save and reads them back.
 *
 * <p>Called from the mixins on {@code Entity} and {@code BlockEntity}, right
 * where each writes its own extra data, so an attachment rides along with the
 * thing it is attached to and needs no storage of its own.
 *
 * <p>Each value is stored under its type's id, through its type's codec — the
 * same 26.2 machinery a data component uses. A type with no codec is transient
 * and simply skipped, which is what makes a transient attachment transient.
 *
 * <p>Public only so the mixins in the neighbouring package can reach it, the
 * same reason {@link Attachments} and {@code EntityAttributes} are — a mod has
 * no call to touch it.
 */
public final class AttachmentStorage {

    private AttachmentStorage() {
    }

    /**
     * Writes every persistent attachment a holder carries.
     *
     * @param attachments the holder's live attachment map
     * @param output      where the holder writes its own data
     */
    public static void save(Map<AttachmentType<?>, Object> attachments, ValueOutput output) {
        for (Map.Entry<AttachmentType<?>, Object> entry : attachments.entrySet()) {
            write(entry.getKey(), entry.getValue(), output);
        }
    }

    /**
     * Reads back every persistent attachment present in a save.
     *
     * @param attachments the holder's live attachment map, filled in place
     * @param input       where the holder reads its own data
     */
    public static void load(Map<AttachmentType<?>, Object> attachments, ValueInput input) {
        // Iterating the registered persistent types, not the keys in the save:
        // the save is read through a codec, and the codec is what a type has.
        // An attachment a mod removed since the world was saved is simply never
        // asked for, so its old value falls away, which is the right outcome.
        for (AttachmentType<?> type : Attachments.persistent()) {
            read(type, input, attachments);
        }
    }

    private static <T> void write(AttachmentType<T> type, Object value, ValueOutput output) {
        Codec<T> codec = type.codec();
        if (codec != null) {
            output.store(type.id().toString(), codec, type.cast(value));
        }
    }

    private static <T> void read(AttachmentType<T> type, ValueInput input,
                                 Map<AttachmentType<?>, Object> attachments) {
        Codec<T> codec = type.codec();
        if (codec != null) {
            input.read(type.id().toString(), codec).ifPresent(value -> attachments.put(type, value));
        }
    }
}
