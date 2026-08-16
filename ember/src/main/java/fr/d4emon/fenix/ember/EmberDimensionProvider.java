package fr.d4emon.fenix.ember;

/**
 * Writes dimensions, and the dimension types they are made of.
 *
 * <p>Two files, and both are needed. A dimension <em>type</em> says what the
 * place is like — how tall, whether there is a sky, how fast a clock runs. A
 * <em>dimension</em> says that such a place exists and how its terrain is
 * generated. A type on its own is a description of nowhere.
 *
 * <pre>{@code
 * @Generator
 * public final class ModDimensions extends EmberDimensionProvider {
 *     @Override
 *     protected void dimensions() {
 *         dimensionType("ruby_realm")
 *                 .height(256).minY(0).logicalHeight(256)
 *                 .skylight(false).ceiling(true)
 *                 .ambientLight(0.1f)
 *                 .save();
 *
 *         dimension("ruby_realm", "mymod:ruby_realm")
 *                 .fixedBiome("mymod:ruby_caverns")
 *                 .save();
 *     }
 * }
 * }</pre>
 *
 * <p>This is also the only way a mod's own biome is ever reached. Minecraft
 * has no datapack way to add a biome to the overworld's noise settings, so a
 * biome that is not in a dimension of its own generates nowhere at all.
 */
public abstract class EmberDimensionProvider extends EmberProvider {

    /** For subclasses. */
    protected EmberDimensionProvider() {
    }

    /** Describes the dimensions. */
    protected abstract void dimensions();

    @Override
    protected final void run() {
        dimensions();
    }

    /**
     * Starts a dimension type: what the place is like.
     *
     * @param name the path part of its id
     * @return a builder; call {@code save()} when done
     */
    protected final Type dimensionType(String name) {
        return new Type(this, name);
    }

    /**
     * Starts a dimension: that the place exists, and how its terrain is made.
     *
     * @param name the path part of its id, which is what {@code /execute in}
     *             takes
     * @param type the dimension type's id
     * @return a builder; call {@code save()} when done
     */
    protected final Dimension dimension(String name, String type) {
        return new Dimension(this, name, type);
    }

    private void save(String directory, String name, String json) {
        output().data(directory + "/" + name + ".json", json);
    }

    /** Collects one dimension type. */
    public static final class Type {

        private final EmberDimensionProvider provider;
        private final String name;

        private int height = 384;
        private int minY = -64;
        private int logicalHeight = 384;
        private boolean skylight = true;
        private boolean ceiling;
        private float ambientLight;
        private double coordinateScale = 1.0;
        private String infiniburn = "#minecraft:infiniburn_overworld";
        private String clock = "minecraft:overworld";
        private String skyColor = "#78a7ff";
        private String fogColor = "#c0d8ff";

        private Type(EmberDimensionProvider provider, String name) {
            this.provider = provider;
            this.name = name;
        }

        /**
         * How many blocks tall the world is.
         *
         * <p>Must be a multiple of 16, and {@code minY + height} must not pass
         * 2032. The game refuses the file otherwise, which is one of the few
         * worldgen mistakes that does say something.
         *
         * @param blocks the height
         * @return this builder
         */
        public Type height(int blocks) {
            this.height = blocks;
            return this;
        }

        /**
         * @param y the lowest block, a multiple of 16
         * @return this builder
         */
        public Type minY(int y) {
            this.minY = y;
            return this;
        }

        /**
         * How high the world behaves as though it ends.
         *
         * <p>Below the real height this is what stops a nether portal or a map
         * from working above it. Usually the same as the height.
         *
         * @param blocks the logical height
         * @return this builder
         */
        public Type logicalHeight(int blocks) {
            this.logicalHeight = blocks;
            return this;
        }

        /**
         * @param has whether the sky lights the place
         * @return this builder
         */
        public Type skylight(boolean has) {
            this.skylight = has;
            return this;
        }

        /**
         * @param has whether there is a solid roof, as the nether has
         * @return this builder
         */
        public Type ceiling(boolean has) {
            this.ceiling = has;
            return this;
        }

