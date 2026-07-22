package fr.d4emon.fenix.example.content;

import fr.d4emon.fenix.ember.EmberTagsProvider;
import fr.d4emon.fenix.ember.Generator;

/**
 * The groups this mod's content belongs to.
 *
 * <p>Joining {@code minecraft:mineable/pickaxe} is what actually makes a
 * pickaxe the right tool — the block's {@code requiresTool()} only says that
 * <em>some</em> correct tool is needed.
 */
public final class ModTags {

    private ModTags() {
    }

    /** Which blocks belong to which groups. */
    @Generator
    public static final class Blocks extends EmberTagsProvider.BlockTagsProvider {

        /** Instantiated by Ember from the compile-time index. */
        public Blocks() {
        }

        @Override
        protected void tags() {
            // Every one of these declares requiresTool(), and without a
            // mineable tag no tool is the right one — so they would break
            // without ever dropping.
            tag("minecraft:mineable/pickaxe")
                    .add(ModBlocks.RUBY_BLOCK)
                    .add(ModBlocks.GLOWING_RUBY_BLOCK)
                    .add(ModBlocks.RUBY_TALLY)
                    .add(ModBlocks.RUBY_SAFE)
                    .add(ModBlocks.RUBY_ORE)
                    .add(ModBlocks.DEEPSLATE_RUBY_ORE);

            tag("minecraft:needs_iron_tool")
                    .add(ModBlocks.RUBY_BLOCK)
                    .add(ModBlocks.GLOWING_RUBY_BLOCK)
                    .add(ModBlocks.RUBY_ORE)
                    .add(ModBlocks.DEEPSLATE_RUBY_ORE);
        }
    }

    /** Which items belong to which groups. */
    @Generator
    public static final class Items extends EmberTagsProvider.ItemTagsProvider {

        /** Instantiated by Ember from the compile-time index. */
        public Items() {
        }

        @Override
        protected void tags() {
            tag("example-mod:gems").add(ModItems.RUBY);
        }
    }
}
