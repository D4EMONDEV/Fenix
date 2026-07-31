package fr.d4emon.fenix.example.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;

/**
 * The recipe the ruby reforging table runs: one input, one output.
 *
 * <p>A recipe type of the mod's own. Built on vanilla's {@code SingleItemRecipe}
 * — the same base the stonecutter uses — so the ingredient, the result and their
 * codecs come for free, and this class only has to say which type and serializer
 * it belongs to. Those two are the mod's, registered in {@link ModContent}, and
 * naming them is the whole point of a custom recipe: the reforging table looks
 * recipes up by {@link #getType()}, so a recipe carrying any other type is one
 * it never finds.
 *
 * <p>The recipes themselves are datapack JSON, under
 * {@code data/example-mod/recipe/}. This class is only how that JSON is read.
 */
public final class RubyReforgingRecipe extends SingleItemRecipe {

    /** How the JSON is read; also the codec the conformance suite round-trips. */
    public static final MapCodec<RubyReforgingRecipe> MAP_CODEC = simpleMapCodec(RubyReforgingRecipe::new);

    /** How a recipe travels to the client on join. */
    public static final StreamCodec<RegistryFriendlyByteBuf, RubyReforgingRecipe> STREAM_CODEC =
            simpleStreamCodec(RubyReforgingRecipe::new);

    /**
     * @param commonInfo the shared recipe fields (whether it notifies)
     * @param input      what goes in
     * @param result     what comes out
     */
    public RubyReforgingRecipe(Recipe.CommonInfo commonInfo, Ingredient input, ItemStackTemplate result) {
        super(commonInfo, input, result);
    }

    @Override
    public RecipeType<RubyReforgingRecipe> getType() {
        return ModContent.REFORGING_TYPE.get();
    }

    @Override
    public RecipeSerializer<RubyReforgingRecipe> getSerializer() {
        return ModContent.REFORGING_SERIALIZER.get();
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        // The reforging table is not the vanilla recipe book, so this is never
        // shown — but it still has to be a real category.
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
