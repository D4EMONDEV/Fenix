package fr.d4emon.fenix.ember;

import fr.d4emon.fenix.registry.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes crafting recipes.
 *
 * <pre>{@code
 * @Generator
 * public final class ModRecipes extends EmberRecipeProvider {
 *     @Override
 *     protected void recipes() {
 *         shaped(ModBlocks.RUBY_BLOCK)
 *                 .pattern("###", "###", "###")
 *                 .define('#', ModItems.RUBY)
 *                 .save();
 *
 *         shapeless(ModItems.RUBY, 9).ingredient(ModBlocks.RUBY_BLOCK).save();
 *     }
 * }
 * }</pre>
 */
public abstract class EmberRecipeProvider extends EmberProvider {

    /** For subclasses. */
    protected EmberRecipeProvider() {
    }

    /** Describes the recipes. */
    protected abstract void recipes();

    @Override
    protected final void run() {
        recipes();
    }

    /**
     * A recipe where the arrangement matters.
     *
     * @param result what it makes
     * @return a builder; call {@code save()} when done
     */
    protected final Shaped shaped(Holder<?> result) {
        return shaped(result, 1);
    }

    /**
     * A recipe where the arrangement matters.
     *
     * @param result what it makes
     * @param count  how many
     * @return a builder; call {@code save()} when done
     */
    protected final Shaped shaped(Holder<?> result, int count) {
        return new Shaped(this, result, count);
    }

    /**
     * A recipe where only the ingredients matter.
     *
     * @param result what it makes
     * @return a builder; call {@code save()} when done
     */
    protected final Shapeless shapeless(Holder<?> result) {
        return shapeless(result, 1);
    }

    /**
     * A recipe where only the ingredients matter.
     *
     * @param result what it makes
     * @param count  how many
     * @return a builder; call {@code save()} when done
     */
    protected final Shapeless shapeless(Holder<?> result, int count) {
        return new Shapeless(this, result, count);
    }

    /**
     * Cutting one block into another on a stonecutter.
     *
     * <p>The natural recipe for a shape cut from a block — a slab, stairs, a
     * wall — and the one vanilla uses for all of its own. Without it a mod's
     * cut shapes exist, are modelled, drop correctly and cannot be made, which
     * is a creative-mode-only block wearing a survival block's coat.
     *
     * @param result what it makes
     * @param from   the block being cut
     */
    protected final void stonecutting(Holder<?> result, Holder<?> from) {
        stonecutting(result, from, 1);
    }

    /**
     * Cutting one block into another on a stonecutter.
     *
     * @param result what it makes
     * @param from   the block being cut
     * @param count  how many, which for a slab is two
     */
    protected final void stonecutting(Holder<?> result, Holder<?> from, int count) {
        String source = EmberOutput.idOf(from.get()).toString();
        // Named after both sides: one block cuts into nine shapes here, and
        // nine files called after the source alone would be one file.
        save(EmberOutput.idOf(result.get()).getPath() + "_from_"
                        + source.substring(source.indexOf(':') + 1) + "_stonecutting",
                """
                {
                  "type": "minecraft:stonecutting",
                  "ingredient": %s,
                  "result": %s
                }
                """.formatted(EmberOutput.quote(source), resultJson(result, count)));
    }

    /**
     * Smelting in a furnace.
     *
     * @param result     what it makes
     * @param ingredient what goes in
     * @param experience how much experience the result is worth
     * @param ticks      how long it takes; a furnace is 200 for most things
     */
    protected final void smelting(Holder<?> result, Holder<?> ingredient,
                                  float experience, int ticks) {
        cooking("smelting", result, ingredient, experience, ticks);
    }

    /**
     * Blasting in a blast furnace, which is twice as fast and gives no more.
     *
     * @param result     what it makes
     * @param ingredient what goes in
     * @param experience how much experience the result is worth
     * @param ticks      how long it takes; a blast furnace is 100 for most things
     */
    protected final void blasting(Holder<?> result, Holder<?> ingredient,
                                  float experience, int ticks) {
        cooking("blasting", result, ingredient, experience, ticks);
    }

    private void cooking(String type, Holder<?> result, Holder<?> ingredient,
                         float experience, int ticks) {
        String source = EmberOutput.idOf(ingredient.get()).toString();
        save(EmberOutput.idOf(result.get()).getPath() + "_from_" + type + "_"
                        + source.substring(source.indexOf(':') + 1),
                """
                {
                  "type": "minecraft:%s",
                  "cookingtime": %d,
                  "experience": %s,
                  "ingredient": %s,
                  "result": %s
                }
                """.formatted(type, ticks, experience,
                        EmberOutput.quote(source), resultJson(result, 1)));
    }

    /** Writes a finished recipe, named after its result unless told otherwise. */
    private void save(String name, String json) {
        output().data("recipe/" + name + ".json", json);
    }

    /** Vanilla omits a count of one, so generated files match hand-written ones. */
    private static String resultJson(Holder<?> result, int count) {
        String id = EmberOutput.idOf(result.get()).toString();
        return count == 1
                ? "{\n    \"id\": %s\n  }".formatted(EmberOutput.quote(id))
                : "{\n    \"count\": %d,\n    \"id\": %s\n  }".formatted(count, EmberOutput.quote(id));
    }

