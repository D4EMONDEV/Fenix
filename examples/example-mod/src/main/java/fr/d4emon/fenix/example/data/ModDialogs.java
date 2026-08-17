package fr.d4emon.fenix.example.data;

import fr.d4emon.fenix.ember.EmberDialogProvider;
import fr.d4emon.fenix.ember.Generator;

/**
 * A screen the server can open without the client knowing about it.
 *
 * <p>Everything else this demo draws is client code — a screen class, a
 * renderer, a mixin. This is a datapack file, so it appears on a vanilla client
 * connected to a server running the mod.
 */
@Generator
public final class ModDialogs extends EmberDialogProvider {

    /** Instantiated by Ember from the compile-time index. */
    public ModDialogs() {
    }

    @Override
    protected void dialogs() {
        notice("shrine_found")
                .title("The Ruby Shrine")
                .body("Four pillars, weathered by whatever has been here longer than you.")
                .body("Something below still glitters.")
                .button("Onwards")
                .save();
    }
}
