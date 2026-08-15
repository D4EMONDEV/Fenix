package fr.d4emon.fenix.ember;

import fr.d4emon.fenix.registry.Holder;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Writes block loot tables — what a block drops when broken.
 *
 * <pre>{@code
 * @Generator
 * public final class ModLootTables extends EmberLootTableProvider {
 *     @Override
 *     protected void lootTables() {
 *         dropsSelf(ModBlocks.RUBY_BLOCK);
 *         drops(ModBlocks.RUBY_ORE, ModItems.RUBY);
 *     }
 * }
 * }</pre>
 *
 * <p>A block with no loot table drops nothing at all, silently — which is the
 * single most common surprise when adding a block by hand.
 */
public abstract class EmberLootTableProvider extends EmberProvider {

    /** For subclasses. */
    protected EmberLootTableProvider() {
    }

    /** Describes the loot tables. */
    protected abstract void lootTables();

    @Override
    protected final void run() {
        lootTables();
    }

    /**
     * The block drops itself — the usual case for a decorative block.
     *
     * @param block the block
     */
    protected final void dropsSelf(Holder<Block> block) {
        write(block, modId() + ":" + block.id().getPath());
    }

    /**
     * The block drops something else, exactly one, whatever the tool.
     *
     * <p>For an ore, reach for {@link #dropsOre} instead: this ignores Fortune
     * and turns a Silk Touch pick into an ordinary one, which is not what a
     * player expects from something that looks like ore.
     *
     * @param block the block
     * @param drop  what it drops
     */
    protected final void drops(Holder<Block> block, Holder<Item> drop) {
        write(block, EmberOutput.idOf(drop.get()).toString());
    }

    /**
     * An ore: its material normally, itself under Silk Touch, more under
     * Fortune.
     *
     * <p>All three are what a player assumes the moment a block looks like ore,
     * and a plain table gives none of them — Silk Touch yields the material like
     * any other pick, Fortune does nothing at all, and nothing anywhere says the
     * table was the reason. It is a block that works and feels broken.
     *
     * @param block the ore block
     * @param drop  the material it yields
     */
    protected final void dropsOre(Holder<Block> block, Holder<Item> drop) {
        String name = block.id().getPath();
        output().data("loot_table/blocks/" + name + ".json", """
                {
                  "type": "minecraft:block",
                  "pools": [
                    {
                      "rolls": 1.0,
                      "entries": [
                        {
                          "type": "minecraft:alternatives",
                          "children": [
                            {
                              "type": "minecraft:item",
                              "name": "%s:%s",
                              "conditions": [
                %s
                              ]
                            },
                            {
                              "type": "minecraft:item",
                              "name": "%s",
                              "functions": [
                                {
                                  "function": "minecraft:apply_bonus",
                                  "enchantment": "minecraft:fortune",
                                  "formula": "minecraft:ore_drops"
                                },
                                {
                                  "function": "minecraft:explosion_decay"
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ],
                  "random_sequence": "%s:blocks/%s"
                }
                """.formatted(modId(), name, silkTouch("                "),
                EmberOutput.idOf(drop.get()), modId(), name));
    }

    /**
     * The block drops itself only under Silk Touch, and otherwise nothing —
     * what glass does.
     *
     * @param block the block
     */
    protected final void dropsWithSilkTouch(Holder<Block> block) {
        String name = block.id().getPath();
        output().data("loot_table/blocks/" + name + ".json", """
                {
                  "type": "minecraft:block",
                  "pools": [
                    {
                      "rolls": 1.0,
                      "conditions": [
                %s
                      ],
                      "entries": [
                        {
                          "type": "minecraft:item",
                          "name": "%s:%s"
                        }
                      ]
                    }
                  ],
                  "random_sequence": "%s:blocks/%s"
                }
                """.formatted(silkTouch("        "), modId(), name, modId(), name));
    }

