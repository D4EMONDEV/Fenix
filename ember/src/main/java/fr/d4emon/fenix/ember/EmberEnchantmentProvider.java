package fr.d4emon.fenix.ember;

import net.minecraft.world.item.enchantment.Enchantment;
import com.google.gson.JsonParser;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes enchantments.
 *
 * <p>Since 1.21 an enchantment is data rather than a class, which is why there
 * is no {@code Registrar.enchantment}: there is nothing to register. What a mod
 * ships is a JSON file, and this writes it.
 *
 * <pre>{@code
 * @Generator
 * public final class ModEnchantments extends EmberEnchantmentProvider {
 *     @Override
 *     protected void enchantments() {
 *         enchantment("ruby_edge")
 *                 .description("Ruby Edge")
 *                 .supports(ItemTags.SWORDS)
 *                 .slots(Slot.MAINHAND)
 *                 .maxLevel(3)
 *                 .weight(5)
 *                 .cost(5, 9, 25, 9)
 *                 .addsDamage(1.0f, 0.5f)
 *                 .save();
 *     }
 * }
 * }</pre>
 *
 * <p>The frame of an enchantment — what it goes on, how rare it is, what it
 * costs — has methods here. Its <em>effects</em> are a language of their own,
 * wide enough that vanilla uses a dozen shapes across its own enchantments, so
 * {@link Builder#effect} takes them as JSON rather than pretending a builder
 * could cover them. {@link Builder#addsDamage} is the one shape common enough
 * to be worth naming.
 */
public abstract class EmberEnchantmentProvider extends EmberProvider {

    /** Where an enchanted item has to be worn or held for it to do anything. */
    public enum Slot {
        ANY("any"), MAINHAND("mainhand"), OFFHAND("offhand"), HAND("hand"),
        FEET("feet"), LEGS("legs"), CHEST("chest"), HEAD("head"),
        ARMOR("armor"), BODY("body"), SADDLE("saddle");

        private final String id;

        Slot(String id) {
            this.id = id;
        }
    }

    /** For subclasses. */
    protected EmberEnchantmentProvider() {
    }

    /** Describes the enchantments. */
    protected abstract void enchantments();

    @Override
    protected final void run() {
        enchantments();
    }

    /**
     * Starts an enchantment.
     *
     * @param name the path part of its id
     * @return a builder; call {@code save()} when done
     */
    protected final Builder enchantment(String name) {
        return new Builder(this, name);
    }

    private void save(String name, String json) {
        // Read back with the game's own codec before it is written. Ember runs
        // inside a real Minecraft with the mod registered, so an effect type
        // the mod invented resolves here and nowhere else — the conformance
        // suite has vanilla only and has to leave those entries out.
        //
        // Worth doing because of how an enchantment fails: one malformed
        // effect makes the whole file fail to load, and the enchantment is
        // then absent from the table and the anvil with one line in the log.
        Enchantment.DIRECT_CODEC.parse(registryOps(), JsonParser.parseString(json))
                .getOrThrow(message -> new IllegalStateException(
                        "enchantment " + name + " would not load: " + message));

        output().data("enchantment/" + name + ".json", json);
    }

    /** Collects one enchantment. */
    public static final class Builder {

        private final EmberEnchantmentProvider provider;
        private final String name;
        private final List<Slot> slots = new ArrayList<>();
        private final Map<String, List<String>> effects = new LinkedHashMap<>();

        private String description;
        private String supported;
        private String primary;
        private String exclusive;
        private int weight = 10;
        private int maxLevel = 1;
        private int anvilCost = 1;
        private int minCostBase = 1;
        private int minCostPerLevel = 10;
        private int maxCostBase = 21;
        private int maxCostPerLevel = 10;

        private Builder(EmberEnchantmentProvider provider, String name) {
            this.provider = provider;
            this.name = name;
        }

        /**
         * The name shown on the item and in the enchanting table.
         *
         * <p>Written as literal text, not a translation key: a mod's language
         * files are generated here too, and one string in two places is one
         * string that goes out of step.
         *
         * @param text the name
         * @return this builder
         */
        public Builder description(String text) {
            this.description = text;
            return this;
        }

        /**
         * What the enchantment can go on at all, by tag.
         *
         * <p>Required. An enchantment supporting nothing can be applied to
         * nothing, and the file loads perfectly well.
         *
         * @param tag the items it may be applied to
         * @return this builder
         */
        public Builder supports(TagKey<Item> tag) {
            this.supported = "#" + tag.location();
            return this;
        }

        /**
         * What the enchanting table will offer it for, if narrower than
         * {@link #supports}.
         *
         * @param tag the items an enchanting table offers it on
         * @return this builder
         */
        public Builder primary(TagKey<Item> tag) {
            this.primary = "#" + tag.location();
            return this;
        }

        /**
         * The group it cannot be combined with.
         *
         * @param tag an exclusive set, such as {@code #minecraft:exclusive_set/damage}
         * @return this builder
         */
        public Builder exclusiveWith(TagKey<net.minecraft.world.item.enchantment.Enchantment> tag) {
            this.exclusive = "#" + tag.location();
            return this;
        }

        /**
         * @param slots where the item must be for it to apply
         * @return this builder
         */
        public Builder slots(Slot... slots) {
            this.slots.addAll(List.of(slots));
            return this;
        }

        /**
         * @param level the highest level it goes to
         * @return this builder
         */
        public Builder maxLevel(int level) {
            this.maxLevel = level;
            return this;
        }

        /**
         * How likely the enchanting table is to offer it.
         *
         * <p>Vanilla's range is 1 for the rarest to 10 for the commonest.
         *
         * @param weight the weight
         * @return this builder
         */
        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        /**
         * @param levels what combining it on an anvil costs
         * @return this builder
         */
        public Builder anvilCost(int levels) {
            this.anvilCost = levels;
            return this;
        }

        /**
         * The experience range the enchanting table offers it in.
         *
         * @param minBase         the cheapest at level one
         * @param minPerLevel     added to that per level above the first
         * @param maxBase         the dearest at level one
         * @param maxPerLevel     added to that per level above the first
         * @return this builder
         */
        public Builder cost(int minBase, int minPerLevel, int maxBase, int maxPerLevel) {
            this.minCostBase = minBase;
            this.minCostPerLevel = minPerLevel;
            this.maxCostBase = maxBase;
            this.maxCostPerLevel = maxPerLevel;
            return this;
        }

        /**
         * Adds to the damage the item deals, the way Sharpness does.
         *
         * @param base       added at level one
         * @param perLevel   added again per level above the first
         * @return this builder
         */
        public Builder addsDamage(float base, float perLevel) {
            return effect("minecraft:damage", """
                    {
                            "effect": {
                              "type": "minecraft:add",
                              "value": {
                                "type": "minecraft:linear",
                                "base": %s,
                                "per_level_above_first": %s
                              }
                            }
                          }""".formatted(EmberOutput.decimal(base), EmberOutput.decimal(perLevel)));
        }

        /**
         * An effect this builder has no method for.
         *
         * <p>Vanilla's effect components are a language of their own, and most
         * of it is rare. This takes one entry verbatim, so anything the game
         * can express is reachable without waiting for a method.
         *
         * @param component the effect component's id, such as
         *                  {@code minecraft:post_attack}
         * @param json      one entry of that component's list
         * @return this builder
         */
        public Builder effect(String component, String json) {
            effects.computeIfAbsent(component, key -> new ArrayList<>()).add(json);
            return this;
        }

        /** Writes the enchantment. */
        public void save() {
            if (supported == null) {
                // The file would load and the enchantment could go on nothing.
                throw new IllegalStateException(
                        name + " supports no items, so nothing could ever carry it");
            }
            if (slots.isEmpty()) {
                throw new IllegalStateException(
                        name + " names no slots, so it would never take effect");
            }

            StringBuilder json = new StringBuilder("{\n")
                    .append("  \"anvil_cost\": ").append(anvilCost).append(",\n")
                    .append("  \"description\": {\n    \"text\": ")
                    .append(EmberOutput.quote(description == null ? name : description))
                    .append("\n  },\n");

            if (!effects.isEmpty()) {
                json.append("  \"effects\": {");
                String between = "\n    ";
                for (Map.Entry<String, List<String>> entry : effects.entrySet()) {
                    json.append(between).append(EmberOutput.quote(entry.getKey()))
                            .append(": [\n      ")
                            .append(String.join(",\n      ", entry.getValue()))
                            .append("\n    ]");
                    between = ",\n    ";
                }
                json.append("\n  },\n");
            }

            if (exclusive != null) {
                json.append("  \"exclusive_set\": ").append(EmberOutput.quote(exclusive))
                        .append(",\n");
            }

            json.append("  \"max_cost\": {\n    \"base\": ").append(maxCostBase)
                    .append(",\n    \"per_level_above_first\": ").append(maxCostPerLevel)
                    .append("\n  },\n")
                    .append("  \"max_level\": ").append(maxLevel).append(",\n")
                    .append("  \"min_cost\": {\n    \"base\": ").append(minCostBase)
                    .append(",\n    \"per_level_above_first\": ").append(minCostPerLevel)
                    .append("\n  },\n");

            if (primary != null) {
                json.append("  \"primary_items\": ").append(EmberOutput.quote(primary))
                        .append(",\n");
            }

            json.append("  \"slots\": [");
            String between = "\n    ";
            for (Slot slot : slots) {
                json.append(between).append('"').append(slot.id).append('"');
                between = ",\n    ";
            }
            json.append("\n  ],\n")
                    .append("  \"supported_items\": ").append(EmberOutput.quote(supported))
                    .append(",\n")
                    .append("  \"weight\": ").append(weight).append("\n")
                    .append("}\n");

            provider.save(name, json.toString());
        }
    }
}
