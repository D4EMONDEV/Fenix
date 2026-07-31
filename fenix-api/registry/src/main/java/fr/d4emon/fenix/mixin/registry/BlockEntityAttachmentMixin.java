package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.attachment.AttachmentHolder;
import fr.d4emon.fenix.registry.attachment.AttachmentStorage;
import fr.d4emon.fenix.registry.attachment.AttachmentType;
import net.minecraft.world.level.block.entity.BlockEntity;
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
 * Makes every block entity an {@link AttachmentHolder}, and saves what it
 * carries — the same arrangement as the entity mixin, at the two methods a
 * block entity uses for its own extra data.
 *
 * <p>One difference in use, not in code: a block entity is written only when it
 * is marked changed, so a mod that sets an attachment on one has to call
 * {@code setChanged()} afterwards, exactly as it would for any other edit. The
 * save hook here is faithful; whether the save happens is the block entity's own
 * rule, left alone on purpose.
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityAttachmentMixin implements AttachmentHolder {

    @Unique
    private Map<AttachmentType<?>, Object> fenix$attachmentData;

    /** Never called — a mixin's constructors are discarded when it is merged. */
    BlockEntityAttachmentMixin() {
    }

    @Override
    public Map<AttachmentType<?>, Object> fenix$attachments() {
        if (fenix$attachmentData == null) {
            fenix$attachmentData = new IdentityHashMap<>();
        }
        return fenix$attachmentData;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void fenix$saveAttachments(ValueOutput output, CallbackInfo info) {
        if (fenix$attachmentData != null && !fenix$attachmentData.isEmpty()) {
            AttachmentStorage.save(fenix$attachmentData, output);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void fenix$loadAttachments(ValueInput input, CallbackInfo info) {
        AttachmentStorage.load(fenix$attachments(), input);
    }
}
