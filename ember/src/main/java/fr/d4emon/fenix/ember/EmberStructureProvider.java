package fr.d4emon.fenix.ember;

import com.google.gson.JsonParser;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes structures: the structure itself, the pool of pieces it is built from,
 * and the set that decides where in the world it is tried.
 *
 * <p>Three files, and a fourth this cannot write. A jigsaw structure places
 * <em>templates</em> — {@code .nbt} files made in game with a structure block —
 * and nothing generates those from Java. The demo ships one built byte by byte,
 * which is possible but is not what Ember is for.
 *
 * <pre>{@code
 * @Generator
 * public final class ModStructures extends EmberStructureProvider {
 *     @Override
 *     protected void structures() {
 *         templatePool("shrine")
 *                 .piece("mymod:ruby_shrine", 1)
 *                 .save();
 *
 *         structure("ruby_shrine")
 *                 .startPool("mymod:shrine")
 *                 .biomes("#minecraft:is_overworld")
 *                 .save();
 *
 *         structureSet("ruby_shrines")
 *                 .structure("mymod:ruby_shrine")
 *                 .spacing(24, 8)
 *                 .save();
 *     }
 * }
 * }</pre>
 *
 * <p>All three are needed and they fail differently. A structure with no set is
 * one the world never tries to place — {@code /place} still works, so it looks
 * finished. A set naming a structure that is not there places nothing, and says
 * nothing. A pool with no pieces produces a structure that generates as empty
 * air.
 */
public abstract class EmberStructureProvider extends EmberProvider {

    /** Which generation step a structure belongs to. */
    public enum Step {
        /** Under the ground: mineshafts, strongholds. */
        UNDERGROUND_STRUCTURES("underground_structures"),
        /** On the ground: villages, temples, and most of what a mod adds. */
        SURFACE_STRUCTURES("surface_structures");

        private final String id;

        Step(String id) {
            this.id = id;
        }
    }

    /** For subclasses. */
    protected EmberStructureProvider() {
    }

    /** Describes the structures. */
    protected abstract void structures();

    @Override
    protected final void run() {
        structures();
    }

    /**
     * Starts a structure.
     *
     * @param name the path part of its id, and what {@code /place structure}
     *             takes
     * @return a builder; call {@code save()} when done
     */
    protected final Structure structure(String name) {
        return new Structure(this, name);
    }

    /**
     * Starts a pool of pieces a jigsaw structure can place.
     *
     * @param name the path part of its id
     * @return a builder; call {@code save()} when done
     */
    protected final Pool templatePool(String name) {
        return new Pool(this, name);
    }

    /**
     * Starts a set: where in the world the structure is tried.
     *
     * @param name the path part of its id
     * @return a builder; call {@code save()} when done
     */
    protected final Set structureSet(String name) {
        return new Set(this, name);
    }

    /**
     * Starts a processor list: what happens to each block as the template is
     * placed.
     *
     * <p>Without one a structure is stamped into the world exactly as it was
     * saved, every block present and every block new. That is right for a
     * building someone just built and wrong for anything the world is supposed
     * to have had for a while.
     *
     * @param name the list's name, referred to from a pool piece
     * @return a builder; call {@code save()} when done
     */
    protected final Processors processorList(String name) {
        return new Processors(this, name);
    }

    private void save(String directory, String name, String json) {
        output().data("worldgen/" + directory + "/" + name + ".json", json);
    }

    /** Collects one structure. */
    public static final class Structure {

        private final EmberStructureProvider provider;
        private final String name;

        private String startPool;
        private String biomes = "#minecraft:is_overworld";
        private Step step = Step.SURFACE_STRUCTURES;
        private int size = 1;
        private int maxDistance = 80;
        private String adaptation = "beard_thin";

        private Structure(EmberStructureProvider provider, String name) {
            this.provider = provider;
            this.name = name;
        }

