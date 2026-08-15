package fr.d4emon.fenix.registry.worldgen;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * The door a mixin opens onto a biome's spawn table.
 *
 * <p>Outside the mixin package on purpose: Mixin merges a mixin class into its
 * target and then refuses to load the mixin itself, so an interface declared
 * beside one could never be cast to. This one is ordinary Java that
 * {@code MobSpawnSettings} comes to implement.
 *
 * @see BiomeModifications#addSpawn
 */
public interface BiomeSpawnAccess {

    /**
     * Adds one entry to this biome's spawn table.
     *
     * @param category what kind of spawning cap it counts against
     * @param entry    the mob, and how many appear at a time
     * @param weight   how likely it is against everything else in that category
     */
    void fenix$addSpawn(MobCategory category, MobSpawnSettings.SpawnerData entry, int weight);

    /**
     * Takes every entry for one mob out of this biome's spawn table.
     *
     * @param category which spawning group to look in
     * @param entity   the mob to remove
     * @return how many entries went, so a caller can say whether anything did
     */
    int fenix$removeSpawn(MobCategory category, EntityType<?> entity);
}
