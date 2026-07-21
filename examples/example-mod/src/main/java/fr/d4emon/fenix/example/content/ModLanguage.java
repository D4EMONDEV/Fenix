package fr.d4emon.fenix.example.content;

import fr.d4emon.fenix.ember.EmberLanguageProvider;
import fr.d4emon.fenix.ember.Generator;

/** What this mod's content is called, in English. */
@Generator
public final class ModLanguage extends EmberLanguageProvider {

    /** Instantiated by Ember from the compile-time index. */
    public ModLanguage() {
    }

    @Override
    protected void translations() {
        add(ModBlocks.RUBY_BLOCK, "Ruby Block");
        add(ModBlocks.GLOWING_RUBY_BLOCK, "Glowing Ruby Block");

        add(ModItems.RUBY, "Ruby");
        add(ModItems.RUBY_HAMMER, "Ruby Hammer");

        // The mod's creative tab. Untranslated it shows as its raw key, which
        // is the sort of thing that ships by accident.
        add("itemGroup.example-mod.example_mod", "Example Mod");
    }
}