        /**
         * The pool the first piece comes from.
         *
         * @param id a template pool's id
         * @return this builder
         */
        public Structure startPool(String id) {
            this.startPool = id;
            return this;
        }

        /**
         * Where it may generate.
         *
         * @param tagOrId a biome tag such as {@code #minecraft:is_overworld},
         *                or a single biome's id
         * @return this builder
         */
        public Structure biomes(String tagOrId) {
            this.biomes = tagOrId;
            return this;
        }

        /**
         * @param step which generation step it belongs to
         * @return this builder
         */
        public Structure step(Step step) {
            this.step = step;
            return this;
        }

        /**
         * How many times the jigsaw may expand from the start piece.
         *
         * <p>One means the start piece and nothing else, which is right for a
         * single building. A village is six.
         *
         * @param depth the expansion depth, at most 7
         * @return this builder
         */
        public Structure size(int depth) {
            this.size = depth;
            return this;
        }

        /**
         * How the ground is reshaped around it.
         *
         * @param how one of {@code none}, {@code beard_thin},
         *            {@code beard_box}, {@code bury}, {@code encapsulate}
         * @return this builder
         */
        public Structure terrainAdaptation(String how) {
            this.adaptation = how;
            return this;
        }

        /** Writes the structure. */
        public void save() {
            if (startPool == null) {
                throw new IllegalStateException(
                        name + " has no start pool, so there is no first piece to place");
            }
            if (size < 1 || size > 7) {
                throw new IllegalStateException(
                        name + ": size is 1 to 7, and " + size + " is not");
            }

            provider.save("structure", name, """
                    {
                      "type": "minecraft:jigsaw",
                      "biomes": %s,
                      "max_distance_from_center": %d,
                      "project_start_to_heightmap": "WORLD_SURFACE_WG",
                      "size": %d,
                      "spawn_overrides": {},
                      "start_height": {
                        "absolute": 0
                      },
                      "start_pool": %s,
                      "step": "%s",
                      "terrain_adaptation": "%s",
                      "use_expansion_hack": false
                    }
                    """.formatted(EmberOutput.quote(biomes), maxDistance, size,
                    EmberOutput.quote(startPool), step.id, adaptation));
        }
    }

    /** Collects one template pool. */
    public static final class Pool {

        private final EmberStructureProvider provider;
        private final String name;
        private final List<String> elements = new ArrayList<>();
        private String fallback = "minecraft:empty";

        private Pool(EmberStructureProvider provider, String name) {
            this.provider = provider;
            this.name = name;
        }

        /**
         * Adds a template file as a possible piece.
         *
         * @param location the template's id, which is a {@code .nbt} under
         *                 {@code data/<namespace>/structure/}
         * @param weight   how likely against the other pieces
         * @return this builder
         */
        public Pool piece(String location, int weight) {
            return piece(location, weight, "minecraft:empty");
        }

        /**
         * Adds a template file as a possible piece, processed on the way in.
         *
         * @param location   the template's id, which is a {@code .nbt} under
         *                   {@code data/<namespace>/structure/}
         * @param weight     how likely against the other pieces
         * @param processors a processor list's id, from
         *                   {@link EmberStructureProvider#processorList}
         * @return this builder
         */
        public Pool piece(String location, int weight, String processors) {
            elements.add("""
                    {
                        "element": {
                          "element_type": "minecraft:single_pool_element",
                          "location": %s,
                          "processors": %s,
                          "projection": "rigid"
                        },
                        "weight": %d
                      }""".formatted(EmberOutput.quote(location),
                    EmberOutput.quote(processors), weight));
            return this;
        }

        /**
         * @param id the pool used when this one cannot expand further
         * @return this builder
         */
        public Pool fallback(String id) {
            this.fallback = id;
            return this;
        }

