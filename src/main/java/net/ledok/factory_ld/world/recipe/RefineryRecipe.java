package net.ledok.factory_ld.world.recipe;

import java.util.List;

import net.ledok.factory_ld.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class RefineryRecipe extends ConstructorRecipe {
    private final PrimaryOutput primaryOutput;
    private final List<FluidEntry> fluidInputs;
    private final List<FluidEntry> fluidOutputs;

    public RefineryRecipe(
        String recipeName,
        String recipeNameKey,
        String category,
        String researchGroup,
        PrimaryOutput primaryOutput,
        List<InputEntry> itemInputs,
        List<FluidEntry> fluidInputs,
        List<ItemStack> itemOutputs,
        List<FluidEntry> fluidOutputs,
        int craftTime
    ) {
        super(recipeName, recipeNameKey, category, researchGroup, itemInputs, itemOutputs, craftTime);
        this.primaryOutput = primaryOutput == null ? PrimaryOutput.ITEM : primaryOutput;
        this.fluidInputs = List.copyOf(fluidInputs == null ? List.of() : fluidInputs);
        this.fluidOutputs = List.copyOf(fluidOutputs == null ? List.of() : fluidOutputs);
    }

    public PrimaryOutput getPrimaryOutput() {
        return primaryOutput;
    }

    public boolean isFluidPrimaryOutput() {
        return primaryOutput == PrimaryOutput.FLUID;
    }

    public FluidEntry getFluidInputEntry(int index) {
        if (index < 0 || index >= fluidInputs.size()) {
            return null;
        }
        return fluidInputs.get(index);
    }

    public FluidEntry getFluidOutputEntry(int index) {
        if (index < 0 || index >= fluidOutputs.size()) {
            return null;
        }
        return fluidOutputs.get(index);
    }

    public List<FluidEntry> getFluidInputs() {
        return fluidInputs;
    }

    public List<FluidEntry> getFluidOutputs() {
        return fluidOutputs;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        if (input == null || input.size() < 1) {
            return false;
        }
        InputEntry in = getInputEntry(0);
        return in != null && in.ingredient().test(input.getItem(0));
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(1, Ingredient.EMPTY);
        InputEntry in = getInputEntry(0);
        ingredients.set(0, in == null ? Ingredient.EMPTY : in.ingredient());
        return ingredients;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return getOutput();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.REFINERY_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.REFINERY_TYPE;
    }

    public record FluidEntry(ResourceLocation fluidId, long amountMb) {
        public FluidEntry {
            amountMb = Math.max(1L, amountMb);
        }
    }

    public enum PrimaryOutput {
        ITEM,
        FLUID
    }
}
