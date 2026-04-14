package net.ledok.factory_ld.world.recipe;

import java.util.List;

import net.ledok.factory_ld.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class MachineRecipe implements Recipe<RecipeInput>, MachineRecipeDefinition {
    private final String machineTypeId;
    private final String recipeName;
    private final String recipeNameKey;
    private final String category;
    private final String researchGroup;
    private final PrimaryOutput primaryOutput;
    private final List<InputEntry> itemInputs;
    private final List<ItemStack> itemOutputs;
    private final List<FluidEntry> fluidInputs;
    private final List<FluidEntry> fluidOutputs;
    private final int craftTime;

    public MachineRecipe(
        String machineTypeId,
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
        this.machineTypeId = machineTypeId;
        this.recipeName = recipeName;
        this.recipeNameKey = recipeNameKey == null ? "" : recipeNameKey;
        this.category = category;
        this.researchGroup = researchGroup;
        this.primaryOutput = primaryOutput == null ? PrimaryOutput.ITEM : primaryOutput;
        this.itemInputs = List.copyOf(itemInputs == null ? List.of() : itemInputs);
        this.fluidInputs = List.copyOf(fluidInputs == null ? List.of() : fluidInputs);
        this.itemOutputs = List.copyOf(itemOutputs == null ? List.of() : itemOutputs);
        this.fluidOutputs = List.copyOf(fluidOutputs == null ? List.of() : fluidOutputs);
        this.craftTime = craftTime;
    }

    public String getMachineTypeId() {
        return machineTypeId;
    }

    @Override
    public String getRecipeName() {
        return recipeName;
    }

    @Override
    public String getRecipeNameKey() {
        return recipeNameKey;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public String getResearchGroup() {
        return researchGroup;
    }

    public PrimaryOutput getPrimaryOutput() {
        return primaryOutput;
    }

    @Override
    public boolean isFluidPrimaryOutput() {
        return primaryOutput == PrimaryOutput.FLUID;
    }

    public int getCraftTime() {
        return craftTime;
    }

    @Override
    public List<InputEntry> getItemInputs() {
        return itemInputs;
    }

    @Override
    public List<ItemStack> getItemOutputs() {
        return itemOutputs;
    }

    @Override
    public List<FluidEntry> getFluidInputs() {
        return fluidInputs;
    }

    @Override
    public List<FluidEntry> getFluidOutputs() {
        return fluidOutputs;
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
        ItemStack out = itemOutputs.get(index);
        return out.isEmpty() ? ItemStack.EMPTY : out.copy();
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

    @Override
    public boolean matches(RecipeInput input, Level level) {
        if (input == null || input.size() < itemInputs.size()) {
            return false;
        }
        for (int i = 0; i < itemInputs.size(); i++) {
            InputEntry entry = itemInputs.get(i);
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty() || stack.getCount() < Math.max(1, entry.count()) || !entry.ingredient().test(stack)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) {
        return getResultItem(provider);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return getOutputEntry(0);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(itemInputs.size(), Ingredient.EMPTY);
        for (int i = 0; i < itemInputs.size(); i++) {
            ingredients.set(i, itemInputs.get(i).ingredient());
        }
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.requireMachineRecipeSerializer(machineTypeId);
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.requireMachineRecipeType(machineTypeId);
    }

    public record InputEntry(Ingredient ingredient, int count) {
        public InputEntry {
            count = Math.max(1, count);
        }
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