        /** Writes the pool. */
        public void save() {
            if (elements.isEmpty()) {
                // The structure would generate, successfully, as nothing at all.
                throw new IllegalStateException(
                        name + " has no pieces, so anything using it generates empty air");
            }

            provider.save("template_pool", name, """
                    {
                      "fallback": %s,
                      "elements": [
                        %s
                      ]
                    }
                    """.formatted(EmberOutput.quote(fallback),
                    String.join(",\n    ", elements)));
        }
    }

    /**
     * Collects one processor list.
     *
     * <p>Order matters: each processor sees what the one before it left, so
     * rotting away half the blocks and then replacing stone with cracked
     * stone touches half as many as the other way round.
     */
    public static final class Processors {

        private final EmberStructureProvider provider;
        private final String name;
        private final List<String> processors = new ArrayList<>();
        private final List<String> rules = new ArrayList<>();

        private Processors(EmberStructureProvider provider, String name) {
            this.provider = provider;
            this.name = name;
        }

        /**
         * Leaves out a fraction of the blocks, at random.
         *
         * <p>This is what makes a structure look old rather than new. Vanilla's
         * ruined portals use it.
         *
         * @param integrity how much survives: 1 is all of it, 0 is none
         * @return this builder
         */
        public Processors rot(float integrity) {
            if (integrity <= 0f || integrity > 1f) {
                // 0 is a structure that generates as nothing at all, and finds
                // its way into a world as an absence rather than an error.
                throw new IllegalArgumentException(
                        name + ": integrity is a fraction that survives, so it must be "
                                + "above 0 and at most 1; got " + integrity);
            }
            processors.add("""
                    {
                          "processor_type": "minecraft:block_rot",
                          "integrity": %s
                        }""".formatted(EmberOutput.decimal(integrity)));
            return this;
        }

        /**
         * Grows moss on it, the way vanilla ages a stone structure.
         *
         * @param mossiness how much, 0 to 1
         * @return this builder
         */
        public Processors mossy(float mossiness) {
            processors.add("""
                    {
                          "processor_type": "minecraft:block_age",
                          "mossiness": %s
                        }""".formatted(EmberOutput.decimal(mossiness)));
            return this;
        }

        /**
         * Swaps one block for another, some of the time.
         *
         * <p>Rules added in a row go into one processor, because a block that
         * a rule has already changed is not offered to the rules after it —
         * which is what stops stone becoming cracked stone and then cracked
         * stone becoming something else in the same pass.
         *
         * @param block       what to look for
         * @param with        what to put there instead
         * @param probability how often, 0 to 1
         * @return this builder
         */
        public Processors replace(String block, String with, float probability) {
            rules.add("""
                    {
                            "input_predicate": {
                              "predicate_type": "minecraft:random_block_match",
                              "block": %s,
                              "probability": %s
                            },
                            "location_predicate": {
                              "predicate_type": "minecraft:always_true"
                            },
                            "output_state": {
                              "Name": %s
                            }
                          }""".formatted(EmberOutput.quote(block),
                    EmberOutput.decimal(probability), EmberOutput.quote(with)));
            return this;
        }

        /**
         * Swaps one block for another, every time.
         *
         * @param block what to look for
         * @param with  what to put there instead
         * @return this builder
         */
        public Processors replace(String block, String with) {
            rules.add("""
                    {
                            "input_predicate": {
                              "predicate_type": "minecraft:block_match",
                              "block": %s
                            },
                            "location_predicate": {
                              "predicate_type": "minecraft:always_true"
                            },
                            "output_state": {
                              "Name": %s
                            }
                          }""".formatted(EmberOutput.quote(block), EmberOutput.quote(with)));
            return this;
        }

        /**
         * Drops the template's structure blocks and jigsaw blocks so they do
         * not end up in the world.
         *
         * <p>A template saved with a structure block still contains it. Left
         * in, a player finds the mod's scaffolding standing in their world.
         *
         * @return this builder
         */
        public Processors dropScaffolding() {
            processors.add("""
                    {
                          "processor_type": "minecraft:block_ignore",
                          "blocks": [
                            {"Name": "minecraft:structure_void"}
                          ]
                        }""");
            return this;
        }

