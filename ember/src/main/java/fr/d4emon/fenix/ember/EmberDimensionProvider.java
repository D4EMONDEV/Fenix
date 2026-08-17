package fr.d4emon.fenix.ember;

import java.util.List;
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

    /**
     * Starts a set of noise settings: what the ground of a dimension is made
     * of, and where it stops.
     *
     * <p>A dimension can borrow vanilla's — {@code minecraft:caves} is what the
     * demo used first — and that is the right answer until the mod wants its
     * own rock. This is for when it does.
     *
     * @param name the path part of its id
     * @return a builder; call {@code save()} when done
     */
    protected final Noise noiseSettings(String name) {
        return new Noise(this, name);
    }

    /**
     * Collects one set of noise settings.
     *
     * <p>The part worth knowing: a noise router is fifteen density functions,
     * and every one of them is required. Most describe the overworld's climate
     * and mean nothing to a dimension with one biome, so they are written as
     * constant zero and only {@link #ground} is shaped. That is a real world —
     * not a rich one, and the escape hatch is there for a rich one.
     */
    public static final class Noise {

        /** The fourteen a simple dimension has no use for. */
        private static final List<String> FLAT = List.of(
                "barrier", "fluid_level_floodedness", "fluid_level_spread", "lava",
                "temperature", "vegetation", "continents", "erosion", "depth", "ridges",
                "preliminary_surface_level", "vein_toggle", "vein_ridged", "vein_gap");

        private final EmberDimensionProvider provider;
        private final String name;

        private String defaultBlock = "minecraft:stone";
        private String defaultFluid = "minecraft:water";
        private int seaLevel = 63;
        private int minY;
        private int height = 256;
        private int solidTo = 40;
        private boolean aquifers = true;
        private boolean oreVeins = true;
        private boolean mobs = true;
        private String router;
        private String surfaceRule;

        private Noise(EmberDimensionProvider provider, String name) {
            this.provider = provider;
            this.name = name;
        }

        /**
         * @param id the block everything below the surface is made of
         * @return this builder
         */
        public Noise defaultBlock(String id) {
            this.defaultBlock = id;
            return this;
        }

        /**
         * @param id the fluid that fills below the sea level
         * @return this builder
         */
        public Noise defaultFluid(String id) {
            this.defaultFluid = id;
            return this;
        }

        /**
         * @param y where that fluid stops
         * @return this builder
         */
        public Noise seaLevel(int y) {
            this.seaLevel = y;
            return this;
        }

        /**
         * The vertical extent generation works in.
         *
         * <p>Must agree with the dimension type using these settings. Where
         * they disagree the world generates to one and is bounded by the other,
         * which shows up as a floor you can fall through or a ceiling of void.
         *
         * @param minY   the lowest block
         * @param height how many blocks upward from there; a multiple of 16
         * @return this builder
         */
        public Noise shape(int minY, int height) {
            this.minY = minY;
            this.height = height;
            return this;
        }

        /**
         * Where solid ground gives way to air.
         *
         * @param y everything below is rock, everything above is open
         * @return this builder
         */
        public Noise ground(int y) {
            this.solidTo = y;
            return this;
        }

        /**
         * @param on whether water pockets form underground
         * @return this builder
         */
        public Noise aquifers(boolean on) {
            this.aquifers = on;
            return this;
        }

        /**
         * @param on whether copper and iron veins run through the rock
         * @return this builder
         */
        public Noise oreVeins(boolean on) {
            this.oreVeins = on;
            return this;
        }

        /**
         * @param on whether the generator spawns mobs as it builds chunks
         * @return this builder
         */
        public Noise mobGeneration(boolean on) {
            this.mobs = on;
            return this;
        }

        /**
         * A noise router written out, for a dimension that wants real terrain.
         *
         * @param json the whole {@code noise_router} object
         * @return this builder
         */
        public Noise router(String json) {
            this.router = json.strip();
            return this;
        }

        /**
         * A surface rule written out.
         *
         * @param json the whole {@code surface_rule} object
         * @return this builder
         */
        public Noise surfaceRule(String json) {
            this.surfaceRule = json.strip();
            return this;
        }

        /** Writes the settings. */
        public void save() {
            if (height % 16 != 0) {
                // The generator works in sections of sixteen. A height that is
                // not a multiple of one is rejected while a world is being
                // created, which is a long way from here.
                throw new IllegalStateException(
                        name + ": height must be a multiple of 16, got " + height);
            }

            StringBuilder functions = new StringBuilder();
            for (String field : FLAT) {
                functions.append("      \"").append(field).append("\": 0.0,\n");
            }
            // Positive is rock, negative is air, and the gradient between them
            // is the surface. One function is the whole of this terrain.
            functions.append("""
                          "final_density": {
                            "type": "minecraft:y_clamped_gradient",
                            "from_y": %d,
                            "to_y": %d,
                            "from_value": 1.0,
                            "to_value": -1.0
                          }""".formatted(solidTo - 8, solidTo + 8));

            String routerJson = router != null ? router
                    : "{\n" + functions + "\n    }";
            String surface = surfaceRule != null ? surfaceRule : """
                    {
                          "type": "minecraft:block",
                          "result_state": {
                            "Name": %s
                          }
                        }""".formatted(EmberOutput.quote(defaultBlock));

            provider.save("worldgen/noise_settings", name, """
                    {
                      "sea_level": %d,
                      "disable_mob_generation": %b,
                      "aquifers_enabled": %b,
                      "ore_veins_enabled": %b,
                      "legacy_random_source": false,
                      "default_block": {
                        "Name": %s
                      },
                      "default_fluid": {
                        "Name": %s
                      },
                      "noise": {
                        "min_y": %d,
                        "height": %d,
                        "size_horizontal": 1,
                        "size_vertical": 2
                      },
                      "noise_router": %s,
                      "surface_rule": %s,
                      "spawn_target": []
                    }
                    """.formatted(seaLevel, !mobs, aquifers, oreVeins,
                    EmberOutput.quote(defaultBlock), EmberOutput.quote(defaultFluid),
                    minY, height, routerJson, surface));
        }
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