    /**
     * {@return the condition matching a tool enchanted with Silk Touch}
     *
     * <p>Indented by the caller because the shape is nested at two different
     * depths, and a loot table that reads badly is one nobody checks.
     */
    private static String silkTouch(String indent) {
        return """
                {
                  "condition": "minecraft:match_tool",
                  "predicate": {
                    "predicates": {
                      "minecraft:enchantments": [
                        {
                          "enchantments": "minecraft:silk_touch",
                          "levels": {
                            "min": 1
                          }
                        }
                      ]
                    }
                  }
                }
                """.stripTrailing().indent(indent.length()).stripTrailing();
    }

    /**
     * A slab, which drops two when it was a double slab and one otherwise.
     *
     * <p>{@link #dropsSelf} is wrong for a slab and wrong quietly: the block
     * breaks, one slab drops, and the other half of what the player placed is
     * gone. Nothing says so, and it looks like an ordinary mistake in counting.
     *
     * @param block the slab
     */
    protected final void dropsSlab(Holder<Block> block) {
        String name = block.id().getPath();
        String id = EmberOutput.idOf(block.get()).toString();
        // explosion_decay rather than survives_explosion: the count is what is
        // reduced, so a slab blown up yields one instead of vanishing whole.
        output().data("loot_table/blocks/" + name + ".json", """
                {
                  "type": "minecraft:block",
                  "pools": [
                    {
                      "rolls": 1.0,
                      "entries": [
                        {
                          "type": "minecraft:item",
                          "name": "%s",
                          "functions": [
                            {
                              "function": "minecraft:set_count",
                              "count": 2.0,
                              "conditions": [
                                {
                                  "condition": "minecraft:block_state_property",
                                  "block": "%s",
                                  "properties": {
                                    "type": "double"
                                  }
                                }
                              ]
                            },
                            {
                              "function": "minecraft:explosion_decay"
                            }
                          ]
                        }
                      ]
                    }
                  ],
                  "random_sequence": "%s:blocks/%s"
                }
                """.formatted(id, id, modId(), name));
    }

    /**
     * A door, which drops once for the two blocks it occupies.
     *
     * <p>A door is two block states, and breaking either breaks both. With
     * {@link #dropsSelf} each half rolls the table and the player gets two
     * doors back from one — the kind of duplication that is only noticed after
     * somebody has been doing it on purpose for a week.
     *
     * @param block the door
     */
    protected final void dropsDoor(Holder<Block> block) {
        String name = block.id().getPath();
        String id = EmberOutput.idOf(block.get()).toString();
        output().data("loot_table/blocks/" + name + ".json", """
                {
                  "type": "minecraft:block",
                  "pools": [
                    {
                      "rolls": 1.0,
                      "conditions": [
                        {
                          "condition": "minecraft:survives_explosion"
                        }
                      ],
                      "entries": [
                        {
                          "type": "minecraft:item",
                          "name": "%s",
                          "conditions": [
                            {
                              "condition": "minecraft:block_state_property",
                              "block": "%s",
                              "properties": {
                                "half": "lower"
                              }
                            }
                          ]
                        }
                      ]
                    }
                  ],
                  "random_sequence": "%s:blocks/%s"
                }
                """.formatted(id, id, modId(), name));
    }

    /**
     * Starts a table for what a mob drops when it dies.
     *
     * <p>Written to {@code loot_table/entities/}, which is where the game looks
     * for it — an entity type with no table there drops nothing, and says
     * nothing about it.
     *
     * <pre>{@code
     * entityLoot(ModContent.RUBY_SPRITE)
     *         .drop(ModItems.RUBY, 0, 2).looting(0, 1)
     *         .save();
     * }</pre>
     *
     * @param entity the entity type
     * @return a builder; call {@code save()} when done
     */
    protected final EntityLoot entityLoot(Holder<?> entity) {
        return new EntityLoot(this, EmberOutput.idOf(entity.get()).getPath());
    }

