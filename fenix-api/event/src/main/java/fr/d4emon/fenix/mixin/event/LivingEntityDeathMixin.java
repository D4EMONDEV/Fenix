package fr.d4emon.fenix.mixin.event;

import fr.d4emon.fenix.event.EntityEvents;
import fr.d4emon.fenix.event.PlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Anything living dying — players included, which is why both events fire from
 * one place rather than from two that could drift apart.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDeathMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    LivingEntityDeathMixin() {
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true, remap = false)
    private void fenix$onHurt(ServerLevel level, DamageSource source, float amount,
                              CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (EntityEvents.HURT.fire(new EntityEvents.Hurt(self, level, source, amount))
                .isCancelled()) {
            // false is what vanilla returns when a hit does not land — immune,
            // invulnerable, already dead. Anything else and the caller believes
            // damage happened.
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void fenix$died(DamageSource cause, CallbackInfo info) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            // The client plays out a death it was told about; acting on it
            // would double whatever the server already did.
            return;
        }
        EntityEvents.DIED.fire(new EntityEvents.Died(self, cause));
        if (self instanceof ServerPlayer player) {
            PlayerEvents.DIED.fire(new PlayerEvents.Died(player, cause));
        }
    }
}
