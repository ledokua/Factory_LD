package net.ledok.factory_ld.world.recipe.view;

import net.minecraft.resources.ResourceLocation;

public record MachineFluidView(ResourceLocation fluidId, long amountMb) {
    public MachineFluidView {
        amountMb = Math.max(0L, amountMb);
    }

    public boolean isEmpty() {
        return fluidId == null || amountMb <= 0;
    }
}
