package fr.d4emon.fenix.example.data;

import fr.d4emon.fenix.example.registry.ModBlocks;
import fr.d4emon.fenix.example.registry.ModContent;
import fr.d4emon.fenix.example.registry.ModItems;

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
        cubeAll(ModBlocks.RUBY_REFORGING);
        // Pillars, so a log laid on its side keeps its end grain — the shape
        // vanilla's own logs have, and what an axis property is for.
        cubeColumn(ModBlocks.RUBY_LOG);
        cubeColumn(ModBlocks.STRIPPED_RUBY_LOG);
        // The four shapes cut from the ruby block. Each borrows its
        // texture rather than needing one of its own.
        slab(ModBlocks.RUBY_SLAB, ModBlocks.RUBY_BLOCK);
        fence(ModBlocks.RUBY_FENCE, ModBlocks.RUBY_BLOCK);
        wall(ModBlocks.RUBY_WALL, ModBlocks.RUBY_BLOCK);
        fenceGate(ModBlocks.RUBY_GATE, ModBlocks.RUBY_BLOCK);
        stairs(ModBlocks.RUBY_STAIRS, ModBlocks.RUBY_BLOCK);
        trapdoor(ModBlocks.RUBY_TRAPDOOR, ModBlocks.RUBY_BLOCK);
        button(ModBlocks.RUBY_BUTTON, ModBlocks.RUBY_BLOCK);
        pressurePlate(ModBlocks.RUBY_PLATE, ModBlocks.RUBY_BLOCK);
        door(ModBlocks.RUBY_DOOR);

        cubeAll(ModBlocks.RUBY_ORE);
        cubeAll(ModBlocks.DEEPSLATE_RUBY_ORE);

        // A spawn egg is an ordinary flat item in 26.2 — one texture, no tint
        // template — so there is nothing special to say about it here.
        flatItem(ModContent.RUBY_WISP_SPAWN_EGG);
        flatItem(ModContent.RUBY_SPRITE_SPAWN_EGG);

        // The bucket had a model written by hand and no item definition beside
        // it, which in 26.2 is what actually chooses a model — so it drew as
        // the missing texture no matter what the model said.
        flatItem(ModContent.RUBY_BRINE.bucket().orElseThrow());

        flatItem(ModItems.RUBY);
        flatItem(ModItems.RUBY_DISC);
        flatItem(ModItems.RUBY_HELMET);
        flatItem(ModItems.RUBY_CHESTPLATE);
        flatItem(ModItems.RUBY_LEGGINGS);
        flatItem(ModItems.RUBY_BOOTS);
        flatItem(ModItems.RUBY_HAMMER);
    }
}
