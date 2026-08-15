package fr.d4emon.fenix.example.data;

import fr.d4emon.fenix.ember.EmberSoundProvider;
import fr.d4emon.fenix.ember.Generator;
import fr.d4emon.fenix.example.registry.ModContent;

/**
 * Which ogg files each sound event plays.
 *
 * <p>Registering a sound event and writing this file are two separate things,
 * and the second is the one that makes noise. An event with no entry here is
 * silence with nothing in the log.
 */
@Generator
public final class ModSounds extends EmberSoundProvider {

    /** Instantiated by Ember from the compile-time index. */
    public ModSounds() {
    }

    @Override
    protected void sounds() {
        // The ogg itself is not shipped: this demo has no audio, and the point
        // being shown is the wiring. In a real mod the file would sit at
        // assets/example-mod/sounds/ruby_chime.ogg.
        add(ModContent.RUBY_CHIME, "ruby_chime");
    }
}
