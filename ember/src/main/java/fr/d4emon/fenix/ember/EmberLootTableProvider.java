package fr.d4emon.fenix.ember;

import fr.d4emon.fenix.registry.Holder;
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
