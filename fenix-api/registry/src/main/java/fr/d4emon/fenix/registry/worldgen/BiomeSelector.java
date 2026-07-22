package fr.d4emon.fenix.registry.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Decides which biomes a modification applies to.
 *
 * <p>Called once per biome, after datapacks have loaded, so it sees whatever
 * biomes are actually in the world — including other mods' — rather than a list
 * written down in advance.
 */
@FunctionalInterface
public interface BiomeSelector {

    /**
     * @param biome the biome being considered
     * @return {@code true} to modify it
     */
    boolean test(BiomeSelector.Context biome);

    /**
     * What a selector gets to look at.
     *
     * @param key    the biome's id
     * @param holder the biome itself, with its tags already bound
     */
    record Context(ResourceKey<Biome> key, Holder<Biome> holder) {

        /**
         * {@return whether the biome carries a tag}
         *
         * <p>Tags are the right question to ask nearly always: {@code
         * is_overworld} covers every overworld biome a datapack or another mod
         * adds, and naming biomes one by one does not.
         *
         * @param tag the tag
         */
        public boolean hasTag(TagKey<Biome> tag) {
            return holder.is(tag);
        }
    }
}
