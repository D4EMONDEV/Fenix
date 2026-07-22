package fr.d4emon.fenix.example.content;

import fr.d4emon.fenix.ember.EmberModelProvider;
import fr.d4emon.fenix.ember.Generator;

/** How this mod's content looks. */
@Generator
public final class ModModels extends EmberModelProvider {

    /** Instantiated by Ember from the compile-time index. */
    public ModModels() {
    }

    @Override
    protected void models() {
        cubeAll(ModBlocks.RUBY_BLOCK);
        cubeAll(ModBlocks.GLOWING_RUBY_BLOCK);
        cubeAll(ModBlocks.RUBY_TALLY);
        cubeAll(ModBlocks.RUBY_SAFE);

        // A spawn egg is an ordinary flat item in 26.2 — one texture, no tint
        // template — so there is nothing special to say about it here.
        flatItem(ModContent.RUBY_WISP_SPAWN_EGG);

        flatItem(ModItems.RUBY);
        flatItem(ModItems.RUBY_HAMMER);
    }
}
