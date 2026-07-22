package fr.d4emon.fenix.example.content;

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
    }
}
