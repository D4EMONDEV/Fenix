package fr.d4emon.fenix.example.data;

import fr.d4emon.fenix.example.registry.ModBlocks;
import fr.d4emon.fenix.example.registry.ModContent;
import fr.d4emon.fenix.example.registry.ModItems;

import fr.d4emon.fenix.ember.EmberLootTableProvider;
import fr.d4emon.fenix.ember.Generator;

/**
 * What this mod's blocks drop.
 *
 * <p>Without these, both blocks would break into nothing at all — quietly.
 */
@Generator
public final class ModLootTables extends EmberLootTableProvider {

    /** Instantiated by Ember from the compile-time index. */
    public ModLootTables() {
    }

    @Override
    protected void lootTables() {
        // What the mod's own creatures leave behind. An entity type with no
        // table in loot_table/entities drops nothing, silently.
        entityLoot(ModContent.RUBY_SPRITE)
                .drop(ModItems.RUBY, 0, 2).looting(1)
                .save();

        entityLoot(ModContent.RUBY_WISP)
                .drop(ModItems.RUBY)
                .save();

        // Nothing refers to a chest table by itself; a structure or a block
        // entity has to name it. This one is here to be named later.
        chestLoot("ruby_cache")
                .rolls(2, 4)
                .item(ModItems.RUBY, 20, 1, 3)
                .item(ModBlocks.RUBY_BLOCK, 5)
                .item(ModItems.RUBY_HAMMER, 2)
                .save();

        dropsSelf(ModBlocks.RUBY_BLOCK);
        // A slab and a door each need a table of their own: a double slab
        // owes two, and a door must not pay out once per half.
        dropsSlab(ModBlocks.RUBY_SLAB);
        dropsSelf(ModBlocks.RUBY_FENCE);
        dropsSelf(ModBlocks.RUBY_WALL);
        dropsSelf(ModBlocks.RUBY_GATE);
        dropsSelf(ModBlocks.RUBY_STAIRS);
        dropsSelf(ModBlocks.RUBY_TRAPDOOR);
        dropsSelf(ModBlocks.RUBY_BUTTON);
        dropsSelf(ModBlocks.RUBY_PLATE);
        dropsDoor(ModBlocks.RUBY_DOOR);
        dropsSelf(ModBlocks.GLOWING_RUBY_BLOCK);
        dropsSelf(ModBlocks.RUBY_TALLY);
        dropsSelf(ModBlocks.RUBY_SAFE);
        dropsSelf(ModBlocks.RUBY_REFORGING);
        dropsSelf(ModBlocks.RUBY_LOG);
        dropsSelf(ModBlocks.STRIPPED_RUBY_LOG);

        // dropsOre, not drops: an ore owes a player three things — its material
        // normally, itself under Silk Touch, and more under Fortune. A plain
        // table gives none of them and says nothing about it.
        dropsOre(ModBlocks.RUBY_ORE, ModItems.RUBY);
        dropsOre(ModBlocks.DEEPSLATE_RUBY_ORE, ModItems.RUBY);
    }
}