    /**
     * Starts a table for what is found in a container.
     *
     * <p>Written to {@code loot_table/chests/}. Unlike a block or an entity
     * table, nothing refers to this by itself: a structure or a block entity
     * has to name it.
     *
     * @param name the path part of its id, under {@code chests/}
     * @return a builder; call {@code save()} when done
     */
    protected final ChestLoot chestLoot(String name) {
        return new ChestLoot(this, name);
    }

    /**
     * Writes one pool holding every entry, which is what both builders want.
     *
     * <p>The callers pass the value of {@code rolls} and nothing else — the
     * key, the comma and the indentation are decided here. They were the
     * callers' business for one commit and the two of them disagreed about
     * how far to indent, which is the kind of thing a committed generated file
     * shows to every reviewer forever.
     *
     * @param rolls the JSON value for {@code rolls}, already indented if it
     *              spans lines
     */
    private void writeTable(String directory, String name, String type,
                            String rolls, List<String> entries) {
        StringBuilder json = new StringBuilder()
                .append("{\n")
                .append("  \"type\": \"").append(type).append("\",\n")
                .append("  \"pools\": [\n")
                .append("    {\n")
                .append("      \"rolls\": ").append(rolls).append(",\n")
                .append("      \"entries\": [\n")
                .append(String.join(",\n", entries)).append("\n")
                .append("      ]\n")
                .append("    }\n")
                .append("  ],\n")
                .append("  \"random_sequence\": \"").append(modId()).append(':')
                .append(directory).append('/').append(name).append("\"\n")
                .append("}\n");
        output().data("loot_table/" + directory + "/" + name + ".json", json.toString());
    }

    /** Collects what a mob drops. */
    public static final class EntityLoot {

        private final EmberLootTableProvider provider;
        private final String name;
        private final List<String> entries = new ArrayList<>();

        private EntityLoot(EmberLootTableProvider provider, String name) {
            this.provider = provider;
            this.name = name;
        }

        /**
         * Drops exactly one of something, every time.
         *
         * @param item what drops
         * @return this builder
         */
        public EntityLoot drop(Holder<?> item) {
            entries.add(item(EmberOutput.idOf(item.get()).toString(), null, null, 0));
            return this;
        }

        /**
         * Drops between {@code min} and {@code max}, evenly.
         *
         * @param item what drops
         * @param min  the fewest, which may be zero
         * @param max  the most
         * @return this builder
         */
        public EntityLoot drop(Holder<?> item, int min, int max) {
            entries.add(item(EmberOutput.idOf(item.get()).toString(), min, max, 0));
            return this;
        }

        /**
         * Lets Looting add to the drop declared just before this.
         *
         * <p>The idiom every vanilla mob table uses. Without it a mod's mob
         * ignores the enchantment, which players read as the mob being bugged
         * rather than as a table that never mentioned it.
         *
         * @param max the most Looting can add, per level
         * @return this builder
         * @throws IllegalStateException if nothing has been dropped yet
         */
        public EntityLoot looting(int max) {
            if (entries.isEmpty()) {
                throw new IllegalStateException(
                        "looting() applies to the drop before it, and there is none yet");
            }
            int last = entries.size() - 1;
            String entry = entries.get(last);
            String opener = "          \"functions\": [\n";
            if (!entry.contains(opener)) {
                // Looting scales a count, so there has to be one to scale.
                throw new IllegalStateException(
                        "looting() needs a drop with a count, such as drop(item, 0, 2)");
            }
            entries.set(last, entry.replace(opener, opener
                    + function("minecraft:enchanted_count_increase", 0, max,
                            "              \"enchantment\": \"minecraft:looting\",\n")
                    + ",\n"));
            return this;
        }

        /**
         * One item entry, indented to sit inside a pool.
         *
         * @param id     what drops
         * @param min    the fewest, or {@code null} for exactly one
         * @param max    the most, ignored when {@code min} is null
         * @param weight nonzero to give the entry a weight, as a chest does
         * @return the entry, as JSON
         */
        static String item(String id, Integer min, Integer max, int weight) {
            StringBuilder entry = new StringBuilder()
                    .append("        {\n")
                    .append("          \"type\": \"minecraft:item\",\n");
            if (min != null) {
                entry.append("          \"functions\": [\n")
                        .append(function("minecraft:set_count", min, max, "")).append("\n")
                        .append("          ],\n");
            }
            entry.append("          \"name\": ").append(EmberOutput.quote(id));
            if (weight > 0) {
                entry.append(",\n          \"weight\": ").append(weight);
            }
            return entry.append("\n        }").toString();
        }

