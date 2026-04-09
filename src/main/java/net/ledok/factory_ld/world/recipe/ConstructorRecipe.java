package net.ledok.factory_ld.world.recipe;

import net.ledok.factory_ld.registry.ModRecipes;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class ConstructorRecipe implements Recipe<RecipeInput> {
    private final String recipeName;
    private final String recipeNameKey;
    private final String category;
    private final String researchGroup;
    private final List<InputEntry> itemInputs;
    private final List<ItemStack> itemOutputs;
    private final int craftTime;

    public ConstructorRecipe(
        String recipeName,
        String recipeNameKey,
        String category,
        String researchGroup,
        List<InputEntry> itemInputs,
        List<ItemStack> itemOutputs,
        int craftTime
    ) {
        this.recipeName = recipeName;
        this.recipeNameKey = recipeNameKey == null ? "" : recipeNameKey;
        this.category = category;
        this.researchGroup = researchGroup;
        this.itemInputs = itemInputs;
        this.itemOutputs = itemOutputs;
        this.craftTime = craftTime;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public String getRecipeNameKey() {
        return recipeNameKey;
    }

    public String getCategory() {
        return category;
    }

    public String getResearchGroup() {
        return researchGroup;
    }

    public Ingredient getInput() {
        InputEntry entry = getInputEntry(0);
        return entry == null ? Ingredient.EMPTY : entry.ingredient();
    }

    public ItemStack getOutput() {
        ItemStack output = getOutputEntry(0);
        return output.isEmpty() ? ItemStack.EMPTY : output.copy();
    }

    public int getInputCount() {
        InputEntry entry = getInputEntry(0);
        return entry == null ? 1 : entry.count();
    }

    public InputEntry getInputEntry(int index) {
        if (index < 0 || index >= itemInputs.size()) {
            return null;
        }
        return itemInputs.get(index);
    }

    public ItemStack getOutputEntry(int index) {
        if (index < 0 || index >= itemOutputs.size()) {
            return ItemStack.EMPTY;
        }
        return itemOutputs.get(index);
    }

    public int getCraftTime() {
        return craftTime;
    }

    public List<InputEntry> getItemInputs() {
        return itemInputs;
    }

    public List<ItemStack> getItemOutputs() {
        return itemOutputs;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        if (input == null || input.size() < 1) {
            return false;
        }
        return this.getInput().test(input.getItem(0));
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) {
        return getOutput();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return getOutput();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(1, Ingredient.EMPTY);
        ingredients.set(0, getInput());
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CONSTRUCTOR_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.CONSTRUCTOR_TYPE;
    }

    public record InputEntry(Ingredient ingredient, int count) {
    }
}
