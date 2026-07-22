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
        add(ModBlocks.RUBY_TALLY, "Ruby Tally");
        add(ModBlocks.RUBY_SAFE, "Ruby Safe");
        add(ModBlocks.RUBY_ORE, "Ruby Ore");
        add(ModBlocks.DEEPSLATE_RUBY_ORE, "Deepslate Ruby Ore");

        // The title above the safe's slots. A raw key, because the name
        // belongs to the menu rather than to any one piece of content.
        add("container.example-mod.ruby_safe", "Ruby Safe");

        add(ModItems.RUBY, "Ruby");
        add(ModItems.RUBY_HAMMER, "Ruby Hammer");
        add(ModContent.RUBY_WISP_SPAWN_EGG, "Ruby Wisp Spawn Egg");

        // The key binding, and the group it sits in. Without these the controls
        // screen shows the raw translation key.
        add("key.example-mod.count_wisps", "Count nearby wisps");
        add("key.category.example-mod.example_mod", "Example Mod");

        // The key comes from the tab, so renaming it cannot leave a stale
        // translation behind.
        add(ModContent.TAB, "Example Mod");
    }
}
