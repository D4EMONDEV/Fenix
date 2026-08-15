package fr.d4emon.fenix.example.data;

import fr.d4emon.fenix.ember.EmberDamageTypeProvider;
import fr.d4emon.fenix.ember.Generator;

/**
 * The mod's own kinds of hurt.
 *
 * <p>Each needs a line in the language file under {@code death.attack.<id>},
 * or a player killed by it gets a blank death message.
 */
@Generator
public final class ModDamageTypes extends EmberDamageTypeProvider {

    /** Instantiated by Ember from the compile-time index. */
    public ModDamageTypes() {
    }

    @Override
    protected void damageTypes() {
        // Standing on a ruby block that is glowing: environmental, so it
        // scales the way the game's own environmental damage does.
        damageType("ruby_burn")
                .exhaustion(0.1f)
                .effects(Effects.BURNING)
                .save();

        // A shard thrown by something. No exhaustion: the player did not
        // choose to be hit by it.
        damageType("ruby_shard")
                .scaling(Scaling.ALWAYS)
                .effects(Effects.POKING)
                .save();
    }
}
