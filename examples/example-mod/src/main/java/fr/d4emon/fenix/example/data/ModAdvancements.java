package fr.d4emon.fenix.example.data;

import fr.d4emon.fenix.ember.EmberAdvancementProvider;
import fr.d4emon.fenix.ember.Generator;
import fr.d4emon.fenix.example.registry.ModBlocks;
import fr.d4emon.fenix.example.registry.ModContent;
import fr.d4emon.fenix.example.registry.ModItems;

/** A small tree, to show what a mod's advancements look like. */
@Generator
public final class ModAdvancements extends EmberAdvancementProvider {

    /** Instantiated by Ember from the compile-time index. */
    public ModAdvancements() {
    }

    @Override
    protected void advancements() {
        // A root opens a tab of its own, so it needs a background. Without one
        // the tab draws on nothing and looks broken rather than empty.
        advancement("root")
                .title("Ruby Age")
                .description("Find your first ruby.")
                .icon(ModItems.RUBY)
                .background("minecraft:block/deepslate")
                .hasItem("ruby", ModItems.RUBY)
                .save();

        advancement("hammer")
                .parent("example-mod:root")
                .title("Something to Hit With")
                .description("Craft a ruby hammer.")
                .icon(ModItems.RUBY_HAMMER)
                .hasItem("hammer", ModItems.RUBY_HAMMER)
                .unlocks("example-mod:ruby_hammer")
                .save();

        // Every criterion required, which is what "all nine" means. One
        // criterion per shape, combined with AND.
        advancement("every_shape")
                .parent("example-mod:root")
                .title("Cut to Fit")
                .description("Collect all nine shapes cut from a ruby block.")
                .icon(ModBlocks.RUBY_STAIRS)
                .challenge()
                .experience(100)
                .hasItem("slab", ModBlocks.RUBY_SLAB)
                .hasItem("stairs", ModBlocks.RUBY_STAIRS)
                .hasItem("fence", ModBlocks.RUBY_FENCE)
                .hasItem("gate", ModBlocks.RUBY_GATE)
                .hasItem("wall", ModBlocks.RUBY_WALL)
                .hasItem("trapdoor", ModBlocks.RUBY_TRAPDOOR)
                .hasItem("button", ModBlocks.RUBY_BUTTON)
                .hasItem("plate", ModBlocks.RUBY_PLATE)
                .hasItem("door", ModBlocks.RUBY_DOOR)
                .save();

        // The one advancement here no vanilla trigger could express: the count
        // lives on the player as an attachment, and only the mod that keeps it
        // can say when it is high enough.
        advancement("well_swung")
                .parent("example-mod:hammer")
                .title("Well Swung")
                .description("Swing the ruby hammer twenty-five times.")
                .icon(ModItems.RUBY_HAMMER)
                .goal()
                .experience(50)
                .criterion("swung", "example-mod:swings", "{\"at_least\": 25}")
                .save();

        // Either egg will do, so the criteria are combined with OR instead.
        advancement("a_friend")
                .parent("example-mod:root")
                .title("Not Alone")
                .description("Meet something the mod brought with it.")
                .icon(ModContent.RUBY_SPRITE_SPAWN_EGG)
                .goal()
                .requireAny()
                .hasItem("sprite", ModContent.RUBY_SPRITE_SPAWN_EGG)
                .hasItem("wisp", ModContent.RUBY_WISP_SPAWN_EGG)
                .save();
    }
}
