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
        dimension("ruby_realm", "example-mod:ruby_realm")
                .fixedBiome("example-mod:ruby_caverns")
                .noiseSettings("minecraft:caves")
                .save();
    }
}
