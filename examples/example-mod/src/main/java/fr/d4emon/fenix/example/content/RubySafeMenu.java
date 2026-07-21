package fr.d4emon.fenix.example.content;

import fr.d4emon.fenix.registry.SimpleMenu;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

/**
 * The ruby safe's window: three rows of nine above the player's inventory.
 *
 * <p>Two constructors, and the difference between them is the whole
 * client-server story. The server builds one over the block's real contents;
 * the client is only told a window opened, so it builds one over an empty
 * container of the right size and lets the sync fill it in. That is what
 * vanilla's chests do too.
 */
public final class RubySafeMenu extends SimpleMenu {

    /** Built by the client, from the registered menu type. */
    public RubySafeMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(RubySafeBlockEntity.SIZE));
    }

    /**
     * Built by the server, over what the block actually holds.
     *
     * @param id        the window id
     * @param inventory the player's inventory
     * @param contents  the block's contents
     */
    public RubySafeMenu(int id, Inventory inventory, Container contents) {
        super(ModContent.RUBY_SAFE_MENU.get(), id, inventory, contents, 9, 3);
    }
}
