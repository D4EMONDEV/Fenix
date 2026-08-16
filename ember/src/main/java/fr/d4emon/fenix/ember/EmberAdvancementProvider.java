package fr.d4emon.fenix.ember;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.RegistryOps;
import net.minecraft.data.registries.VanillaRegistries;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.Advancement;

import fr.d4emon.fenix.registry.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes advancements — the tree a player works through, and the toasts that
 * appear when they do.
 *
 * <p>An advancement is a criterion, a display, and a place in a tree. All three
 * are JSON, and the JSON is unforgiving in the quiet way: a trigger whose name
 * does not exist, or a criterion that no requirement names, produces a file the
 * game loads and an advancement nobody can ever earn. Nothing logs.
 *
 * <pre>{@code
 * @Generator
 * public final class ModAdvancements extends EmberAdvancementProvider {
 *
 *     @Override
 *     protected void advancements() {
 *         advancement("root")
 *                 .title("Ruby Age")
 *                 .description("Find your first ruby.")
 *                 .icon(ModItems.RUBY)
 *                 .background("minecraft:block/stone")
 *                 .hasItem("ruby", ModItems.RUBY)
 *                 .save();
 *
 *         advancement("full_set")
 *                 .parent("example-mod:root")
 *                 .title("Cut to Fit")
 *                 .description("Craft every ruby shape.")
 *                 .icon(ModBlocks.RUBY_STAIRS)
 *                 .challenge()
 *                 .experience(100)
 *                 .hasItem("stairs", ModBlocks.RUBY_STAIRS)
 *                 .hasItem("slab", ModBlocks.RUBY_SLAB)
 *                 .save();
 *     }
 * }
 * }</pre>
 *
 * <p>Every criterion added is required by default, and they are combined with
 * AND — which is what "craft every shape" means. {@link Builder#requireAny()}
 * switches to OR.
 */
public abstract class EmberAdvancementProvider extends EmberProvider {

    /** For subclasses. */
    protected EmberAdvancementProvider() {
    }

    /** Describes the advancements. */
    protected abstract void advancements();

    @Override
    protected final void run() {
        advancements();
    }

    /**
     * Starts an advancement.
     *
     * @param name the path part of its id; may contain slashes to make a
     *             directory, as vanilla does with {@code story/mine_stone}
     * @return a builder; call {@code save()} when done
     */
    protected final Builder advancement(String name) {
        return new Builder(this, name);
    }

    private void save(String name, String json) {
        // Read back with the game's own codec before it is written. Ember runs
        // inside a real Minecraft with the mod already registered, so every id
        // the file names — including triggers the mod added itself — is
        // resolvable here and nowhere else. A conformance run has vanilla only
        // and has to substitute; this does not.
        //
        // Worth doing because of what an advancement does when it is wrong: a
        // trigger that does not exist, a criterion no requirement names, an
        // icon that is not an item. The game logs one line at pack load and
        // then behaves as though the advancement were merely unearned, which
        // is indistinguishable from conditions that are hard.
        Advancement.CODEC.parse(registryOps(), JsonParser.parseString(json))
                .getOrThrow(message -> new IllegalStateException(
                        "advancement " + name + " would not load: " + message));

        output().data("advancement/" + name + ".json", json);
    }

    /**
     * The ops the check above reads with, built once.
     *
     * <p>Not {@code JsonOps.INSTANCE}: a criterion names items as a holder set,
     * and a holder set is resolved through the ops rather than parsed out of
     * the JSON. Plain ops have no registry to resolve against, so every item in
     * every criterion fails — and the message it fails with is "Not a json
     * array", which describes the last branch tried rather than the cause.
     *
     * <p>Building the lookup runs the datapack registries, which is slow enough
     * to be worth doing once for a whole generation rather than once per file.
     */
    private static DynamicOps<JsonElement> registryOps() {
        if (ops == null) {
            ops = RegistryOps.create(JsonOps.INSTANCE, VanillaRegistries.createLookup());
        }
        return ops;
    }

    private static DynamicOps<JsonElement> ops;

    /** Collects one advancement. */
    public static final class Builder {

        private final EmberAdvancementProvider provider;
        private final String name;
        private final Map<String, String> criteria = new LinkedHashMap<>();
        private final List<String> rewardRecipes = new ArrayList<>();

        private String parent;
        private String title = "";
        private String description = "";
        private String icon;
        private String background;
        private String frame;
        private boolean hidden;
        private boolean showToast = true;
        private boolean announce = true;
        private boolean any;
        private int experience;

        private Builder(EmberAdvancementProvider provider, String name) {
            this.provider = provider;
            this.name = name;
        }

        /**
         * The advancement this one hangs from.
         *
         * <p>Left out, this advancement is a root and starts a tab of its own —
         * which also means it wants a {@link #background}.
         *
         * @param id the parent's full id
         * @return this builder
         */
        public Builder parent(String id) {
            this.parent = id;
            return this;
        }

        /**
         * @param text the name shown on the toast and in the tree
         * @return this builder
         */
        public Builder title(String text) {
            this.title = text;
            return this;
        }

        /**
         * @param text the line under the title
         * @return this builder
         */
        public Builder description(String text) {
            this.description = text;
            return this;
        }

        /**
         * @param item what is drawn in the frame
         * @return this builder
         */
        public Builder icon(Holder<?> item) {
            this.icon = EmberOutput.idOf(item.get()).toString();
            return this;
        }

        /**
         * The texture behind a root advancement's tab.
         *
         * @param texture a texture id, such as {@code minecraft:block/stone}
         * @return this builder
         */
        public Builder background(String texture) {
            this.background = texture;
            return this;
        }

        /** Draws the ornate frame vanilla uses for its hardest advancements.
         *
         * @return this builder
         */
        public Builder challenge() {
            this.frame = "challenge";
            return this;
        }

        /** Draws the rounded frame vanilla uses for milestones.
         *
         * @return this builder
         */
        public Builder goal() {
            this.frame = "goal";
            return this;
        }

        /** Hides this until it is earned.
         *
         * @return this builder
         */
        public Builder hidden() {
            this.hidden = true;
            return this;
        }

        /** Earns silently: no toast, no chat line.
         *
         * @return this builder
         */
        public Builder quiet() {
            this.showToast = false;
            this.announce = false;
            return this;
        }

        /**
         * @param amount experience granted on completion
         * @return this builder
         */
        public Builder experience(int amount) {
            this.experience = amount;
            return this;
        }

        /**
         * Unlocks a recipe when this is earned.
         *
         * @param recipe the recipe's id
         * @return this builder
         */
        public Builder unlocks(String recipe) {
            rewardRecipes.add(recipe);
            return this;
        }

        /**
         * Any one criterion is enough, rather than all of them.
         *
         * @return this builder
         */
        public Builder requireAny() {
            this.any = true;
            return this;
        }

        /**
         * Earned by having an item in the inventory at some point.
         *
         * @param key  a name for this criterion, unique within the advancement
         * @param item what to look for
         * @return this builder
         */
        public Builder hasItem(String key, Holder<?> item) {
            return hasItemId(key, EmberOutput.idOf(item.get()).toString());
        }

        /**
         * Earned by having anything in a tag.
         *
         * @param key a name for this criterion
         * @param tag what counts
         * @return this builder
         */
        public Builder hasItem(String key, TagKey<Item> tag) {
            return hasItemId(key, "#" + tag.location());
        }

        private Builder hasItemId(String key, String id) {
            criteria.put(key, """
                    {
                          "trigger": "minecraft:inventory_changed",
                          "conditions": {
                            "items": [
                              {
                                "items": %s
                              }
                            ]
                          }
                        }""".formatted(EmberOutput.quote(id)));
            return this;
        }

        /**
         * Earned by killing one of a kind of entity.
         *
         * @param key    a name for this criterion
         * @param entity the entity type's id
         * @return this builder
         */
        public Builder killed(String key, String entity) {
            criteria.put(key, """
                    {
                          "trigger": "minecraft:player_killed_entity",
                          "conditions": {
                            "entity": [
                              {
                                "condition": "minecraft:entity_properties",
                                "entity": "this",
                                "predicate": {
                                  "minecraft:entity_type": %s
                                }
                              }
                            ]
                          }
                        }""".formatted(EmberOutput.quote(entity)));
            return this;
        }

        /**
         * Earned only when a mod's own code says so.
         *
         * <p>The {@code impossible} trigger never fires on its own, which is
         * how vanilla writes an advancement that some other rule grants.
         *
         * @param key a name for this criterion
         * @return this builder
         */
        public Builder grantedByCode(String key) {
            criteria.put(key, """
                    {
                          "trigger": "minecraft:impossible"
                        }""");
            return this;
        }

        /**
         * A criterion this builder has no helper for.
         *
         * <p>There are around eighty triggers and most are rare. This takes the
         * conditions verbatim, so anything vanilla can express is reachable
         * without waiting for a method to be added.
         *
         * @param key        a name for this criterion
         * @param trigger    the trigger's id, such as {@code minecraft:bred_animals}
         * @param conditions the conditions object, JSON, or {@code null} for none
         * @return this builder
         */
        public Builder criterion(String key, String trigger, String conditions) {
            String body = conditions == null || conditions.isBlank()
                    ? "{\n      \"trigger\": %s\n    }".formatted(EmberOutput.quote(trigger))
                    : """
                    {
                          "trigger": %s,
                          "conditions": %s
                        }""".formatted(EmberOutput.quote(trigger), conditions);
            criteria.put(key, body);
            return this;
        }

        /** Writes the advancement. */
        public void save() {
            if (criteria.isEmpty()) {
                // An advancement with no criteria loads and can never be
                // earned, which looks exactly like one whose trigger is wrong.
                throw new IllegalStateException(
                        name + " has no criteria, so nothing could ever earn it");
            }

            StringBuilder json = new StringBuilder("{\n");
            if (parent != null) {
                json.append("  \"parent\": ").append(EmberOutput.quote(parent)).append(",\n");
            }

            json.append("  \"criteria\": {");
            String separator = "\n    ";
            for (Map.Entry<String, String> entry : criteria.entrySet()) {
                json.append(separator).append(EmberOutput.quote(entry.getKey()))
                        .append(": ").append(entry.getValue());
                separator = ",\n    ";
            }
            json.append("\n  },\n");

            json.append("  \"display\": {\n")
                    .append("    \"title\": ").append(component(title)).append(",\n")
                    .append("    \"description\": ").append(component(description)).append(",\n")
                    .append("    \"icon\": {\n      \"id\": ")
                    .append(EmberOutput.quote(icon == null ? "minecraft:stone" : icon))
                    .append("\n    }");
            if (background != null) {
                json.append(",\n    \"background\": ").append(EmberOutput.quote(background));
            }
            if (frame != null) {
                json.append(",\n    \"frame\": ").append(EmberOutput.quote(frame));
            }
            if (hidden) {
                json.append(",\n    \"hidden\": true");
            }
            if (!showToast) {
                json.append(",\n    \"show_toast\": false");
            }
            if (!announce) {
                json.append(",\n    \"announce_to_chat\": false");
            }
            json.append("\n  },\n");

            // Requirements are an OR of ANDs. One inner list holding every
            // criterion is "all of them"; one list each is "any of them".
            json.append("  \"requirements\": [");
            if (any) {
                json.append("\n    [");
                String inner = "";
                for (String key : criteria.keySet()) {
                    json.append(inner).append("\n      ").append(EmberOutput.quote(key));
                    inner = ",";
                }
                json.append("\n    ]");
            } else {
                String outer = "";
                for (String key : criteria.keySet()) {
                    json.append(outer).append("\n    [\n      ")
                            .append(EmberOutput.quote(key)).append("\n    ]");
                    outer = ",";
                }
            }
            json.append("\n  ]");

            if (experience > 0 || !rewardRecipes.isEmpty()) {
                json.append(",\n  \"rewards\": {");
                String inner = "\n    ";
                if (experience > 0) {
                    json.append(inner).append("\"experience\": ").append(experience);
                    inner = ",\n    ";
                }
                if (!rewardRecipes.isEmpty()) {
                    json.append(inner).append("\"recipes\": [");
                    String each = "\n      ";
                    for (String recipe : rewardRecipes) {
                        json.append(each).append(EmberOutput.quote(recipe));
                        each = ",\n      ";
                    }
                    json.append("\n    ]");
                }
                json.append("\n  }");
            }

            json.append("\n}\n");
            provider.save(name, json.toString());
        }

        /**
         * A title or description as the game wants it.
         *
         * <p>Literal text rather than a translation key: a mod writing its
         * advancements here has its language files here too, and one string in
         * two places is one string that goes out of step.
         */
        private static String component(String text) {
            return "{\n      \"text\": %s\n    }".formatted(EmberOutput.quote(text));
        }
    }
}