    private static String defaultName(Holder<?> result) {
        return EmberOutput.idOf(result.get()).getPath();
    }

    /** A recipe where the arrangement matters. */
    public static final class Shaped {

        private final EmberRecipeProvider provider;
        private final Holder<?> result;
        private final int count;
        private final List<String> rows = new ArrayList<>();
        private final Map<Character, String> keys = new LinkedHashMap<>();
        private String name;

        private Shaped(EmberRecipeProvider provider, Holder<?> result, int count) {
            this.provider = provider;
            this.result = result;
            this.count = count;
        }

        /**
         * The grid, one string per row.
         *
         * @param rows up to three rows of up to three characters; a space is empty
         * @return this builder
         */
        public Shaped pattern(String... rows) {
            this.rows.addAll(List.of(rows));
            return this;
        }

        /**
         * What a character in the pattern stands for.
         *
         * @param key        the character
         * @param ingredient registered content
         * @return this builder
         */
        public Shaped define(char key, Holder<?> ingredient) {
            return define(key, EmberOutput.idOf(ingredient.get()).toString());
        }

        /**
         * What a character stands for, when anything in a tag will do.
         *
         * <p>Which is how most vanilla recipes are written — a stick is two of
         * {@code #minecraft:planks}, not two of oak. A recipe naming one block
         * where a tag was meant works for that block and silently refuses every
         * other member of the family.
         *
         * @param key the character
         * @param tag what may go there
         * @return this builder
         */
        public Shaped define(char key, TagKey<Item> tag) {
            return define(key, "#" + tag.location());
        }

        /**
         * What a character stands for, by id, for vanilla content.
         *
         * @param key the character used in the pattern
         * @param id  what may go there, or {@code #namespace:tag} for a tag
         * @return this builder
         */
        public Shaped define(char key, String id) {
            keys.put(key, id);
            return this;
        }

        /**
         * Names the file, when the result's own name would collide.
         *
         * @param recipeName the file name, without extension
         * @return this builder
         */
        public Shaped named(String recipeName) {
            this.name = recipeName;
            return this;
        }

        /** Writes the recipe. */
        public void save() {
            StringBuilder json = new StringBuilder("{\n  \"type\": \"minecraft:crafting_shaped\",\n  \"key\": {");
            String separator = "\n    ";
            for (Map.Entry<Character, String> key : keys.entrySet()) {
                json.append(separator)
                        .append(EmberOutput.quote(String.valueOf(key.getKey())))
                        .append(": ").append(EmberOutput.quote(key.getValue()));
                separator = ",\n    ";
            }
            json.append("\n  },\n  \"pattern\": [");
            separator = "\n    ";
            for (String row : rows) {
                json.append(separator).append(EmberOutput.quote(row));
                separator = ",\n    ";
            }
            json.append("\n  ],\n  \"result\": ").append(resultJson(result, count)).append("\n}\n");

            provider.save(name != null ? name : defaultName(result), json.toString());
        }
    }

    /** A recipe where only the ingredients matter. */
    public static final class Shapeless {

        private final EmberRecipeProvider provider;
        private final Holder<?> result;
        private final int count;
        private final List<String> ingredients = new ArrayList<>();
        private String name;

        private Shapeless(EmberRecipeProvider provider, Holder<?> result, int count) {
            this.provider = provider;
            this.result = result;
            this.count = count;
        }

        /**
         * Adds an ingredient.
         *
         * @param ingredient registered content
         * @return this builder
         */
        public Shapeless ingredient(Holder<?> ingredient) {
            return ingredient(EmberOutput.idOf(ingredient.get()).toString());
        }

        /**
         * Adds an ingredient that may be anything in a tag.
         *
         * <p>Which is how most vanilla recipes are written — a stick is two of
         * {@code #minecraft:planks}, not two of oak. A recipe naming one block
         * where a tag was meant works for that block and silently refuses
         * every other member of the family.
         *
         * @param tag what may go in
         * @return this builder
         */
        public Shapeless ingredient(TagKey<Item> tag) {
            return ingredient("#" + tag.location());
        }

        /**
         * Adds an ingredient by id, for vanilla content.
         *
         * @param id the full id, such as {@code minecraft:diamond}
         * @return this builder
         */
        public Shapeless ingredient(String id) {
            ingredients.add(id);
            return this;
        }

        /**
         * Names the file, when the result's own name would collide.
         *
         * @param recipeName the file name, without extension
         * @return this builder
         */
        public Shapeless named(String recipeName) {
            this.name = recipeName;
            return this;
        }

        /** Writes the recipe. */
        public void save() {
            StringBuilder json = new StringBuilder(
                    "{\n  \"type\": \"minecraft:crafting_shapeless\",\n  \"ingredients\": [");
            String separator = "\n    ";
            for (String ingredient : ingredients) {
                json.append(separator).append(EmberOutput.quote(ingredient));
                separator = ",\n    ";
            }
            json.append("\n  ],\n  \"result\": ").append(resultJson(result, count)).append("\n}\n");

            provider.save(name != null ? name : defaultName(result), json.toString());
        }
    }
}
