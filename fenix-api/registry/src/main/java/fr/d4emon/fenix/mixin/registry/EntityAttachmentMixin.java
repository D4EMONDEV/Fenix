package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.attachment.AttachmentHolder;
import fr.d4emon.fenix.registry.attachment.AttachmentStorage;
import fr.d4emon.fenix.registry.attachment.AttachmentType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Makes every entity an {@link AttachmentHolder}, and saves what it carries.
 *
 * <p>The store hooks the tail of the two methods every entity already uses for
 * its own persistence — so a mod's attachment is written to the chunk and read
 * back from it exactly when the entity is, with no separate storage and no
 * lifecycle for a mod to get wrong.
 *
 * <p>{@code AttachmentStorage} is called only through this and its block-entity
 * twin: the whole point is that {@code Entity} never knew the data was there.
 */
@Mixin(net.minecraft.world.entity.Entity.class)
public abstract class EntityAttachmentMixin implements AttachmentHolder {

    // Null until the first attachment is asked for: an entity that carries none
    // — nearly all of them — costs one field and not a map.
    @Unique
    private Map<AttachmentType<?>, Object> fenix$attachmentData;

    /** Never called — a mixin's constructors are discarded when it is merged. */
    EntityAttachmentMixin() {
    }

    @Override
    public Map<AttachmentType<?>, Object> fenix$attachments() {
        if (fenix$attachmentData == null) {
            fenix$attachmentData = new IdentityHashMap<>();
        }
        return fenix$attachmentData;
    }

    @Inject(method = "saveWithoutId", at = @At("TAIL"))
    private void fenix$saveAttachments(ValueOutput output, CallbackInfo info) {
        if (fenix$attachmentData != null && !fenix$attachmentData.isEmpty()) {
            AttachmentStorage.save(fenix$attachmentData, output);
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void fenix$loadAttachments(ValueInput input, CallbackInfo info) {
        AttachmentStorage.load(fenix$attachments(), input);
    }
}
