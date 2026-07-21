package fr.d4emon.fenix.ember;

import fr.d4emon.fenix.registry.Holder;
import net.minecraft.resources.Identifier;

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
     * Starts describing a tag.
     *
     * @param tag the tag's id, such as {@code minecraft:mineable/pickaxe}
     * @return a builder to add entries to
     */
    protected final Tag tag(String tag) {
        return new Tag(tags.computeIfAbsent(Identifier.parse(tag), key -> new ArrayList<>()));
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

        @Override
        String directory() {
            return "item";
        }
    }
}
