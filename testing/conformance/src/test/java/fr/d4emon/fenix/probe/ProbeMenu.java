package fr.d4emon.fenix.probe;

import fr.d4emon.fenix.registry.SimpleMenu;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

/** A menu, so that registering one is exercised through the real loader. */
public final class ProbeMenu extends SimpleMenu {

    /** Built by the client, from the registered type. */
    public ProbeMenu(int id, Inventory inventory) {
        super(ProbeContent.CHEST_MENU.get(), id, inventory, new SimpleContainer(27), 9, 3);
    }
}
