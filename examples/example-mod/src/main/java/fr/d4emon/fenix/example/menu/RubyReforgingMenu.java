package fr.d4emon.fenix.example.menu;

import fr.d4emon.fenix.example.block.entity.RubyReforgingBlockEntity;
import fr.d4emon.fenix.example.registry.ModContent;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The reforging table's window: an input slot, an output slot, and the player's
 * inventory.
 *
 * <p>Not a {@link fr.d4emon.fenix.registry.SimpleMenu}, because that lays out a
 * grid where every slot behaves the same, and this needs a slot the player can
 * put things into and one they can only take from. Two slots is little enough
 * that {@code quickMoveStack} is written out here — the one thing
 * {@code SimpleMenu} exists to spare a mod, done by hand where the layout is its
 * own.
 */
public final class RubyReforgingMenu extends AbstractContainerMenu {

    private static final int SLOT = 18;
    private static final int CONTAINER_SLOTS = RubyReforgingBlockEntity.SIZE;

    private final Container container;

    /** Built by the client, over an empty container the sync fills. */
    public RubyReforgingMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(RubyReforgingBlockEntity.SIZE));
    }

    /**
     * Built by the server, over the table's real contents.
     *
     * @param id        the window id
     * @param inventory the player's inventory
     * @param container the table's two slots
     */
    @SuppressWarnings("this-escape")
    public RubyReforgingMenu(int id, Inventory inventory, Container container) {
        super(ModContent.RUBY_REFORGING_MENU.get(), id);
        this.container = container;

        // Positioned to line up with the furnace panel the screen borrows.
        addSlot(new Slot(container, RubyReforgingBlockEntity.INPUT, 56, 17));
        addSlot(new Slot(container, RubyReforgingBlockEntity.OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // Take-only: the reforging fills this, the player empties it.
                return false;
            }
        });

        int inventoryTop = 84;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        8 + column * SLOT, inventoryTop + row * SLOT));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * SLOT, inventoryTop + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();

        boolean fromTable = index < CONTAINER_SLOTS;
        boolean moved;
        if (fromTable) {
            // Out of the table, into the player's inventory.
            moved = moveItemStackTo(stack, CONTAINER_SLOTS, slots.size(), true);
        } else {
            // Into the input slot only — never the output, which is take-only.
            moved = moveItemStackTo(stack, RubyReforgingBlockEntity.INPUT,
                    RubyReforgingBlockEntity.INPUT + 1, false);
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return before;
    }
}
