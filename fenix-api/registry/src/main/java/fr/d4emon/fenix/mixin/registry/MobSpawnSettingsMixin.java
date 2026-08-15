package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.worldgen.BiomeSpawnAccess;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Opens a biome's spawn table so a mod can add to it.
 *
 * <p>The table is a {@code Map} of immutable lists built once when the biome is
 * loaded, which is right for the game and leaves a mod nowhere to put a mob.
 * The map is replaced rather than mutated: the one a biome is built with may be
 * immutable, and the loaded registries are shared, so writing through the
 * original would either throw or edit something another world is using.
 */
@Mixin(MobSpawnSettings.class)
public abstract class MobSpawnSettingsMixin implements BiomeSpawnAccess {

    @Mutable
    @Shadow
    @Final
    private Map<MobCategory, WeightedList<MobSpawnSettings.SpawnerData>> spawners;

    @Override
    public void fenix$addSpawn(MobCategory category, MobSpawnSettings.SpawnerData entry,
                               int weight) {
        // EnumMap rather than a copy of whatever came in: the game reads this
        // for every spawn attempt in every loaded chunk, and an EnumMap keyed
        // by a small enum is an array lookup.
        Map<MobCategory, WeightedList<MobSpawnSettings.SpawnerData>> replacement =
                new EnumMap<>(MobCategory.class);
        replacement.putAll(spawners);

        WeightedList<MobSpawnSettings.SpawnerData> existing =
                replacement.getOrDefault(category, WeightedList.of());
        List<Weighted<MobSpawnSettings.SpawnerData>> entries = new ArrayList<>(existing.unwrap());
        entries.add(new Weighted<>(entry, weight));

        replacement.put(category, WeightedList.of(entries));
        spawners = Map.copyOf(replacement);
    }

    @Override
    public int fenix$removeSpawn(MobCategory category, EntityType<?> entity) {
        WeightedList<MobSpawnSettings.SpawnerData> existing = spawners.get(category);
        if (existing == null) {
            return 0;
        }

        List<Weighted<MobSpawnSettings.SpawnerData>> kept = existing.unwrap().stream()
                .filter(weighted -> weighted.value().type() != entity)
                .toList();
        int removed = existing.unwrap().size() - kept.size();
        if (removed == 0) {
            // Nothing to do, and nothing to rebuild. Replacing the map anyway
            // would allocate one per biome per load for every removal that
            // matched nothing, which is most of them.
            return 0;
        }

        Map<MobCategory, WeightedList<MobSpawnSettings.SpawnerData>> replacement =
                new EnumMap<>(MobCategory.class);
        replacement.putAll(spawners);
        replacement.put(category, WeightedList.of(kept));
        spawners = Map.copyOf(replacement);
        return removed;
    }
}
