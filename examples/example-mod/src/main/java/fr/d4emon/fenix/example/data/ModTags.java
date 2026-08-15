package fr.d4emon.fenix.example.data;

import fr.d4emon.fenix.example.registry.ModBlocks;
import fr.d4emon.fenix.example.registry.ModItems;

import fr.d4emon.fenix.ember.EmberTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.EnchantmentTags;
import fr.d4emon.fenix.example.registry.ModContent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
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

        /**
         * A tag of this mod's own, holding the nine shapes cut from a ruby
         * block. Nothing declares a constant for it, so it is named by hand —
         * the case the string form of {@code tag} exists for.
         */
        private static final String SHAPES = "example-mod:ruby_shapes";

        @Override
        protected void tags() {
            // Named through the game's own constants rather than by string.
            // A constant that does not exist fails the build; a misspelled
            // string writes a valid file into a tag nothing reads, which is
            // how the fences came to stand alone in a row of fences.
            //
            // Every one of these declares requiresTool(), and without a
            // mineable tag no tool is the right one — so they would break
            // without ever dropping.
            // The nine cut shapes as one family, so the tags below can name
            // them once instead of nine times. This is how vanilla composes:
            // #fences is #wooden_fences plus the nether brick one, not a list
            // of every fence in the game.
            tag(SHAPES)
                    .add(ModBlocks.RUBY_SLAB)
                    .add(ModBlocks.RUBY_STAIRS)
                    .add(ModBlocks.RUBY_FENCE)
                    .add(ModBlocks.RUBY_GATE)
                    .add(ModBlocks.RUBY_WALL)
                    .add(ModBlocks.RUBY_TRAPDOOR)
                    .add(ModBlocks.RUBY_DOOR)
                    .add(ModBlocks.RUBY_BUTTON)
                    .add(ModBlocks.RUBY_PLATE);

            tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .add(ModBlocks.RUBY_BLOCK)
                    .add(ModBlocks.GLOWING_RUBY_BLOCK)
                    .add(ModBlocks.RUBY_TALLY)
                    .add(ModBlocks.RUBY_SAFE)
                    .add(ModBlocks.RUBY_REFORGING)
                    .add(ModBlocks.RUBY_ORE)
                    .add(ModBlocks.DEEPSLATE_RUBY_ORE)
                    // The cut shapes were missing from here entirely. Seven of
                    // them declare requiresTool(), and a block that requires a
                    // tool no tag names cannot be broken for its drop by any
                    // tool at all — it takes a long time and gives back
                    // nothing. The button and plate instabreak, so they are
                    // here for the tool sound rather than for the drop.
                    .addTag(SHAPES);

            tag(BlockTags.NEEDS_IRON_TOOL)
                    .add(ModBlocks.RUBY_BLOCK)
                    .add(ModBlocks.GLOWING_RUBY_BLOCK)
                    .add(ModBlocks.RUBY_ORE)
                    .add(ModBlocks.DEEPSLATE_RUBY_ORE)
                    .add(ModBlocks.RUBY_SLAB)
                    .add(ModBlocks.RUBY_STAIRS)
                    .add(ModBlocks.RUBY_FENCE)
                    .add(ModBlocks.RUBY_GATE)
                    .add(ModBlocks.RUBY_WALL)
                    .add(ModBlocks.RUBY_TRAPDOOR)
                    .add(ModBlocks.RUBY_DOOR);

            // How a fence decides what to reach for. FenceBlock.isSameFence
            // asks the tag, not the class, so a fence outside it stands alone
            // in a row of its own kind — which is what happened here. The gate
            // kept working throughout, because a gate is matched by class.
            //
            // Not in wooden_fences, deliberately: the test is that both fences
            // answer the same way, and two blocks that are both absent agree.
            tag(BlockTags.FENCES).add(ModBlocks.RUBY_FENCE);
            tag(BlockTags.FENCE_GATES).add(ModBlocks.RUBY_GATE);

            // Walls read BlockTags.WALLS in the same way, and for the same
            // reason: a wall not in it connects to solid blocks and to nothing
            // that is a wall.
            tag(BlockTags.WALLS).add(ModBlocks.RUBY_WALL);

            // The rest are not load-bearing for shape, but they are what other
            // mods and vanilla's own behaviours look at — a door in no doors
            // tag is invisible to anything reasoning about doors.
            tag(BlockTags.SLABS).add(ModBlocks.RUBY_SLAB);
            tag(BlockTags.STAIRS).add(ModBlocks.RUBY_STAIRS);
            tag(BlockTags.DOORS).add(ModBlocks.RUBY_DOOR);
            tag(BlockTags.TRAPDOORS).add(ModBlocks.RUBY_TRAPDOOR);
            tag(BlockTags.BUTTONS).add(ModBlocks.RUBY_BUTTON);
            tag(BlockTags.PRESSURE_PLATES).add(ModBlocks.RUBY_PLATE);
        }
    }

    /** Which entities belong to which groups. */
    @Generator
    public static final class Entities extends EmberTagsProvider.EntityTagsProvider {

        /** Instantiated by Ember from the compile-time index. */
        public Entities() {
        }

        @Override
        protected void tags() {
            // Without this the sprite is not counted as a mob by anything that
            // reasons about mobs — spawners, the beacon, other mods' rules.
            tag(EntityTypeTags.SENSITIVE_TO_SMITE).add(ModContent.RUBY_SPRITE);
        }
    }

    /** Which damage types belong to which groups. */
    @Generator
    public static final class DamageTypes extends EmberTagsProvider.DamageTypeTagsProvider {

        /** Instantiated by Ember from the compile-time index. */
        public DamageTypes() {
        }

        @Override
        protected void tags() {
            // A damage type in no tag is one that armour, enchantments and the
            // game rules have never heard of. Fire damage that is not in
            // is_fire ignores Fire Protection, and nothing says why.
            tag(DamageTypeTags.IS_FIRE).add("example-mod:ruby_burn");
            tag(DamageTypeTags.IS_PROJECTILE).add("example-mod:ruby_shard");
        }
    }

    /** Which enchantments belong to which groups. */
    @Generator
    public static final class Enchantments extends EmberTagsProvider.EnchantmentTagsProvider {

        /** Instantiated by Ember from the compile-time index. */
        public Enchantments() {
        }

        @Override
        protected void tags() {
            // Without this the enchantment exists, can be given by command,
            // and is never once offered by an enchanting table. Nothing says
            // so; it reads as the table being unlucky.
            tag(EnchantmentTags.IN_ENCHANTING_TABLE).add("example-mod:ruby_edge");
            tag(EnchantmentTags.NON_TREASURE).add("example-mod:ruby_edge");
        }
    }

    /** Which fluids belong to which groups. */
    @Generator
    public static final class Fluids extends EmberTagsProvider.FluidTagsProvider {

        /** Instantiated by Ember from the compile-time index. */
        public Fluids() {
        }

        @Override
        protected void tags() {
            // A group of the mod's own: nothing in vanilla means "brine", and
            // claiming the water tag would make every water check treat this
            // as water.
            tag("example-mod:brines").add("example-mod:ruby_brine");
            tag("example-mod:brines").add("example-mod:flowing_ruby_brine");
        }
    }

    /** Which game events belong to which groups. */
    @Generator
    public static final class GameEvents extends EmberTagsProvider.GameEventTagsProvider {

        /** Instantiated by Ember from the compile-time index. */
        public GameEvents() {
        }

        @Override
        protected void tags() {
            // A game event outside this tag is one sculk cannot hear, which is
            // most of the reason to have declared it.
            tag(GameEventTags.VIBRATIONS).add("example-mod:ruby_chime_event");
        }
    }

    /** Which items belong to which groups. */
    @Generator
    public static final class Items extends EmberTagsProvider.ItemTagsProvider {

        /** Instantiated by Ember from the compile-time index. */
        public Items() {
        }

        /**
         * A tag of this mod's own, holding the nine shapes cut from a ruby
         * block. Nothing declares a constant for it, so it is named by hand —
         * the case the string form of {@code tag} exists for.
         */
        private static final String SHAPES = "example-mod:ruby_shapes";

        @Override
        protected void tags() {
            // A tag of the mod's own: nothing declares a constant for it, which
            // is exactly when the string form is the right one.
            tag("example-mod:gems").add(ModItems.RUBY);
        }
    }
}
