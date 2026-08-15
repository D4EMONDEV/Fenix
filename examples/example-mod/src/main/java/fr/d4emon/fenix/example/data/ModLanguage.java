package fr.d4emon.fenix.example.data;

import fr.d4emon.fenix.example.registry.ModBlocks;
import fr.d4emon.fenix.example.registry.ModContent;
import fr.d4emon.fenix.example.registry.ModItems;

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
        add("entity.example-mod.ruby_sprite", "Ruby Sprite");
        add("attribute.example-mod.ruby_charge", "Ruby Charge");
        add(ModBlocks.RUBY_SLAB, "Ruby Slab");
        add(ModBlocks.RUBY_FENCE, "Ruby Fence");
        add(ModBlocks.RUBY_WALL, "Ruby Wall");
        add(ModBlocks.RUBY_GATE, "Ruby Gate");
        add(ModBlocks.RUBY_STAIRS, "Ruby Stairs");
        add(ModBlocks.RUBY_TRAPDOOR, "Ruby Trapdoor");
        add(ModBlocks.RUBY_BUTTON, "Ruby Button");
        add(ModBlocks.RUBY_PLATE, "Ruby Pressure Plate");
        add(ModBlocks.RUBY_DOOR, "Ruby Door");
        add(ModBlocks.GLOWING_RUBY_BLOCK, "Glowing Ruby Block");
        add(ModBlocks.RUBY_TALLY, "Ruby Tally");
        add(ModBlocks.RUBY_SAFE, "Ruby Safe");
        add(ModBlocks.RUBY_REFORGING, "Ruby Reforging Table");
        add(ModBlocks.RUBY_LOG, "Ruby Log");
        add(ModBlocks.STRIPPED_RUBY_LOG, "Stripped Ruby Log");
        add(ModBlocks.RUBY_ORE, "Ruby Ore");
        add(ModBlocks.DEEPSLATE_RUBY_ORE, "Deepslate Ruby Ore");

        // The title above the safe's slots. A raw key, because the name
        // belongs to the menu rather than to any one piece of content.
        add("container.example-mod.ruby_safe", "Ruby Safe");
        add("container.example-mod.ruby_reforging", "Reforging Table");

        // The villager profession's name. The key is the one the registrar
        // builds — entity.<mod id>.villager.<name> — so it cannot drift.
        add("entity.example-mod.villager.jeweller", "Jeweller");

        add(ModItems.RUBY, "Ruby");
        add(ModItems.RUBY_HAMMER, "Ruby Hammer");
        add(ModContent.RUBY_WISP_SPAWN_EGG, "Ruby Wisp Spawn Egg");
        add(ModContent.RUBY_SPRITE_SPAWN_EGG, "Ruby Sprite Spawn Egg");
        add(ModContent.RUBY_BRINE.bucket().orElseThrow(), "Ruby Brine Bucket");

        // The key binding, and the group it sits in. Without these the controls
        // screen shows the raw translation key.
        add("key.example-mod.count_wisps", "Count nearby wisps");

        // The status effect, and the message the hammer sends. The first %s is
        // this hammer's swing count (a data component on the stack); the second
        // is the player's lifetime total (a persistent attachment on the player).
        add(ModContent.RUBY_GLIMMER, "Ruby Glimmer");

        // The tooltip line the client half adds under a ruby's name.
        add("tooltip.example-mod.ruby", "Warm to the touch.");
        add("message.example-mod.too_bright", "That block is too bright to break by hand.");

        // The potion's name follows vanilla's scheme for potion items, so the
        // key names Minecraft rather than this mod.
        add("item.minecraft.potion.effect.glimmering", "Potion of Glimmering");
        add("item.minecraft.splash_potion.effect.glimmering", "Splash Potion of Glimmering");
        add("item.minecraft.lingering_potion.effect.glimmering", "Lingering Potion of Glimmering");
        add("item.minecraft.tipped_arrow.effect.glimmering", "Arrow of Glimmering");
        add("message.example-mod.glimmer", "The hammer glimmers on swing %s — %s swung in all.");
        add("key.category.example-mod.example_mod", "Example Mod");

        // The key comes from the tab, so renaming it cannot leave a stale
        // translation behind.
        add(ModContent.TAB, "Example Mod");
    }
}
