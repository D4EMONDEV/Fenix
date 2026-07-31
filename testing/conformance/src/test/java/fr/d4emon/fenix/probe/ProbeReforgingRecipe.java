package fr.d4emon.fenix.probe;

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
 * A recipe of a type the mod invented, for the conformance check.
 *
 * <p>Mirrors vanilla's {@code StonecutterRecipe}: one input, one output, built
 * on {@code SingleItemRecipe}, which is the shape a mod's custom recipe usually
 * wants. Its type and serializer are the mod's own — that is the whole point.
 */
public final class ProbeReforgingRecipe extends SingleItemRecipe {

    public static final MapCodec<ProbeReforgingRecipe> MAP_CODEC = simpleMapCodec(ProbeReforgingRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, ProbeReforgingRecipe> STREAM_CODEC =
            simpleStreamCodec(ProbeReforgingRecipe::new);

    public ProbeReforgingRecipe(Recipe.CommonInfo commonInfo, Ingredient input, ItemStackTemplate result) {
        super(commonInfo, input, result);
    }

    @Override
    public RecipeType<ProbeReforgingRecipe> getType() {
        return ProbeContent.REFORGING_TYPE.get();
    }

    @Override
    public RecipeSerializer<ProbeReforgingRecipe> getSerializer() {
        return ProbeContent.REFORGING_SERIALIZER.get();
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        // A custom station is not the vanilla recipe book, so this is never
        // shown; it still has to be a real category.
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
