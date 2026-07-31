package fr.d4emon.fenix.mixin.event;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import fr.d4emon.fenix.event.LootEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Fires {@link LootEvents#LOADING} for each loot table as it is read.
 *
 * <p>Loot tables became a datapack registry, so there is no single "load a loot
 * table" method to hook — they arrive as a directory scan, and the results are
 * registered a moment later out of a local map. This catches that map while it
 * is still a map: the scan takes it as a parameter, which is the only place the
 * tables are both parsed and still changeable.
 *
 * <p>The same scan reads predicates and item modifiers too, so the registry key
 * is checked before anything is touched.
 */
@Mixin(SimpleJsonResourceReloadListener.class)
public class LootTableLoadMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    LootTableLoadMixin() {
    }

    @Inject(method = "scanDirectory(Lnet/minecraft/server/packs/resources/ResourceManager;"
            + "Lnet/minecraft/resources/ResourceKey;Lcom/mojang/serialization/DynamicOps;"
            + "Lcom/mojang/serialization/Codec;Ljava/util/Map;)V",
            at = @At("TAIL"))
    private static void fenix$loadedLootTables(ResourceManager manager,
                                               ResourceKey<? extends Registry<?>> registryKey,
                                               DynamicOps<JsonElement> ops, Codec<?> codec,
                                               Map<Identifier, ?> result, CallbackInfo info) {
        if (registryKey != Registries.LOOT_TABLE || !LootEvents.LOADING.hasListeners()) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<Identifier, LootTable> tables = (Map<Identifier, LootTable>) result;
        // Replaced in place: the caller registers whatever is in this map, so a
        // rebuilt table has to go back under the same id.
        tables.replaceAll(LootEvents::fire);
    }
}
