package fr.d4emon.fenix.example.data;

import fr.d4emon.fenix.ember.EmberTradeProvider;
import fr.d4emon.fenix.ember.Generator;
import fr.d4emon.fenix.example.registry.ModItems;

/**
 * What the jeweller offers.
 *
 * <p>These three files used to be written by hand under {@code resources}.
 * Nothing was wrong with them, which is the point: they are exactly the kind of
 * small, correct, unchecked JSON that stops being correct the first time
 * somebody edits it.
 */
@Generator
public final class ModTrades extends EmberTradeProvider {

    /** Instantiated by Ember from the compile-time index. */
    public ModTrades() {
    }

    @Override
    protected void trades() {
        trade("buy_ruby")
                .wants("minecraft:emerald", 1)
                .gives(ModItems.RUBY)
                .maxUses(12)
                .xp(2)
                .save();

        trade("sell_ruby")
                .wants(ModItems.RUBY, 1)
                .gives("minecraft:emerald")
                .maxUses(12)
                .xp(2)
                .reputationDiscount(0.05f)
                .save();

        tradeSet("jeweller")
                .amount(2)
                .trades("buy_ruby", "sell_ruby")
                .save();
    }
}
