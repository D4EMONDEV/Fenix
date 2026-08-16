package fr.d4emon.fenix.registry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Describes an item, then registers it.
 *
 * <pre>{@code
 * public static final Holder<Item> RUBY = REGISTRAR.newItem("ruby")
 *         .stacksTo(16)
 *         .rarity(Rarity.UNCOMMON)
 *         .register();
 * }</pre>
 *
 * <p>Anything not covered here is reachable through
 * {@link #properties(UnaryOperator)}, which hands you vanilla's own builder.
 */
public final class ItemBuilder {

    private final Registrar registrar;
    private final String name;

    private UnaryOperator<Item.Properties> properties = UnaryOperator.identity();
    private Function<Item.Properties, Item> factory = Item::new;

    ItemBuilder(Registrar registrar, String name) {
        this.registrar = registrar;
        this.name = name;
    }

    /**
     * Sets the most that fits in one stack.
     *
     * @param size 1 to 99
     * @return this builder
     */
    public ItemBuilder stacksTo(int size) {
        return properties(props -> props.stacksTo(size));
    }

    /**
     * Makes it a tool with durability, which cannot stack.
     *
     * @param uses how many uses before it breaks
     * @return this builder
     */
    public ItemBuilder durability(int uses) {
        return properties(props -> props.durability(uses));
    }

    /**
     * Sets the colour its name is shown in.
     *
     * @param rarity the rarity
     * @return this builder
     */
    public ItemBuilder rarity(Rarity rarity) {
        return properties(props -> props.rarity(rarity));
    }

    /**
     * Survives fire and lava, like netherite.
     *
     * @return this builder
     */
    public ItemBuilder fireResistant() {
        return properties(Item.Properties::fireResistant);
    }

    /**
     * Applies anything else vanilla's builder offers.
     *
     * @param step what to do to the properties
     * @return this builder
     */
    /**
     * Makes this item a music disc, playing one of the mod's own songs.
     *
     * <p>The song is data — {@code EmberCosmeticsProvider} writes it — and this
     * names it by key rather than by value, because the datapack registry it
     * lives in is not loaded when items are registered. A disc whose song was
     * never written is a disc that goes into a jukebox and plays silence.
     *
     * @param name the song's path, in this mod's namespace
     * @return this builder
     */
    public ItemBuilder jukeboxSong(String name) {
        return properties(properties -> properties.jukeboxPlayable(
                ResourceKey.create(Registries.JUKEBOX_SONG, registrar.identifier(name))));
    }

    /**
     * Makes this item a piece of armour.
     *
     * <p>Sets the protection, the durability and the slot in one go, from the
     * material. What it does not do is make it visible: that is the equipment
     * asset the material names, which Ember writes.
     *
     * @param material what the set is made of
     * @param type     which piece this is
     * @return this builder
     */
    public ItemBuilder armor(net.minecraft.world.item.equipment.ArmorMaterial material,
                             net.minecraft.world.item.equipment.ArmorType type) {
        return properties(properties -> properties.humanoidArmor(material, type));
    }

    public ItemBuilder properties(UnaryOperator<Item.Properties> step) {
        Objects.requireNonNull(step, "step");
        UnaryOperator<Item.Properties> previous = properties;
        properties = props -> step.apply(previous.apply(props));
        return this;
    }

    /**
     * Builds a subclass instead of a plain {@link Item}.
     *
     * @param itemFactory builds the item from the finished properties
     * @return this builder
     */
    public ItemBuilder from(Function<Item.Properties, Item> itemFactory) {
        this.factory = Objects.requireNonNull(itemFactory, "itemFactory");
        return this;
    }

    /**
     * Declares the item. Registration itself happens at {@code apply()}.
     *
     * @return a handle on the item
     */
    public Holder<Item> register() {
        return registrar.item(name, props -> factory.apply(properties.apply(props)));
    }
}
