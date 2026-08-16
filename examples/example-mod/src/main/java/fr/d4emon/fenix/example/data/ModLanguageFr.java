package fr.d4emon.fenix.example.data;

import fr.d4emon.fenix.example.registry.ModBlocks;
import fr.d4emon.fenix.example.registry.ModContent;
import fr.d4emon.fenix.example.registry.ModItems;

import fr.d4emon.fenix.ember.EmberLanguageProvider;
import fr.d4emon.fenix.ember.Generator;

/**
 * The same content, in French.
 *
 * <p>A second generator rather than a second method on the first: each one
 * writes one file, and a language a mod drops is a class it deletes.
 *
 * <p>The keys have to match {@link ModLanguage}'s exactly. They do here because
 * both ask the content for its own key rather than writing one out — which is
 * the difference between a translation that follows a rename and one that
 * quietly stops applying.
 */
@Generator
public final class ModLanguageFr extends EmberLanguageProvider {

    /** Instantiated by Ember from the compile-time index. */
    public ModLanguageFr() {
        super("fr_fr");
    }

    @Override
    protected void translations() {
        add(ModBlocks.RUBY_BLOCK, "Bloc de rubis");
        add(ModBlocks.GLOWING_RUBY_BLOCK, "Bloc de rubis lumineux");
        add(ModBlocks.RUBY_TALLY, "Compteur de rubis");
        add(ModBlocks.RUBY_SAFE, "Coffre-fort de rubis");
        add(ModBlocks.RUBY_REFORGING, "Table de reforge");
        add(ModBlocks.RUBY_LOG, "Bûche de rubis");
        add(ModBlocks.STRIPPED_RUBY_LOG, "Bûche de rubis écorcée");
        add(ModBlocks.RUBY_ORE, "Minerai de rubis");
        add(ModBlocks.DEEPSLATE_RUBY_ORE, "Minerai de rubis de l'ardoise des abîmes");

        add("container.example-mod.ruby_safe", "Coffre-fort de rubis");
        add("container.example-mod.ruby_reforging", "Table de reforge");
        add("entity.example-mod.villager.jeweller", "Joaillier");

        add(ModItems.RUBY, "Rubis");
        add(ModItems.RUBY_HAMMER, "Marteau de rubis");
        add(ModItems.RUBY_DISC, "Disque");
        add(ModItems.RUBY_HELMET, "Casque de rubis");
        add(ModItems.RUBY_CHESTPLATE, "Plastron de rubis");
        add(ModItems.RUBY_LEGGINGS, "Jambières de rubis");
        add(ModItems.RUBY_BOOTS, "Bottes de rubis");
        add(ModContent.RUBY_WISP_SPAWN_EGG, "Œuf d'apparition de feu follet de rubis");
        add(ModContent.RUBY_SPRITE_SPAWN_EGG, "Œuf d'apparition d'esprit de rubis");
        add(ModContent.RUBY_BRINE.bucket().orElseThrow(), "Seau de saumure de rubis");

        add("key.example-mod.count_wisps", "Compter les feux follets proches");
        add("key.category.example-mod.example_mod", "Example Mod");

        add(ModContent.RUBY_GLIMMER, "Lueur de rubis");
        add("message.example-mod.glimmer", "Le marteau luit au coup %s — %s coups en tout.");
        add("tooltip.example-mod.ruby", "Tiède au toucher.");
        // A damage type with no death message kills players silently.
        add("death.attack.ruby_burn", "%s Brûlé par l'éclat du rubis");
        add("death.attack.ruby_shard", "%s Transpercé par un éclat de rubis");
        add("message.example-mod.too_bright", "Ce bloc est trop lumineux pour être cassé à la main.");

        add("item.minecraft.potion.effect.glimmering", "Potion de lueur");
        add("item.minecraft.splash_potion.effect.glimmering", "Potion jetable de lueur");
        add("item.minecraft.lingering_potion.effect.glimmering", "Potion persistante de lueur");
        add("item.minecraft.tipped_arrow.effect.glimmering", "Flèche de lueur");

        add(ModContent.TAB, "Example Mod");
    }
}
