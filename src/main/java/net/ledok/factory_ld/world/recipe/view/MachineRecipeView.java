package net.ledok.factory_ld.world.recipe.view;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

public interface MachineRecipeView {
    ResourceLocation id();

    String category();

    String name();

    String nameKey();

    int craftTimeTicks();

    List<MachineItemStackView> itemInputs();

    List<MachineFluidView> fluidInputs();

    List<MachineItemStackView> itemOutputs();

    List<MachineFluidView> fluidOutputs();
}
