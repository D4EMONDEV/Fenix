package fr.d4emon.fenix.example.content;

import fr.d4emon.fenix.ember.EmberRecipeProvider;
import fr.d4emon.fenix.ember.Generator;

/** How this mod's content is crafted. */
@Generator
public final class ModRecipes extends EmberRecipeProvider {

    /** Instantiated by Ember from the compile-time index. */
    public ModRecipes() {
    }

    @Override
    protected void recipes() {
        // Nine rubies make a block, and a block gives them back.
        shaped(ModBlocks.RUBY_BLOCK)
                .pattern("###", "###", "###")
                .define('#', ModItems.RUBY)
                .save();

        shapeless(ModItems.RUBY, 9)
                .ingredient(ModBlocks.RUBY_BLOCK)
                .named("ruby_from_block")
                .save();

        // A tool, to show an ingredient from vanilla alongside one of ours.
        shaped(ModItems.RUBY_HAMMER)
                .pattern("###", " | ", " | ")
                .define('#', ModItems.RUBY)
                .define('|', "minecraft:stick")
                .save();
    }
}
