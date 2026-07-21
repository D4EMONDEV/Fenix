package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.EntityAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a mod's entities have default attributes.
 *
 * <p>Vanilla's table is an {@code ImmutableMap}, so nothing can be added to
 * it. Consulting a second table before it costs one lookup and avoids
 * replacing vanilla's — which would also mean building it during bootstrap,
 * before the attribute registry is bound.
 *
 * <p>What this prevents is not subtle: a {@code LivingEntity} asks for its
 * attributes while it is being constructed, so an entity that is missing dies
 * on the spot, inside vanilla, nowhere near the mod that registered it.
 */
@Mixin(DefaultAttributes.class)
public class DefaultAttributesMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    DefaultAttributesMixin() {
    }

    @Inject(method = "getSupplier", at = @At("HEAD"), cancellable = true)
    private static void fenix$modSupplier(EntityType<? extends LivingEntity> type,
                                          CallbackInfoReturnable<AttributeSupplier> info) {
        AttributeSupplier declared = EntityAttributes.get(type);
        if (declared != null) {
            info.setReturnValue(declared);
        }
    }

    @Inject(method = "hasSupplier", at = @At("HEAD"), cancellable = true)
    private static void fenix$modHasSupplier(EntityType<?> type,
                                             CallbackInfoReturnable<Boolean> info) {
        if (EntityAttributes.has(type)) {
            info.setReturnValue(true);
        }
    }
}
