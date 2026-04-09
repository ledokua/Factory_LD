package net.ledok.factory_ld.world.recipe.view;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

public record SimpleMachineRecipeView(
    ResourceLocation id,
    String category,
    String name,
    String nameKey,
    int craftTimeTicks,
    List<MachineItemStackView> itemInputs,
    List<MachineFluidView> fluidInputs,
    List<MachineItemStackView> itemOutputs,
    List<MachineFluidView> fluidOutputs
) implements MachineRecipeView {
    public SimpleMachineRecipeView {
        category = category == null ? "" : category;
        name = name == null ? "" : name;
        nameKey = nameKey == null ? "" : nameKey;
        craftTimeTicks = Math.max(1, craftTimeTicks);
        itemInputs = List.copyOf(itemInputs == null ? List.of() : itemInputs);
        fluidInputs = List.copyOf(fluidInputs == null ? List.of() : fluidInputs);
        itemOutputs = List.copyOf(itemOutputs == null ? List.of() : itemOutputs);
        fluidOutputs = List.copyOf(fluidOutputs == null ? List.of() : fluidOutputs);
    }
}