        /**
         * @param level how much light there is with no source, 0 to 1
         * @return this builder
         */
        public Type ambientLight(float level) {
            this.ambientLight = level;
            return this;
        }

        /**
         * How far a block here is in overworld blocks.
         *
         * <p>The nether's 8 is what makes a portal cover eight times the
         * ground.
         *
         * @param scale the scale
         * @return this builder
         */
        public Type coordinateScale(double scale) {
            this.coordinateScale = scale;
            return this;
        }

        /**
         * @param tag the block tag fire burns forever on
         * @return this builder
         */
        public Type infiniburn(String tag) {
            this.infiniburn = tag;
            return this;
        }

        /**
         * @param id the clock that decides day and night here
         * @return this builder
         */
        public Type clock(String id) {
            this.clock = id;
            return this;
        }

        /**
         * @param sky the sky's colour, as {@code #rrggbb}
         * @param fog the fog's colour
         * @return this builder
         */
        public Type colors(String sky, String fog) {
            this.skyColor = sky;
            this.fogColor = fog;
            return this;
        }

        /** Writes the dimension type. */
        public void save() {
            if (height % 16 != 0 || minY % 16 != 0) {
                throw new IllegalStateException(
                        name + ": height and min_y are both multiples of 16, and " + height
                                + " / " + minY + " are not");
            }

            provider.save("dimension_type", name, """
                    {
                      "ambient_light": %s,
                      "attributes": {
                        "minecraft:visual/sky_color": %s,
                        "minecraft:visual/fog_color": %s
                      },
                      "coordinate_scale": %s,
                      "default_clock": %s,
                      "has_ceiling": %b,
                      "has_ender_dragon_fight": false,
                      "has_skylight": %b,
                      "height": %d,
                      "infiniburn": %s,
                      "logical_height": %d,
                      "min_y": %d,
                      "monster_spawn_block_light_limit": 0,
                      "monster_spawn_light_level": {
                        "type": "minecraft:uniform",
                        "max_inclusive": 7,
                        "min_inclusive": 0
                      }
                    }
                    """.formatted(EmberOutput.decimal(ambientLight),
                    EmberOutput.quote(skyColor), EmberOutput.quote(fogColor),
                    EmberOutput.decimal(coordinateScale), EmberOutput.quote(clock),
                    ceiling, skylight, height, EmberOutput.quote(infiniburn),
                    logicalHeight, minY));
        }
    }

    /** Collects one dimension. */
    public static final class Dimension {

        private final EmberDimensionProvider provider;
        private final String name;
        private final String type;

        private String biome;
        private String settings = "minecraft:overworld";

        private Dimension(EmberDimensionProvider provider, String name, String type) {
            this.provider = provider;
            this.name = name;
            this.type = type;
        }

        /**
         * Fills the whole dimension with one biome.
         *
         * <p>The simplest biome source there is, and the one that makes a mod's
         * own biome reachable at all: everything else needs a noise parameter
         * list, which is a large file and a research project.
         *
         * @param id the biome's id
         * @return this builder
         */
        public Dimension fixedBiome(String id) {
            this.biome = id;
            return this;
        }

        /**
         * Which noise settings shape the terrain.
         *
         * @param id a noise settings id, such as {@code minecraft:caves}
         * @return this builder
         */
        public Dimension noiseSettings(String id) {
            this.settings = id;
            return this;
        }

        /** Writes the dimension. */
        public void save() {
            if (biome == null) {
                throw new IllegalStateException(
                        name + " has no biome source, so there is nothing to generate");
            }

            provider.save("dimension", name, """
                    {
                      "type": %s,
                      "generator": {
                        "type": "minecraft:noise",
                        "settings": %s,
                        "biome_source": {
                          "type": "minecraft:fixed",
                          "biome": %s
                        }
                      }
                    }
                    """.formatted(EmberOutput.quote(type), EmberOutput.quote(settings),
                    EmberOutput.quote(biome)));
        }
    }
}
