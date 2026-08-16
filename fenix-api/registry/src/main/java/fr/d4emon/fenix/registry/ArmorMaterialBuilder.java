package fr.d4emon.fenix.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Collects what a set of armour is.
 *
 * <p>Every field has the answer iron gives, so a mod naming only what differs
 * gets something sensible rather than something broken. The one field with no
 * safe default is the protection: armour that protects nothing is armour a
 * player will wear once.
 *
 * @see Registrar#armorMaterial(String)
 */
public final class ArmorMaterialBuilder {

    private final Registrar registrar;
    private final String name;
    private final Map<ArmorType, Integer> protection = new EnumMap<>(ArmorType.class);

    private int durability = 15;
    private int enchantmentValue = 9;
    private Holder<SoundEvent> equipSound;
    private float toughness;
    private float knockbackResistance;
    private TagKey<Item> repairedWith = ItemTags.IRON_TOOL_MATERIALS;

    ArmorMaterialBuilder(Registrar registrar, String name) {
        this.registrar = registrar;
        this.name = name;
    }

    /**
     * The multiplier on each piece's durability.
     *
     * <p>Not a number of uses: the game multiplies this by a per-piece figure,
     * so 15 is iron and 33 is diamond.
     *
     * @param factor the multiplier
     * @return this builder
     */
    public ArmorMaterialBuilder durability(int factor) {
        this.durability = factor;
        return this;
    }

    /**
     * How much one piece protects.
     *
     * <p>Required. A material with no protection at all is armour that does
     * nothing, and nothing says so.
     *
     * @param type   which piece
     * @param points the armour points it gives
     * @return this builder
     */
    public ArmorMaterialBuilder protection(ArmorType type, int points) {
        protection.put(type, points);
        return this;
    }

    /**
     * @param value how well it takes enchantments; iron is 9, gold 25
     * @return this builder
     */
    public ArmorMaterialBuilder enchantmentValue(int value) {
        this.enchantmentValue = value;
        return this;
    }

    /**
     * @param sound what putting it on sounds like
     * @return this builder
     */
    public ArmorMaterialBuilder equipSound(Holder<SoundEvent> sound) {
        this.equipSound = sound;
        return this;
    }

    /**
     * @param value how much damage it shrugs off beyond the points; diamond
     *              is 2
     * @return this builder
     */
    public ArmorMaterialBuilder toughness(float value) {
        this.toughness = value;
        return this;
    }

    /**
     * @param value how much knockback it resists, 0 to 1; netherite is 0.1
     * @return this builder
     */
    public ArmorMaterialBuilder knockbackResistance(float value) {
        this.knockbackResistance = value;
        return this;
    }

    /**
     * @param tag what mends it on an anvil
     * @return this builder
     */
    public ArmorMaterialBuilder repairedWith(TagKey<Item> tag) {
        this.repairedWith = tag;
        return this;
    }

    /**
     * {@return the material, ready to hand to {@link ItemBuilder#armor}}
     *
     * @throws IllegalStateException if no piece protects anything
     */
    public ArmorMaterial build() {
        if (protection.isEmpty()) {
            throw new IllegalStateException(
                    name + " protects nothing, so it is armour that does nothing");
        }

        // The equip sound is a registered value, and the default is vanilla's
        // own iron. Reading it here rather than in the field initialiser
        // matters: a field would resolve before the registries are open.
        net.minecraft.core.Holder<SoundEvent> sound = equipSound == null
                ? SoundEvents.ARMOR_EQUIP_IRON
                : BuiltInRegistries.SOUND_EVENT.wrapAsHolder(equipSound.get());

        return new ArmorMaterial(durability, Map.copyOf(protection), enchantmentValue,
                sound, toughness, knockbackResistance, repairedWith,
                ResourceKey.create(EquipmentAssets.ROOT_ID, registrar.identifier(name)));
    }
}
