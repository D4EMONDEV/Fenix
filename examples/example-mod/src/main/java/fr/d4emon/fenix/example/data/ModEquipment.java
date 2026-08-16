package fr.d4emon.fenix.example.data;

import fr.d4emon.fenix.ember.EmberEquipmentProvider;
import fr.d4emon.fenix.ember.Generator;

/**
 * What ruby armour looks like on whoever is wearing it.
 *
 * <p>The other half of armour. The material names this asset; this names the
 * textures. Miss it and the armour protects, wears down and is invisible.
 */
@Generator
public final class ModEquipment extends EmberEquipmentProvider {

    /** Instantiated by Ember from the compile-time index. */
    public ModEquipment() {
    }

    @Override
    protected void equipment() {
        humanoidArmor("ruby");
    }
}
