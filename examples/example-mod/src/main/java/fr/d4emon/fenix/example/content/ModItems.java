package fr.d4emon.fenix.example.content;

import fr.d4emon.fenix.registry.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * The mod's items.
 *
 * <p>The items that place {@link ModBlocks}' blocks are not declared here —
 * {@code withItem()} on a block registers its own, so a block and the item that
 * places it can never drift apart.
 */
public final class ModItems {

    /** A plain crafting material. */
    public static final Holder<Item> RUBY = ModContent.REGISTRAR.newItem("ruby")
            .rarity(Rarity.UNCOMMON)
            .register();

    /** Something that does not stack, to show the difference. */
    public static final Holder<Item> RUBY_HAMMER = ModContent.REGISTRAR.newItem("ruby_hammer")
            .durability(250)
            .rarity(Rarity.RARE)
            .from(RubyHammer::new)
            .register();

    private ModItems() {
    }

    /** Loads this class, which is what runs the declarations above. */
    static void load() {
    }
}
