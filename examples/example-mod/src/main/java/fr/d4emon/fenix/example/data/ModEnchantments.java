package fr.d4emon.fenix.example.data;

import fr.d4emon.fenix.ember.EmberEnchantmentProvider;
import fr.d4emon.fenix.ember.Generator;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;

/**
 * The mod's enchantments.
 *
 * <p>There is no {@code Registrar.enchantment}: since 1.21 an enchantment is
 * data, so shipping this file is the whole of adding one.
 */
@Generator
public final class ModEnchantments extends EmberEnchantmentProvider {

    /** Instantiated by Ember from the compile-time index. */
    public ModEnchantments() {
    }

    @Override
    protected void enchantments() {
        // Sharpness's own shape, with the numbers turned down. In the damage
        // exclusive set, so it cannot be stacked with Sharpness itself — which
        // is the difference between an enchantment and a straight upgrade.
        enchantment("ruby_edge")
                .description("Ruby Edge")
                .supports(ItemTags.SWORDS)
                .primary(ItemTags.SWORDS)
                .exclusiveWith(EnchantmentTags.DAMAGE_EXCLUSIVE)
                .slots(Slot.MAINHAND)
                .maxLevel(3)
                .weight(4)
                .anvilCost(2)
                .cost(4, 9, 24, 9)
                .addsDamage(0.5f, 0.5f)
                .save();
    }
}
