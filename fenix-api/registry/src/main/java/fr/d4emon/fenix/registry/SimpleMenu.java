package fr.d4emon.fenix.registry;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * A chest-shaped menu: a grid of slots above the player's inventory.
 *
 * <pre>{@code
 * public final class SafeMenu extends SimpleMenu {
 *     public SafeMenu(int id, Inventory player, Container safe) {
 *         super(ModContent.SAFE_MENU.get(), id, player, safe, 9, 3);
 *     }
 * }
 * }</pre>
 *
 * <p>Extending {@code AbstractContainerMenu} directly means writing
 * {@code quickMoveStack} — shift-clicking — and that method is the single most
 * copied-and-broken piece of code in Minecraft modding. The usual version moves
 * a stack into the wrong half, or loops forever, or silently deletes items when
 * the destination is full. It is hard to get right because it needs to know
 * which slots belong to whom, and a mod that lays out its own slots is the only
 * thing that knows.
 *
 * <p>So this class lays them out, and gets the method right once.
 */
public abstract class SimpleMenu extends AbstractContainerMenu {

    /** Vanilla's spacing, which players' muscle memory already expects. */
    private static final int SLOT = 18;

    private final Container container;
    private final int containerSlots;

    /**
     * Lays the slots out and wires the menu up.
     *
     * @param type      this menu's registered type
     * @param id        the id the server gave this window
     * @param inventory the player's inventory
     * @param container what the menu is showing
     * @param columns   how wide the grid is
     * @param rows      how tall
     * @throws IllegalArgumentException if the container is smaller than the grid
     */
    // addSlot is public and called from here, which javac notes as a `this`
    // escape. Every vanilla menu does the same, and the alternative is a
    // half-built menu that a subclass has to finish -- worse in every way that
    // matters.
    @SuppressWarnings("this-escape")
    protected SimpleMenu(MenuType<?> type, int id, Inventory inventory, Container container,
                         int columns, int rows) {
        super(type, id);
        this.container = Objects.requireNonNull(container, "container");
        this.containerSlots = columns * rows;

        if (container.getContainerSize() < containerSlots) {
            throw new IllegalArgumentException("the container has " + container.getContainerSize()
                    + " slots but the menu shows " + containerSlots
                    + " — the extra ones would be unreachable and their contents lost");
        }

        // The container first, then the player: quickMoveStack below relies on
        // that order, and so does every vanilla menu.
        int top = 18;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                addSlot(new Slot(container, row * columns + column,
                        8 + column * SLOT, top + row * SLOT));
            }
        }

        int inventoryTop = top + rows * SLOT + 13;
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

    /**
     * Moves a stack between the menu and the player's inventory.
     *
     * <p>Written once, correctly. The contract is unusual and is what trips
     * every hand-written version: return the stack <em>as it was</em>, and an
     * empty stack once nothing is left, or the game loops asking again.
     *
     * @param player who shift-clicked
     * @param index  the slot they clicked
     * @return what was in the slot before the move, or empty when nothing moved
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();

        boolean fromContainer = index < containerSlots;
        boolean moved = fromContainer
                ? moveItemStackTo(stack, containerSlots, slots.size(), true)
                : moveItemStackTo(stack, 0, containerSlots, false);
        if (!moved) {
            // Nothing moved, so nothing changed. Answering with the stack would
            // have the game call this again with the same result, forever.
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
