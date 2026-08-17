package fr.d4emon.fenix.example.data;

import fr.d4emon.fenix.ember.EmberDimensionProvider;
import fr.d4emon.fenix.ember.Generator;

/**
 * A dimension made of the mod's own biome.
 *
 * <p>This is what makes {@code example-mod:ruby_caverns} reachable. Without it
 * the biome file is complete, loads, and generates nowhere — Minecraft has no
 * datapack way to add a biome to the overworld, so a dimension of its own is
 * the only door.
 */
@Generator
public final class ModDimensions extends EmberDimensionProvider {

    /** Instantiated by Ember from the compile-time index. */
    public ModDimensions() {
    }

    @Override
    protected void dimensions() {
        // Roofed and unlit, because the biome it holds is a cavern.
        dimensionType("ruby_realm")
                .height(256)
                .minY(0)
                .logicalHeight(256)
                .skylight(false)
                .ceiling(true)
                .ambientLight(0.1f)
                .colors("#3a2129", "#2b171d")
                .save();

        // A fixed biome source: the whole dimension is ruby caverns. Anything
        // richer needs a noise parameter list, which is a large file and a
        // research project of its own.
        // Its own rock rather than vanilla's. Borrowing minecraft:caves was
        // right until the realm wanted to be made of something — a dimension
        // whose ground is the mod's own block is the first thing anyone
        // notices, and the last thing a borrowed setting can give.
        noiseSettings("ruby_realm")
                .defaultBlock("example-mod:ruby_block")
                .defaultFluid("minecraft:water")
                .shape(0, 256)
                .ground(96)
                .seaLevel(48)
                .oreVeins(false)
                .save();

        dimension("ruby_realm", "example-mod:ruby_realm")
                .fixedBiome("example-mod:ruby_caverns")
                .noiseSettings("example-mod:ruby_realm")
                .save();
    }
}
