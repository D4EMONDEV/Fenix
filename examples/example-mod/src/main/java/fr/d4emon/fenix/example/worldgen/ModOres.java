package fr.d4emon.fenix.example.worldgen;

import fr.d4emon.fenix.example.registry.ModBlocks;
import fr.d4emon.fenix.example.registry.ModContent;

import fr.d4emon.fenix.ember.EmberOreProvider;
import fr.d4emon.fenix.ember.Generator;

/**
 * Where ruby ore generates.
 *
 * <p>This writes the two files that say what the ore is and where it may go.
 * Which biomes actually want it is a third thing, and it is code:
 * {@link ModContent#register()} says {@code overworld()}.
 */
@Generator
public final class ModOres extends EmberOreProvider {

    /** Instantiated by Ember from the compile-time index. */
    public ModOres() {
    }

    @Override
    protected void ores() {
        // Roughly diamond's rarity, but spread over the whole stone column
        // rather than hidden at the bottom, so it is findable while testing.
        ore("ruby_ore", ModBlocks.RUBY_ORE, ModBlocks.DEEPSLATE_RUBY_ORE)
                .veinSize(6)
                .veinsPerChunk(4)
                .between(-48, 48)
                .discardOnAirExposure(0.5f)
                .write();

        // The two files the mod's own feature needs. Without them it is
        // registered code that nothing ever calls.
        placedFeature("ruby_spire", "example-mod:ruby_spire", "{}", """
                [
                    {"type": "minecraft:rarity_filter", "chance": 90},
                    {"type": "minecraft:in_square"},
                    {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
                    {"type": "minecraft:biome"}
                  ]""");

        // A tree, so the mod's own decorator has something to decorate. The
        // trunk and foliage are vanilla's shapes; only the decorator is ours,
        // which is the cheap half of a custom tree.
        placedFeature("ruby_tree", "minecraft:tree", """
                {
                    "trunk_provider": {"type": "minecraft:simple_state_provider",
                      "state": {"Name": "example-mod:ruby_log"}},
                    "below_trunk_provider": {"type": "minecraft:simple_state_provider",
                      "state": {"Name": "example-mod:ruby_log"}},
                    "trunk_placer": {"type": "example-mod:ruby_spiral",
                      "base_height": 5, "height_rand_a": 2, "height_rand_b": 0},
                    "foliage_provider": {"type": "minecraft:simple_state_provider",
                      "state": {"Name": "minecraft:oak_leaves", "Properties": {
                        "distance": "7", "persistent": "false", "waterlogged": "false"}}},
                    "foliage_placer": {"type": "minecraft:blob_foliage_placer",
                      "radius": 2, "offset": 0, "height": 3},
                    "dirt_provider": {"type": "minecraft:simple_state_provider",
                      "state": {"Name": "minecraft:dirt"}},
                    "minimum_size": {"type": "minecraft:two_layers_feature_size",
                      "limit": 1, "lower_size": 0, "upper_size": 1},
                    "decorators": [
                      {"type": "example-mod:ruby_clusters", "chance": 4}
                    ],
                    "ignore_vines": true
                  }""", """
                [
                    {"type": "minecraft:rarity_filter", "chance": 60},
                    {"type": "minecraft:in_square"},
                    {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
                    {"type": "minecraft:biome"}
                  ]""");
    }
}
