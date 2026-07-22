package fr.d4emon.fenix.example.content;

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
        dropsSelf(ModBlocks.RUBY_BLOCK);
        dropsSelf(ModBlocks.GLOWING_RUBY_BLOCK);
        dropsSelf(ModBlocks.RUBY_TALLY);
        dropsSelf(ModBlocks.RUBY_SAFE);

        // Ore drops its material, not itself — the reason to reach for
        // drops(...) rather than dropsSelf(...).
        drops(ModBlocks.RUBY_ORE, ModItems.RUBY);
        drops(ModBlocks.DEEPSLATE_RUBY_ORE, ModItems.RUBY);
    }
}