        /**
         * A processor this builder has no method for, written out.
         *
         * @param json the processor object, including its {@code processor_type}
         * @return this builder
         */
        public Processors processor(String json) {
            processors.add(json.strip());
            return this;
        }

        /** Writes the list. */
        public void save() {
            if (!rules.isEmpty()) {
                processors.add("""
                        {
                          "processor_type": "minecraft:rule",
                          "rules": [
                            %s
                          ]
                        }""".formatted(String.join(",\n        ", rules)));
            }
            if (processors.isEmpty()) {
                // Legal, and never what was meant: a list that does nothing is
                // the same as not naming a list, written in three files.
                throw new IllegalStateException(
                        name + " has no processors, so it does nothing to what it processes");
            }

            // Laid out by Gson rather than by hand. A rule sits three levels
            // deep inside a processor inside a list, and a Java text block
            // measures its indentation against its own shortest line — so
            // nesting them by eye produces valid JSON that is unreadable.
            // Reading it back to print it also means anything written here
            // that is not JSON fails in Ember rather than in a world.
            provider.save("processor_list", name,
                    PRINTER.toJson(JsonParser.parseString("""
                            {
                              "processors": [
                                %s
                              ]
                            }
                            """.formatted(String.join(",\n", processors)))) + "\n");
        }
    }

    /**
     * Two spaces and no HTML escaping, which is how the rest of Ember's files
     * are laid out.
     */
    private static final Gson PRINTER =
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** Collects one structure set. */
    public static final class Set {

        private final EmberStructureProvider provider;
        private final String name;
        private final List<String> structures = new ArrayList<>();
        private int spacing = 32;
        private int separation = 8;
        private int salt = 0;

        private Set(EmberStructureProvider provider, String name) {
            this.provider = provider;
            this.name = name;
        }

        /**
         * Adds a structure this set places.
         *
         * @param id     the structure's id
         * @param weight how likely against the others in the set
         * @return this builder
         */
        public Set structure(String id, int weight) {
            structures.add("""
                    {
                        "structure": %s,
                        "weight": %d
                      }""".formatted(EmberOutput.quote(id), weight));
            return this;
        }

        /**
         * Adds a structure this set places, as its only one.
         *
         * @param id the structure's id
         * @return this builder
         */
        public Set structure(String id) {
            return structure(id, 1);
        }

        /**
         * How far apart, in chunks.
         *
         * @param spacing    the average distance between attempts
         * @param separation the least it can be; must be under the spacing
         * @return this builder
         */
        public Set spacing(int spacing, int separation) {
            this.spacing = spacing;
            this.separation = separation;
            return this;
        }

        /**
         * The number that decides where the grid falls.
         *
         * <p>Two sets sharing a salt and a spacing generate in the same chunks
         * forever, so a mod wants one nobody else picked.
         *
         * @param value the salt
         * @return this builder
         */
        public Set salt(int value) {
            this.salt = value;
            return this;
        }

        /** Writes the set. */
        public void save() {
            if (structures.isEmpty()) {
                throw new IllegalStateException(name + " places no structures");
            }
            if (separation >= spacing) {
                // The game refuses this, but late and with a message about
                // numbers rather than about which set is wrong.
                throw new IllegalStateException(
                        name + ": separation must be under spacing, and " + separation
                                + " is not under " + spacing);
            }

            provider.save("structure_set", name, """
                    {
                      "placement": {
                        "type": "minecraft:random_spread",
                        "salt": %d,
                        "separation": %d,
                        "spacing": %d
                      },
                      "structures": [
                        %s
                      ]
                    }
                    """.formatted(salt, separation, spacing,
                    String.join(",\n    ", structures)));
        }
    }
}
