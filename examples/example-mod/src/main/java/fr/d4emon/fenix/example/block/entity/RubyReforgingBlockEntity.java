package fr.d4emon.fenix.example.block.entity;

import fr.d4emon.fenix.example.menu.RubyReforgingMenu;
import fr.d4emon.fenix.example.recipe.RubyReforgingRecipe;
import fr.d4emon.fenix.example.registry.ModContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;

/**
 * A little furnace-shaped machine that runs a recipe of the mod's own type.
 *
 * <p>Two slots — an input and an output — and a timer. Every tick it asks the
 * recipe manager whether the input is a {@link RubyReforgingRecipe}, and after a
 * few seconds turns one input into one output. The asking is the whole demo:
 * {@code getRecipeFor} with the mod's own {@link ModContent#REFORGING_TYPE} is
 * how a custom station finds a custom recipe, and it works because the type, the
 * serializer and the recipe were all registered.
 */
public final class RubyReforgingBlockEntity extends BaseContainerBlockEntity {

    /** The slot an item to reforge goes in. */
    public static final int INPUT = 0;
    /** The slot the reforged item comes out of. */
    public static final int OUTPUT = 1;
    /** Two slots: input and output. */
    public static final int SIZE = 2;

    /** Three seconds at twenty ticks a second — long enough to watch happen. */
    private static final int REFORGE_TICKS = 60;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int progress;

    /**
     * @param pos   where the block is
     * @param state what the block is
     */
    public RubyReforgingBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.RUBY_REFORGING_ENTITY.get(), pos, state);
    }

    /**
     * The server's tick: reforge the input once the timer fills, if a recipe
     * matches and the output has room.
     *
     * @param level the level, always a server level here — the block only ticks
     *              on the server
     * @param pos   where the block is
     * @param state what the block is
     * @param be    the block entity
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  RubyReforgingBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        ItemStack input = be.items.get(INPUT);
        if (input.isEmpty()) {
            be.progress = 0;
            return;
        }

        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Optional<RecipeHolder<RubyReforgingRecipe>> recipe = server.recipeAccess()
                .getRecipeFor(ModContent.REFORGING_TYPE.get(), recipeInput, server);
        if (recipe.isEmpty()) {
            be.progress = 0;
            return;
        }

        ItemStack result = recipe.get().value().assemble(recipeInput);
        if (!be.fits(result)) {
            // A full output stalls the timer rather than losing the result: the
            // player empties the output and it picks up where it left off.
            be.progress = 0;
            return;
        }

        if (++be.progress >= REFORGE_TICKS) {
            be.progress = 0;
            be.produce(result);
            input.shrink(1);
            be.setChanged();
        }
    }

    /** {@return whether the output slot can take the result} */
    private boolean fits(ItemStack result) {
        ItemStack output = items.get(OUTPUT);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void produce(ItemStack result) {
        ItemStack output = items.get(OUTPUT);
        if (output.isEmpty()) {
            items.set(OUTPUT, result.copy());
        } else {
            output.grow(result.getCount());
        }
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.example-mod.ruby_reforging");
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
        return new RubyReforgingMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        progress = input.getIntOr("Progress", 0);
    }
}
