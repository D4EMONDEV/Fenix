package fr.d4emon.fenix.example.data;

import fr.d4emon.fenix.example.registry.ModBlocks;
import fr.d4emon.fenix.example.registry.ModItems;

import fr.d4emon.fenix.ember.EmberRecipeProvider;
import net.minecraft.tags.ItemTags;
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

        // The nine cut shapes, on a stonecutter. Without these they are
        // registered, modelled, named, and obtainable only in creative — which
        // is not a block, it is a decoration for screenshots.
        stonecutting(ModBlocks.RUBY_SLAB, ModBlocks.RUBY_BLOCK, 2);
        stonecutting(ModBlocks.RUBY_STAIRS, ModBlocks.RUBY_BLOCK);
        stonecutting(ModBlocks.RUBY_FENCE, ModBlocks.RUBY_BLOCK);
        stonecutting(ModBlocks.RUBY_GATE, ModBlocks.RUBY_BLOCK);
        stonecutting(ModBlocks.RUBY_WALL, ModBlocks.RUBY_BLOCK);
        stonecutting(ModBlocks.RUBY_TRAPDOOR, ModBlocks.RUBY_BLOCK);
        stonecutting(ModBlocks.RUBY_BUTTON, ModBlocks.RUBY_BLOCK);
        stonecutting(ModBlocks.RUBY_PLATE, ModBlocks.RUBY_BLOCK);
        stonecutting(ModBlocks.RUBY_DOOR, ModBlocks.RUBY_BLOCK);

        // Both ores smelt, so the mod's ore behaves like every other ore a
        // player has met. Blasting is the same yield in half the time.
        smelting(ModItems.RUBY, ModBlocks.RUBY_ORE, 1.0f, 200);
        blasting(ModItems.RUBY, ModBlocks.RUBY_ORE, 1.0f, 100);

        // A tool, to show an ingredient from vanilla alongside one of ours.
        shaped(ModItems.RUBY_HAMMER)
                .pattern("###", " | ", " | ")
                .define('#', ModItems.RUBY)
                .define('|', "minecraft:stick")
                .save();

        // The three blocks with block entities had no recipe at all, so the
        // parts of this mod that show the most were reachable only in creative.
        //
        // Each takes planks by tag rather than by name. That is not decoration:
        // a recipe naming oak works with oak and silently refuses the other
        // eleven woods, and the player who tried birch concludes the mod is
        // broken rather than that they used the wrong plank.
        shaped(ModBlocks.RUBY_TALLY)
                .pattern("###", "#R#", "###")
                .define('#', ItemTags.PLANKS)
                .define('R', ModItems.RUBY)
                .save();

        shaped(ModBlocks.RUBY_SAFE)
                .pattern("RRR", "R R", "RRR")
                .define('R', ModItems.RUBY)
                .save();

        shaped(ModBlocks.RUBY_REFORGING)
                .pattern("BB ", "###", "###")
                .define('B', ModBlocks.RUBY_BLOCK)
                .define('#', ItemTags.PLANKS)
                .save();
    }
}
