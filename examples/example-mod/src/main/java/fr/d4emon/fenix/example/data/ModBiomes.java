package fr.d4emon.fenix.example.data;

import fr.d4emon.fenix.ember.EmberBiomeProvider;
import fr.d4emon.fenix.ember.Generator;

/**
 * A biome of the mod's own.
 *
 * <p>Writing this file is not the same as the biome appearing in a world. It
 * is a complete, valid biome that nothing places — and Minecraft has no
 * datapack way to add one to the overworld's noise settings. Reaching it needs
 * a dimension of its own or a mixin into the biome source, neither of which
 * this demo does.
 *
 * <p>It is here because the file is the part Fenix generates, and because
 * `/locate biome` proves it loaded even when nothing generates it.
 */
@Generator
public final class ModBiomes extends EmberBiomeProvider {

    /** Instantiated by Ember from the compile-time index. */
    public ModBiomes() {
    }

    @Override
    protected void biomes() {
        biome("ruby_caverns")
                .temperature(0.7f)
                .downfall(0.3f)
                .precipitation(false)
                .skyColor("#3a2129")
                .fogColor("#2b171d")
                .waterColor("#8a2846")
                .waterFogColor("#5c1a2e")
                .grassColor("#6b4a52")
                .foliageColor("#7a5560")
                .carver("minecraft:cave")
                .carver("minecraft:canyon")
                // The mod's own ore, in the step ores belong to. Named rather
                // than numbered: at index 6 in the raw file, and an off-by-one
                // would generate it among the lakes.
                .feature(Step.UNDERGROUND_ORES, "example-mod:ruby_ore")
                .spawn("creature", "example-mod:ruby_sprite", 20, 2, 4)
                .spawn("monster", "minecraft:zombie", 40, 2, 4)
                .save();
    }
}
