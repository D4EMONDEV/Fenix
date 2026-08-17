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
                // The mod's own curve rather than a straight line: strongest at
                // level 2, weaker at 1 and 3, so the best level is not simply
                // the highest one.
                .effect("minecraft:damage", """
                        {
                              "effect": {
                                "type": "example-mod:ruby_rising",
                                "peak": 2.0,
                                "strength": 2.5
                              }
                            }""")
                // And the mod's own effect, which no vanilla enchantment could
                // express: it takes the glimmer from whoever is hit and gives
                // it to whoever swung. Named here by id; the class behind that
                // id is registered in Java.
                .effect("minecraft:post_attack", """
                        {
                              "enchanted": "attacker",
                              "affected": "victim",
                              "effect": {
                                "type": "example-mod:ruby_drain",
                                "seconds": 4
                              }
                            }""")
                .save();
    }
}
