package net.ledok.factory_ld.world.recipe;

import java.util.List;

import net.minecraft.world.item.ItemStack;

public interface MachineRecipeDefinition {
    String getRecipeName();

    String getRecipeNameKey();

    String getCategory();

    String getResearchGroup();

    int getCraftTime();

    List<MachineRecipe.InputEntry> getItemInputs();

    List<ItemStack> getItemOutputs();

    default List<MachineRecipe.FluidEntry> getFluidInputs() {
        return List.of();
    }

    default List<MachineRecipe.FluidEntry> getFluidOutputs() {
        return List.of();
    }

    default boolean isFluidPrimaryOutput() {
        return false;
    }
}
