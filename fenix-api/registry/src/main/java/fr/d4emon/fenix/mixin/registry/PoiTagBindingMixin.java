package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.VillagerJobSites;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

/**
 * Says, out loud, when a mod's villager profession can never be taken.
 *
 * <p>Tags arrive with the datapacks, long after registration, so the question
 * "is this job site findable" has no answer at the moment a mod registers one.
 * It has one here: this is where the tags are bound to the registry, and the
 * first instant the check is possible.
 *
 * <p>Reads nothing and changes nothing — it lets vanilla bind its tags and then
 * asks the registry a question, which is why it sits at the tail rather than
 * anywhere it could interfere.
 */
@Mixin(MappedRegistry.class)
public class PoiTagBindingMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    PoiTagBindingMixin() {
    }

    @Inject(method = "bindTags", at = @At("TAIL"))
    @SuppressWarnings("unchecked")
    private void fenix$checkJobSitesAreAcquirable(Map<TagKey<?>, List<Holder<?>>> pendingTags,
                                                  CallbackInfo info) {
        Registry<?> registry = (Registry<?>) (Object) this;
        if (registry.key() == VillagerJobSites.registryKey()) {
            VillagerJobSites.verify((Registry<PoiType>) registry);
        }
    }
}
