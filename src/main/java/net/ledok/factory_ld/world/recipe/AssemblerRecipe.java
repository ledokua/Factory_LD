package net.ledok.factory_ld.world.recipe;

import java.util.List;

import net.ledok.factory_ld.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class AssemblerRecipe extends ConstructorRecipe {
    public AssemblerRecipe(
        String recipeName,
        String recipeNameKey,
        String category,
        String researchGroup,
        List<InputEntry> itemInputs,
        List<ItemStack> itemOutputs,
        int craftTime
    ) {
        super(recipeName, recipeNameKey, category, researchGroup, itemInputs, itemOutputs, craftTime);
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        if (input == null || input.size() < 2) {
            return false;
        }
        InputEntry in1 = getInputEntry(0);
        InputEntry in2 = getInputEntry(1);
        if (in1 == null || in2 == null) {
            return false;
        }
        return in1.ingredient().test(input.getItem(0)) && in2.ingredient().test(input.getItem(1));
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(2, Ingredient.EMPTY);
        InputEntry in1 = getInputEntry(0);
        InputEntry in2 = getInputEntry(1);
        ingredients.set(0, in1 == null ? Ingredient.EMPTY : in1.ingredient());
        ingredients.set(1, in2 == null ? Ingredient.EMPTY : in2.ingredient());
        return ingredients;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return getOutput();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ASSEMBLER_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ASSEMBLER_TYPE;
    }
}
