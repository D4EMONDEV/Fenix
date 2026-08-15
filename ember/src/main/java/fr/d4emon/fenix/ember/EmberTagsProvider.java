package fr.d4emon.fenix.ember;

import fr.d4emon.fenix.registry.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes tags — the groups the game and other mods reason about, like
 * "things a pickaxe mines" or "planks".
 *
 * <p>Use one of the nested providers; the split exists because a block tag and
 * an item tag are different files even when they hold the same names.
 *
 * <p>A tag file belongs to the <em>tag's</em> namespace, not the mod's. Adding
 * to {@code minecraft:mineable/pickaxe} writes into Minecraft's data directory,
 * and the game merges every pack's copy — which is exactly how a mod joins a
 * vanilla tag without replacing it.
 */
public abstract class EmberTagsProvider extends EmberProvider {

    private final Map<Identifier, List<String>> tags = new LinkedHashMap<>();

    private EmberTagsProvider() {
    }

    /** {@return the directory tags of this kind live in, such as {@code block}} */
    abstract String directory();

    /** Describes the tags. */
    protected abstract void tags();

    @Override
    protected final void run() {
        tags();
        tags.forEach((tag, values) -> {
            StringBuilder json = new StringBuilder("{\n  \"values\": [");
            String separator = "\n    ";
            for (String value : values) {
                json.append(separator).append(EmberOutput.quote(value));
                separator = ",\n    ";
            }
            output().data(tag.getNamespace(),
                    "tags/" + directory() + "/" + tag.getPath() + ".json",
                    json.append("\n  ]\n}\n").toString());
        });
    }

    /**
     * Starts describing a tag, named by hand.
     *
     * <p>For a tag with no constant to name it: the mod's own, or one belonging
     * to another mod that may not be installed. Where the game has a constant,
     * {@link BlockTagsProvider#tag(TagKey)} and its item counterpart are safer
     * — a misspelling here is a file the game reads, finds nothing wrong with,
     * and never uses.
     *
     * @param tag the tag's id, such as {@code minecraft:mineable/pickaxe}
     * @return a builder to add entries to
     */
    protected final Tag tag(String tag) {
        return new Tag(tags.computeIfAbsent(Identifier.parse(tag), key -> new ArrayList<>()));
    }

    /** Shared by the two typed overloads below. */
    final Tag tagOf(TagKey<?> tag) {
        return new Tag(tags.computeIfAbsent(tag.location(), key -> new ArrayList<>()));
    }

    /** Collects the contents of one tag. */
    public static final class Tag {

        private final List<String> values;

        private Tag(List<String> values) {
            this.values = values;
        }

        /**
         * Adds content to the tag.
         *
         * @param content a registered block or item
         * @return this builder
         */
        public Tag add(Holder<?> content) {
            values.add(EmberOutput.idOf(content.get()).toString());
            return this;
        }

        /**
         * Adds every member of another tag to this one.
         *
         * <p>A tag can hold tags, and vanilla leans on it: {@code fences} is
         * {@code #wooden_fences} plus the nether brick one. A mod adding a
         * whole family at once wants this rather than a line per block.
         *
         * <p>The reference is by name, so the other tag does not have to exist
         * — which is how a mod joins a tag another mod may or may not define.
         *
         * @param tag the tag whose members join this one
         * @return this builder
         */
        public Tag addTag(TagKey<?> tag) {
            return addTag(tag.location().toString());
        }

        /**
         * Adds every member of another tag, named by hand.
         *
         * <p>For a tag with no constant: the mod's own, or one belonging to a
         * mod that may not be installed.
         *
         * @param tag the tag's id, without the leading {@code #}
         * @return this builder
         */
        public Tag addTag(String tag) {
            values.add("#" + Identifier.parse(tag));
            return this;
        }

        /**
         * Adds something by id, for vanilla content or another mod's.
         *
         * @param id the full id, such as {@code minecraft:stone}
         * @return this builder
         */
        public Tag add(String id) {
            values.add(id);
            return this;
        }
    }

    /** Tags of blocks. */
    public abstract static class BlockTagsProvider extends EmberTagsProvider {

        /** For subclasses. */
        protected BlockTagsProvider() {
        }

        /**
         * Starts describing one of the game's own block tags.
         *
         * <pre>{@code
         * tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.RUBY_BLOCK);
         * }</pre>
         *
         * <p>Preferred over the string form wherever a constant exists. The
         * name is then javac's problem rather than the player's: a tag that
         * does not exist will not compile, where a misspelled string writes a
         * perfectly valid file into a tag nothing reads. That is how the demo's
         * fences spent three releases refusing to connect.
         *
         * <p>Typed, so a block tag cannot be described by the item provider.
         *
         * @param tag the tag, from {@code BlockTags} or another mod's constants
         * @return a builder to add entries to
         */
        protected final Tag tag(TagKey<Block> tag) {
            return tagOf(tag);
        }

        @Override
        String directory() {
            return "block";
        }
    }

    /** Tags of items. */
    public abstract static class ItemTagsProvider extends EmberTagsProvider {

        /** For subclasses. */
        protected ItemTagsProvider() {
        }

        /**
         * Starts describing one of the game's own item tags.
         *
         * <pre>{@code
         * tag(ItemTags.SWORDS).add(ModItems.RUBY_BLADE);
         * }</pre>
         *
         * @param tag the tag, from {@code ItemTags} or another mod's constants
         * @return a builder to add entries to
         * @see BlockTagsProvider#tag(TagKey)
         */
        protected final Tag tag(TagKey<Item> tag) {
            return tagOf(tag);
        }

        @Override
        String directory() {
            return "item";
        }
    }
}
