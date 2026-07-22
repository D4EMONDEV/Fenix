package fr.d4emon.fenix.registry.worldgen;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** The selectors most modifications want. */
public final class BiomeSelectors {

    private BiomeSelectors() {
    }

    /** {@return a selector matching every biome} */
    public static BiomeSelector all() {
        return biome -> true;
    }

    /**
     * {@return a selector matching every overworld biome}
     *
     * <p>By tag, so it covers biomes added by datapacks and by other mods. This
     * is what an ore usually wants.
     */
    public static BiomeSelector overworld() {
        return tagged(BiomeTags.IS_OVERWORLD);
    }

    /** {@return a selector matching every Nether biome} */
    public static BiomeSelector nether() {
        return tagged(BiomeTags.IS_NETHER);
    }

    /** {@return a selector matching every End biome} */
    public static BiomeSelector end() {
        return tagged(BiomeTags.IS_END);
    }

    /**
     * {@return a selector matching biomes carrying a tag}
     *
     * @param tag the tag
     * @throws NullPointerException if {@code tag} is {@code null}
     */
    public static BiomeSelector tagged(TagKey<Biome> tag) {
        Objects.requireNonNull(tag, "tag");
        return biome -> biome.hasTag(tag);
    }

    /**
     * {@return a selector matching exactly the biomes named}
     *
     * <p>Precise, and brittle in proportion: a biome nobody has added yet is a
     * biome this will not match. Prefer {@link #tagged} where a tag says what
     * you mean.
     *
     * @param keys the biomes
     * @throws NullPointerException if {@code keys} is {@code null}
     */
    @SafeVarargs
    public static BiomeSelector only(ResourceKey<Biome>... keys) {
        Set<ResourceKey<Biome>> wanted = Set.copyOf(List.of(keys));
        return biome -> wanted.contains(biome.key());
    }
}
