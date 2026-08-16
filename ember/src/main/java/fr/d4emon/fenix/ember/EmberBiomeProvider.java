package fr.d4emon.fenix.ember;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes biomes.
 *
 * <p>A biome file is long and mostly lists: what generates in it, in which of
 * eleven ordered steps, and what spawns in it, in which category. Both are
 * positional in the JSON — the features are an array of arrays whose index is
 * the generation step — and both are silent when wrong. A feature put in the
 * wrong step still generates, at the wrong moment, on top of or underneath the
 * thing it was meant to accompany.
 *
 * <pre>{@code
 * @Generator
 * public final class ModBiomes extends EmberBiomeProvider {
 *     @Override
 *     protected void biomes() {
 *         biome("ruby_caverns")
 *                 .temperature(0.7f)
 *                 .downfall(0.3f)
 *                 .skyColor("#5d3a4a")
 *                 .waterColor("#8a2846")
 *                 .carver("minecraft:cave")
 *                 .feature(Step.UNDERGROUND_ORES, "mymod:ruby_ore")
 *                 .spawn("creature", "mymod:ruby_sprite", 6, 1, 3)
 *                 .save();
 *     }
 * }
 * }</pre>
 *
 * <p>Writing the file is not the same as the biome appearing in a world. A
 * biome nothing places generates nowhere, and Minecraft has no datapack way to
 * add one to the overworld's noise settings — that needs a dimension of its
 * own, or code. Fenix does not pretend otherwise.
 */
public abstract class EmberBiomeProvider extends EmberProvider {

    /**
     * The eleven generation steps, in the order the file wants them.
     *
     * <p>Named rather than numbered on purpose: the JSON is an array whose
     * index is this ordinal, and an off-by-one there is a feature that
     * generates in the wrong step with nothing said about it.
     */
    public enum Step {
        RAW_GENERATION,
        LAKES,
        LOCAL_MODIFICATIONS,
        UNDERGROUND_STRUCTURES,
        SURFACE_STRUCTURES,
        STRONGHOLDS,
        UNDERGROUND_ORES,
        UNDERGROUND_DECORATION,
        FLUID_SPRINGS,
        VEGETAL_DECORATION,
        TOP_LAYER_MODIFICATION
    }

    /** For subclasses. */
    protected EmberBiomeProvider() {
    }

    /** Describes the biomes. */
    protected abstract void biomes();

    @Override
    protected final void run() {
        biomes();
    }

    /**
     * Starts a biome.
     *
     * @param name the path part of its id
     * @return a builder; call {@code save()} when done
     */
    protected final Builder biome(String name) {
        return new Builder(this, name);
    }

    private void save(String name, String json) {
        output().data("worldgen/biome/" + name + ".json", json);
    }

    /** Collects one biome. */
    public static final class Builder {

        private final EmberBiomeProvider provider;
        private final String name;
        private final Map<Step, List<String>> features = new EnumMap<>(Step.class);
        private final Map<String, List<String>> spawners = new LinkedHashMap<>();
        private final List<String> carvers = new ArrayList<>();
        private final Map<String, String> effects = new LinkedHashMap<>();

        private float temperature = 0.8f;
        private float downfall = 0.4f;
        private boolean precipitation = true;
        private String skyColor = "#78a7ff";

        private Builder(EmberBiomeProvider provider, String name) {
            this.provider = provider;
            this.name = name;
        }

        /**
         * @param value how warm; below 0.15 it snows rather than rains
         * @return this builder
         */
        public Builder temperature(float value) {
            this.temperature = value;
            return this;
        }

        /**
         * @param value how wet, from 0 to 1
         * @return this builder
         */
        public Builder downfall(float value) {
            this.downfall = value;
            return this;
        }

        /**
         * @param has whether it rains or snows here at all
         * @return this builder
         */
        public Builder precipitation(boolean has) {
            this.precipitation = has;
            return this;
        }

        /**
         * The sky's colour.
         *
         * <p>In 26.2 this lives under {@code attributes} rather than
         * {@code effects}, unlike every other colour.
         *
         * @param hex a colour, as {@code #rrggbb}
         * @return this builder
         */
        public Builder skyColor(String hex) {
            this.skyColor = hex;
            return this;
        }

        /**
         * @param hex the water's colour, as {@code #rrggbb}
         * @return this builder
         */
        public Builder waterColor(String hex) {
            effects.put("water_color", hex);
            return this;
        }

