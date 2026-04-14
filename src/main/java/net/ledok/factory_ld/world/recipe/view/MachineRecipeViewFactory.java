package net.ledok.factory_ld.world.recipe.view;

import java.util.ArrayList;
import java.util.List;

import net.ledok.factory_ld.world.recipe.MachineRecipeDefinition;
import net.ledok.factory_ld.world.recipe.MachineRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class MachineRecipeViewFactory {
    private MachineRecipeViewFactory() {
    }

    public static MachineRecipeView fromRecipe(ResourceLocation id, MachineRecipeDefinition recipe) {
        List<MachineItemStackView> inputs = new ArrayList<>();
        for (MachineRecipe.InputEntry input : recipe.getItemInputs()) {
            ItemStack[] options = input.ingredient().getItems();
            if (options.length == 0) {
                continue;
            }
            ItemStack display = options[0].copy();
            display.setCount(1);
            inputs.add(new MachineItemStackView(display, input.count()));
        }

        List<MachineItemStackView> outputs = new ArrayList<>();
        for (ItemStack stack : recipe.getItemOutputs()) {
            ItemStack display = stack.copy();
            int amount = Math.max(1, display.getCount());
            display.setCount(1);
            outputs.add(new MachineItemStackView(display, amount));
        }
        List<MachineFluidView> fluidInputs = new ArrayList<>();
        for (MachineRecipe.FluidEntry input : recipe.getFluidInputs()) {
            fluidInputs.add(new MachineFluidView(input.fluidId(), input.amountMb()));
        }
        List<MachineFluidView> fluidOutputs = new ArrayList<>();
        for (MachineRecipe.FluidEntry output : recipe.getFluidOutputs()) {
            fluidOutputs.add(new MachineFluidView(output.fluidId(), output.amountMb()));
        }

        return new SimpleMachineRecipeView(
            id,
            recipe.getCategory(),
            recipe.getRecipeName(),
            recipe.getRecipeNameKey(),
            recipe.getCraftTime(),
            inputs,
            fluidInputs,
            outputs,
            fluidOutputs
        );
    }
}
