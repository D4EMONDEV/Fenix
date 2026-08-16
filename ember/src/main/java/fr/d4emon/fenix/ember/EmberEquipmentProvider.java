package fr.d4emon.fenix.ember;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes equipment assets — which textures are drawn on something wearing a
 * mod's armour.
 *
 * <p>This is the half of armour that has nothing to do with protection. An
 * {@code ArmorMaterial} names an asset; this writes it. Without the file the
 * armour equips, protects, takes damage and is invisible on the body — and
 * nothing anywhere mentions it.
 *
 * <pre>{@code
 * @Generator
 * public final class ModEquipment extends EmberEquipmentProvider {
 *     @Override
 *     protected void equipment() {
 *         humanoidArmor("ruby");
 *     }
 * }
 * }</pre>
 *
 * <p>{@link #humanoidArmor} needs two textures, and the split is not where
 * anybody expects: leggings are drawn from a second file because they are a
 * different model layer, not because they are a different item.
 *
 * <ul>
 *   <li>{@code textures/entity/equipment/humanoid/<name>.png} — helmet,
 *       chestplate and boots
 *   <li>{@code textures/entity/equipment/humanoid_leggings/<name>.png} — the
 *       leggings
 * </ul>
 */
public abstract class EmberEquipmentProvider extends EmberProvider {

    /** For subclasses. */
    protected EmberEquipmentProvider() {
    }

    /** Describes the equipment assets. */
    protected abstract void equipment();

    @Override
    protected final void run() {
        equipment();
    }

    /**
     * Armour worn by players and most mobs.
     *
     * <p>Writes the four layers a humanoid set needs: the body, the baby
     * variant, and the leggings, which come from a texture of their own.
     *
     * @param name the asset's name, matching what the armour material names
     */
    protected final void humanoidArmor(String name) {
        String texture = modId() + ":" + name;
        write(name, Map.of(
                "humanoid", List.of(texture),
                "humanoid_baby", List.of(texture),
                "humanoid_leggings", List.of(texture)));
    }

    /**
     * Starts an asset with layers named by hand.
     *
     * <p>For anything that is not a humanoid set: horse armour, wolf armour, a
     * llama's carpet, a nautilus's shell.
     *
     * @param name the asset's name
     * @return a builder; call {@code save()} when done
     */
    protected final Builder asset(String name) {
        return new Builder(this, name);
    }

    private void write(String name, Map<String, List<String>> layers) {
        StringBuilder json = new StringBuilder("{\n  \"layers\": {");
        String between = "\n    ";

        // Sorted, because a map's order is not a decision anybody made and a
        // generated file that reshuffles between runs is a diff nobody can read.
        for (String layer : new java.util.TreeSet<>(layers.keySet())) {
            json.append(between).append(EmberOutput.quote(layer)).append(": [");
            String each = "\n        ";
            for (String texture : layers.get(layer)) {
                json.append(each).append("{\n          \"texture\": ")
                        .append(EmberOutput.quote(texture)).append("\n        }");
                each = ",\n        ";
            }
            json.append("\n      ]");
            between = ",\n    ";
        }

        json.append("\n  }\n}\n");
        output().asset("equipment/" + name + ".json", json.toString());
    }

    /** Collects one equipment asset. */
    public static final class Builder {

        private final EmberEquipmentProvider provider;
        private final String name;
        private final Map<String, List<String>> layers = new LinkedHashMap<>();

        private Builder(EmberEquipmentProvider provider, String name) {
            this.provider = provider;
            this.name = name;
        }

        /**
         * Adds one layer.
         *
         * @param layer   the layer's name, such as {@code humanoid} or
         *                {@code horse_body}
         * @param texture the texture's id, without the directory or extension
         * @return this builder
         */
        public Builder layer(String layer, String texture) {
            layers.computeIfAbsent(layer, key -> new ArrayList<>()).add(texture);
            return this;
        }

        /** Writes the asset. */
        public void save() {
            if (layers.isEmpty()) {
                throw new IllegalStateException(
                        name + " has no layers, so anything wearing it is invisible");
            }
            provider.write(name, layers);
        }
    }
}
