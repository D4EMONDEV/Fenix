package fr.d4emon.fenix.example.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What the ruby safe holds.
 *
 * <p>Extends {@code BaseContainerBlockEntity} rather than implementing
 * {@code Container} by hand: vanilla's base already answers the fifteen
 * questions a container gets asked, and it is also a {@code MenuProvider}, so
 * the block can hand it straight to {@code Player.openMenu}.
 */
public final class RubySafeBlockEntity extends BaseContainerBlockEntity {

    /** Three rows of nine, the shape the screen draws. */
    public static final int SIZE = 27;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    /**
     * @param pos   where the block is
     * @param state what the block is
     */
    public RubySafeBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.RUBY_SAFE_ENTITY.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.example-mod.ruby_safe");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> replacement) {
        items = replacement;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        // The server's side of the window. The client builds its own from the
        // registered type, with an empty container that the sync then fills.
        return new RubySafeMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }
}