        /**
         * One function with a uniform count, indented to sit in a list.
         *
         * @param name  the function's id
         * @param min   the fewest
         * @param max   the most
         * @param extra any further lines the function needs, already indented
         * @return the function, as JSON
         */
        static String function(String name, int min, int max, String extra) {
            return "            {\n"
                    + "              \"function\": \"" + name + "\",\n"
                    + extra
                    + "              \"count\": {\n"
                    + "                \"type\": \"minecraft:uniform\",\n"
                    + "                \"min\": " + EmberOutput.decimal(min) + ",\n"
                    + "                \"max\": " + EmberOutput.decimal(max) + "\n"
                    + "              }\n"
                    + "            }";
        }

        /** Writes the table. */
        public void save() {
            if (entries.isEmpty()) {
                throw new IllegalStateException(name + " drops nothing, so it needs no table");
            }
            provider.writeTable("entities", name, "minecraft:entity", "1.0", entries);
        }
    }

    /** Collects what a container holds. */
    public static final class ChestLoot {

        private final EmberLootTableProvider provider;
        private final String name;
        private final List<String> entries = new ArrayList<>();
        private int minRolls = 1;
        private int maxRolls = 1;

        private ChestLoot(EmberLootTableProvider provider, String name) {
            this.provider = provider;
            this.name = name;
        }

        /**
         * How many times the pool is drawn from.
         *
         * @param min the fewest draws
         * @param max the most
         * @return this builder
         */
        public ChestLoot rolls(int min, int max) {
            this.minRolls = min;
            this.maxRolls = max;
            return this;
        }

        /**
         * One possible find.
         *
         * @param item   what it is
         * @param weight how likely against everything else in the pool
         * @return this builder
         */
        public ChestLoot item(Holder<?> item, int weight) {
            return item(item, weight, 1, 1);
        }

        /**
         * One possible find, in a quantity.
         *
         * @param item   what it is
         * @param weight how likely against everything else in the pool
         * @param min    the fewest
         * @param max    the most
         * @return this builder
         */
        public ChestLoot item(Holder<?> item, int weight, int min, int max) {
            String base = EntityLoot.item(EmberOutput.idOf(item.get()).toString(),
                    min == 1 && max == 1 ? null : min,
                    min == 1 && max == 1 ? null : max,
                    weight);
            entries.add(base);
            return this;
        }

        /** Writes the table. */
        public void save() {
            if (entries.isEmpty()) {
                throw new IllegalStateException(name + " holds nothing, so it needs no table");
            }
            String rolls = minRolls == maxRolls
                    ? EmberOutput.decimal(minRolls)
                    : "{\n"
                            + "        \"type\": \"minecraft:uniform\",\n"
                            + "        \"min\": " + EmberOutput.decimal(minRolls) + ",\n"
                            + "        \"max\": " + EmberOutput.decimal(maxRolls) + "\n"
                            + "      }";
            provider.writeTable("chests", name, "minecraft:chest", rolls, entries);
        }
    }

    private void write(Holder<Block> block, String dropId) {
        String name = block.id().getPath();
        output().data("loot_table/blocks/" + name + ".json", """
                {
                  "type": "minecraft:block",
                  "pools": [
                    {
                      "rolls": 1.0,
                      "conditions": [
                        {
                          "condition": "minecraft:survives_explosion"
                        }
                      ],
                      "entries": [
                        {
                          "type": "minecraft:item",
                          "name": "%s"
                        }
                      ]
                    }
                  ],
                  "random_sequence": "%s:blocks/%s"
                }
                """.formatted(dropId, modId(), name));
    }
}