        /**
         * @param hex the colour of fog underwater
         * @return this builder
         */
        public Builder waterFogColor(String hex) {
            effects.put("water_fog_color", hex);
            return this;
        }

        /**
         * @param hex the colour of fog in the air
         * @return this builder
         */
        public Builder fogColor(String hex) {
            effects.put("fog_color", hex);
            return this;
        }

        /**
         * @param hex the grass tint
         * @return this builder
         */
        public Builder grassColor(String hex) {
            effects.put("grass_color", hex);
            return this;
        }

        /**
         * @param hex the leaf tint
         * @return this builder
         */
        public Builder foliageColor(String hex) {
            effects.put("foliage_color", hex);
            return this;
        }

        /**
         * Adds a carver — what cuts caves and ravines through it.
         *
         * @param id a configured carver's id
         * @return this builder
         */
        public Builder carver(String id) {
            carvers.add(id);
            return this;
        }

        /**
         * Adds a placed feature, in a named step rather than at an index.
         *
         * @param step where in generation it happens
         * @param id   a placed feature's id
         * @return this builder
         */
        public Builder feature(Step step, String id) {
            features.computeIfAbsent(step, key -> new ArrayList<>()).add(id);
            return this;
        }

        /**
         * Adds something that spawns here.
         *
         * @param category the spawn category, such as {@code creature} or
         *                 {@code monster}; it decides which cap this counts
         *                 against
         * @param entity   the entity type's id
         * @param weight   how likely against everything else in that category
         * @param min      the fewest that appear at once
         * @param max      the most
         * @return this builder
         */
        public Builder spawn(String category, String entity, int weight, int min, int max) {
            spawners.computeIfAbsent(category, key -> new ArrayList<>()).add("""
                    {
                            "type": %s,
                            "weight": %d,
                            "minCount": %d,
                            "maxCount": %d
                          }""".formatted(EmberOutput.quote(entity), weight, min, max));
            return this;
        }

        /** Writes the biome. */
        public void save() {
            StringBuilder json = new StringBuilder("{\n");

            json.append("  \"attributes\": {\n    \"minecraft:visual/sky_color\": ")
                    .append(EmberOutput.quote(skyColor)).append("\n  },\n");

            json.append("  \"carvers\": [");
            append(json, carvers, "    ", true);
            json.append("],\n");

            json.append("  \"downfall\": ").append(EmberOutput.decimal(downfall)).append(",\n");

            json.append("  \"effects\": {");
            String between = "\n    ";
            for (Map.Entry<String, String> effect : effects.entrySet()) {
                json.append(between).append(EmberOutput.quote(effect.getKey()))
                        .append(": ").append(EmberOutput.quote(effect.getValue()));
                between = ",\n    ";
            }
            json.append(effects.isEmpty() ? "" : "\n  ").append("},\n");

            // Every step, in order, empty or not: the array is positional and a
            // short one silently shifts everything after it.
            json.append("  \"features\": [");
            String outer = "";
            for (Step step : Step.values()) {
                json.append(outer).append("\n    [");
                append(json, features.getOrDefault(step, List.of()), "      ", true);
                json.append("]");
                outer = ",";
            }
            json.append("\n  ],\n");

            json.append("  \"has_precipitation\": ").append(precipitation).append(",\n")
                    .append("  \"spawn_costs\": {},\n")
                    .append("  \"spawners\": {");
            between = "\n    ";
            for (Map.Entry<String, List<String>> entry : spawners.entrySet()) {
                json.append(between).append(EmberOutput.quote(entry.getKey())).append(": [\n      ")
                        .append(String.join(",\n      ", entry.getValue()))
                        .append("\n    ]");
                between = ",\n    ";
            }
            json.append(spawners.isEmpty() ? "" : "\n  ").append("},\n")
                    .append("  \"temperature\": ").append(EmberOutput.decimal(temperature))
                    .append("\n}\n");

            provider.save(name, json.toString());
        }

        /** Appends a list of ids, indented, or leaves the brackets empty. */
        private static void append(StringBuilder json, List<String> values,
                                   String indent, boolean quoted) {
            String between = "\n" + indent;
            for (String value : values) {
                json.append(between).append(quoted ? EmberOutput.quote(value) : value);
                between = ",\n" + indent;
            }
            if (!values.isEmpty()) {
                json.append("\n").append(indent, 0, indent.length() - 2);
            }
        }
    }
}
