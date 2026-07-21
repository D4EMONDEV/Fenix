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
     * The block drops something else, the way ore drops its material.
     *
     * @param block the block
     * @param drop  what it drops
     */
    protected final void drops(Holder<Block> block, Holder<Item> drop) {
        write(block, EmberOutput.idOf(drop.get()).toString());
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
