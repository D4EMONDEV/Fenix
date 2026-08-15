package fr.d4emon.fenix.ember;

/**
 * Writes damage types — what a source of damage is, and how it is described.
 *
 * <p>Since damage became data, a mod that hurts a player has to declare the
 * kind of hurt first. Without one there is nothing to name in the death
 * message, nothing for armour and enchantments to consult, and nothing the
 * game rules can exempt.
 *
 * <pre>{@code
 * @Generator
 * public final class ModDamageTypes extends EmberDamageTypeProvider {
 *     @Override
 *     protected void damageTypes() {
 *         damageType("ruby_shard")
 *                 .message("ruby_shard")
 *                 .exhaustion(0.1f)
 *                 .save();
 *     }
 * }
 * }</pre>
 *
 * <p>The death message itself is a translation key the game builds from the
 * message id: {@code death.attack.<message>}. It belongs in the mod's language
 * file, and a damage type without one kills players with a blank line.
 */
public abstract class EmberDamageTypeProvider extends EmberProvider {

    /** How much the damage scales with difficulty. */
    public enum Scaling {
        /** The same on every difficulty. */
        NEVER("never"),
        /** Harder difficulties hurt more, unless a player dealt it. */
        WHEN_CAUSED_BY_LIVING_NON_PLAYER("when_caused_by_living_non_player"),
        /** Harder difficulties always hurt more. */
        ALWAYS("always");

        private final String id;

        Scaling(String id) {
            this.id = id;
        }
    }

    /** What the damage looks and sounds like. */
    public enum Effects {
        /** The ordinary hit. */
        HURT("hurt"),
        THORNS("thorns"),
        DROWNING("drowning"),
        BURNING("burning"),
        POKING("poking"),
        FREEZING("freezing");

        private final String id;

        Effects(String id) {
            this.id = id;
        }
    }

    /** For subclasses. */
    protected EmberDamageTypeProvider() {
    }

    /** Describes the damage types. */
    protected abstract void damageTypes();

    @Override
    protected final void run() {
        damageTypes();
    }

    /**
     * Starts a damage type.
     *
     * @param name the path part of its id
     * @return a builder; call {@code save()} when done
     */
    protected final Builder damageType(String name) {
        return new Builder(this, name);
    }

    private void save(String name, String json) {
        output().data("damage_type/" + name + ".json", json);
    }

    /** Collects one damage type. */
    public static final class Builder {

        private final EmberDamageTypeProvider provider;
        private final String name;

        private String message;
        private float exhaustion;
        private Scaling scaling = Scaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER;
        private Effects effects;

        private Builder(EmberDamageTypeProvider provider, String name) {
            this.provider = provider;
            this.name = name;
            this.message = name;
        }

        /**
         * The half of the death message key that is not fixed.
         *
         * <p>{@code message("ruby_shard")} means the game looks for
         * {@code death.attack.ruby_shard}. Defaults to the damage type's own
         * name, which is usually what you want.
         *
         * @param key the message id
         * @return this builder
         */
        public Builder message(String key) {
            this.message = key;
            return this;
        }

        /**
         * How much hunger the damage costs.
         *
         * <p>Vanilla uses 0.1 for most environmental damage and 0 for damage a
         * player cannot avoid.
         *
         * @param amount the exhaustion
         * @return this builder
         */
        public Builder exhaustion(float amount) {
            this.exhaustion = amount;
            return this;
        }

        /**
         * @param how the damage scales with difficulty
         * @return this builder
         */
        public Builder scaling(Scaling how) {
            this.scaling = how;
            return this;
        }

        /**
         * @param what the damage looks and sounds like
         * @return this builder
         */
        public Builder effects(Effects what) {
            this.effects = what;
            return this;
        }

        /** Writes the damage type. */
        public void save() {
            StringBuilder json = new StringBuilder("{\n");
            if (effects != null) {
                json.append("  \"effects\": \"").append(effects.id).append("\",\n");
            }
            json.append("  \"exhaustion\": ").append(EmberOutput.decimal(exhaustion)).append(",\n")
                    .append("  \"message_id\": ").append(EmberOutput.quote(message)).append(",\n")
                    .append("  \"scaling\": \"").append(scaling.id).append("\"\n")
                    .append("}\n");
            provider.save(name, json.toString());
        }
    }
}
