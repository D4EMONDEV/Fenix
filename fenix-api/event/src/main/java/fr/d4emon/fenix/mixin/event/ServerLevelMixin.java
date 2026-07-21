package fr.d4emon.fenix.mixin.event;

import fr.d4emon.fenix.event.EntityEvents;
import fr.d4emon.fenix.event.Flow;
import fr.d4emon.fenix.event.LevelEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A level being saved, and entities trying to enter it. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    ServerLevelMixin() {
    }

    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void fenix$spawning(Entity entity, CallbackInfoReturnable<Boolean> info) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (EntityEvents.SPAWNING.fire(new EntityEvents.Spawning(entity, self)) == Flow.CANCEL) {
            // False, not true: the caller is being told the entity was not
            // added, which is the truth and what vanilla checks.
            info.setReturnValue(false);
        }
    }

    @Inject(method = "save", at = @At("HEAD"))
    private void fenix$saving(ProgressListener progress, boolean flush, boolean skip,
                              CallbackInfo info) {
        LevelEvents.SAVING.fire(new LevelEvents.Of((ServerLevel) (Object) this));
    }
}
