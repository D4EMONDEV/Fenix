package fr.d4emon.fenix.example.registry;

import fr.d4emon.fenix.example.item.RubyHammer;

import fr.d4emon.fenix.registry.Holder;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
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
    /**
     * A music disc playing the mod's own song.
     *
     * <p>The song is a data file; this is the item that carries it. Either
     * without the other is silent: a song nothing plays, or a disc that plays
     * nothing.
     */
    /**
     * What ruby armour is made of.
     *
     * <p>A value rather than a registration. The asset id it carries points at
     * a file ModEquipment writes; without that file the armour protects
     * perfectly and cannot be seen on the wearer.
     */
    public static final ArmorMaterial RUBY_ARMOR = ModContent.REGISTRAR.armorMaterial("ruby")
            .durability(22)
            .protection(ArmorType.HELMET, 3)
            .protection(ArmorType.CHESTPLATE, 7)
            .protection(ArmorType.LEGGINGS, 5)
            .protection(ArmorType.BOOTS, 3)
            .enchantmentValue(12)
            .toughness(1.5f)
            .knockbackResistance(0.05f)
            .build();

    public static final Holder<Item> RUBY_HELMET = ModContent.REGISTRAR.newItem("ruby_helmet")
            .stacksTo(1)
            .armor(RUBY_ARMOR, ArmorType.HELMET)
            .register();

    public static final Holder<Item> RUBY_CHESTPLATE =
            ModContent.REGISTRAR.newItem("ruby_chestplate")
                    .stacksTo(1)
                    .armor(RUBY_ARMOR, ArmorType.CHESTPLATE)
                    .register();

    public static final Holder<Item> RUBY_LEGGINGS =
            ModContent.REGISTRAR.newItem("ruby_leggings")
                    .stacksTo(1)
                    .armor(RUBY_ARMOR, ArmorType.LEGGINGS)
                    .register();

    public static final Holder<Item> RUBY_BOOTS = ModContent.REGISTRAR.newItem("ruby_boots")
            .stacksTo(1)
            .armor(RUBY_ARMOR, ArmorType.BOOTS)
            .register();

    public static final Holder<Item> RUBY_DISC = ModContent.REGISTRAR.newItem("ruby_disc")
            .stacksTo(1)
            .rarity(net.minecraft.world.item.Rarity.RARE)
            .jukeboxSong("ruby_waltz")
            .register();

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
