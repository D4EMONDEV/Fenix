package fr.d4emon.fenix.ember;

import fr.d4emon.fenix.registry.Holder;
import net.minecraft.world.level.block.Block;

import java.util.Locale;
import java.util.Objects;

/**
 * Writes the two files an ore needs to generate.
 *
 * <pre>{@code
 * @Generator
 * public final class ModOres extends EmberOreProvider {
 *     @Override
 *     protected void ores() {
 *         ore("ruby_ore", ModBlocks.RUBY_ORE, ModBlocks.DEEPSLATE_RUBY_ORE)
 *                 .veinSize(9)
 *                 .veinsPerChunk(8)
 *                 .between(-64, 64)
 *                 .write();
 *     }
 * }
 * }</pre>
 *
 * <p>Two files, because the game splits the question in two: a
 * <em>configured feature</em> says what to place — which block, in veins of
 * what size, replacing what — and a <em>placed feature</em> says where, how
 * often and between which heights. Neither does anything on its own.
 *
 * <p>Nor do the two together: a placed feature that no biome refers to is never
 * run. Say which biomes want it with {@code BiomeModifications.addFeature}.
 *
 * <p>The stone and deepslate blocks are separate on purpose. Vanilla's ores
 * each have a deepslate variant because the two replace different blocks, and
 * an ore that skips it appears as stone-textured lumps below y=0.
 */
public abstract class EmberOreProvider extends EmberProvider {

    /** For subclasses. */
    protected EmberOreProvider() {
    }

    /** Describes the ores. */
    protected abstract void ores();

    @Override
    protected final void run() {
        ores();
    }

    /**
     * Starts describing an ore.
     *
     * @param name      the name both files take, and the one
     *                  {@code BiomeModifications} refers to
     * @param stone     the block placed in stone
     * @param deepslate the block placed in deepslate, below y=0
     * @return a builder; nothing is written until {@code write()}
     * @throws NullPointerException if any argument is {@code null}
     */
    protected final Ore ore(String name, Holder<Block> stone, Holder<Block> deepslate) {
        return new Ore(name, Objects.requireNonNull(stone, "stone"),
                Objects.requireNonNull(deepslate, "deepslate"));
    }

    /**
     * Starts describing an ore with no deepslate variant.
     *
     * <p>Right for an ore that only occurs above y=0. Below it, the stone block
     * simply never replaces deepslate and the ore stops.
     *
     * @param name  the name both files take
     * @param stone the block placed in stone
     * @return a builder; nothing is written until {@code write()}
     * @throws NullPointerException if any argument is {@code null}
     */
    protected final Ore ore(String name, Holder<Block> stone) {
        return new Ore(name, Objects.requireNonNull(stone, "stone"), null);
    }

    /** Collects an ore's numbers, then writes both files. */
    public final class Ore {

        private final String name;
        private final Holder<Block> stone;
        private final Holder<Block> deepslate;

        private int veinSize = 9;
        private int veinsPerChunk = 8;
        private int minY = -64;
        private int maxY = 64;
        private float discardOnAirExposure;

        private Ore(String name, Holder<Block> stone, Holder<Block> deepslate) {
            this.name = name;
            this.stone = stone;
            this.deepslate = deepslate;
        }

        /**
         * How many blocks a vein holds at most.
         *
         * @param blocks 0 to 64; vanilla's iron is 9, its diamond 8
         * @return this builder
         */
        public Ore veinSize(int blocks) {
            veinSize = blocks;
            return this;
        }

        /**
         * How many veins are attempted per chunk.
         *
         * <p>Attempted, not placed: a vein whose height lands in the air or in
         * an existing cave places less than its size, or nothing.
         *
         * @param count the number of attempts
         * @return this builder
         */
        public Ore veinsPerChunk(int count) {
            veinsPerChunk = count;
            return this;
        }

        /**
         * The heights it generates between, inclusive.
         *
         * @param min the lowest y
         * @param max the highest y
         * @return this builder
         */
        public Ore between(int min, int max) {
            minY = min;
            maxY = max;
            return this;
        }

        /**
         * How often a block touching air is dropped from the vein.
         *
         * <p>What keeps ore out of cave walls. Vanilla uses 0.5 for the ores
         * that would otherwise be visible from every cave, and 0 for the rest.
         *
         * @param chance 0 to 1
         * @return this builder
         */
        public Ore discardOnAirExposure(float chance) {
            discardOnAirExposure = chance;
            return this;
        }

        /** Writes both files. */
        public void write() {
            String id = modId() + ":" + name;

            String targets = deepslate == null
                    ? target("minecraft:stone_ore_replaceables", stone)
                    : target("minecraft:stone_ore_replaceables", stone) + ",\n"
                            + target("minecraft:deepslate_ore_replaceables", deepslate);

            output().data("worldgen/configured_feature/" + name + ".json", """
                    {
                      "type": "minecraft:ore",
                      "config": {
                        "size": %d,
                        "discard_chance_on_air_exposure": %s,
                        "targets": [
                    %s
                        ]
                      }
                    }
                    """.formatted(veinSize, format(discardOnAirExposure), targets));

            // in_square spreads the vein within the chunk; without it every
            // vein in every chunk starts at the same corner, in visible rows.
            output().data("worldgen/placed_feature/" + name + ".json", """
                    {
                      "feature": "%s",
                      "placement": [
                        { "type": "minecraft:count", "count": %d },
                        { "type": "minecraft:in_square" },
                        {
                          "type": "minecraft:height_range",
                          "height": {
                            "type": "minecraft:uniform",
                            "min_inclusive": { "absolute": %d },
                            "max_inclusive": { "absolute": %d }
                          }
                        },
                        { "type": "minecraft:biome" }
                      ]
                    }
                    """.formatted(id, veinsPerChunk, minY, maxY));
        }

        private String target(String tag, Holder<Block> block) {
            // Indented to sit inside the "targets" array of the block above.
            // A text block strips its own incidental whitespace, so the
            // indentation has to be written rather than inherited.
            return """
                    {
                      "target": { "predicate_type": "minecraft:tag_match", "tag": "%s" },
                      "state": { "Name": "%s" }
                    }"""
                    .formatted(tag, block.id())
                    .indent(6)
                    .stripTrailing();
        }
    }

    /** Trims a float to what JSON needs, so 0.0 does not read as 0.0000000. */
    private static String format(float value) {
        return value == Math.rint(value)
                ? String.format(Locale.ROOT, "%.1f", value)
                : String.valueOf(value);
    }
}
