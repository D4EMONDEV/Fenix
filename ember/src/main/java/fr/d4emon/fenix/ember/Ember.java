package fr.d4emon.fenix.ember;

import fr.d4emon.fenix.registry.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Writes a mod's resource files.
 *
 * <p>Handed to an {@link EmberGenerator} at build time. Content is passed as
 * the {@link Holder} it was registered with, so a name is never repeated as a
 * string that could drift — rename the block and its model, blockstate and
 * translation follow.
 *
 * <p>Every method here describes an <em>intent</em> ("this is a cube with one
 * texture") rather than a file. Which files that turns into, and in which
 * format, is Minecraft's business and changes between versions; that is the
 * point of generating them.
 */
public interface Ember {

    /**
     * {@return the mod everything is being written for}
     */
    String modId();

    /**
     * A solid cube with the same texture on every face — the common case.
     *
     * <p>Writes the block model, the blockstate, and the model definition for
     * the item that places it. Expects a texture at
     * {@code assets/<mod>/textures/block/<name>.png}, which is the one thing
     * that cannot be generated.
     *
     * @param block the block
     */
    void cubeAll(Holder<Block> block);

    /**
     * A solid cube, and the name shown for it — the whole of a simple block in
     * one line.
     *
     * @param block   the block
     * @param english what to call it
     */
    void cubeAll(Holder<Block> block, String english);

    /**
     * A flat item, drawn from a single texture, like most crafting materials.
     *
     * <p>Expects a texture at {@code assets/<mod>/textures/item/<name>.png}.
     *
     * @param item the item
     */
    void flatItem(Holder<Item> item);

    /**
     * A flat item, and the name shown for it.
     *
     * @param item    the item
     * @param english what to call it
     */
    void flatItem(Holder<Item> item, String english);

    /**
     * The English name shown for a block.
     *
     * <p>Named differently from {@link #itemName} because generics erase: the
     * two would otherwise be the same method.
     *
     * @param block   the block
     * @param english what to call it
     */
    void blockName(Holder<Block> block, String english);

    /**
     * The English name shown for an item.
     *
     * @param item    the item
     * @param english what to call it
     */
    void itemName(Holder<Item> item, String english);

    /**
     * Any other translation.
     *
     * @param key     the translation key
     * @param english what it reads
     */
    void translate(String key, String english);
}
