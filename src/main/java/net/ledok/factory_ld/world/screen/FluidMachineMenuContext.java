package net.ledok.factory_ld.world.screen;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.resources.ResourceLocation;

public interface FluidMachineMenuContext extends MachineMenuContext {
    boolean isFluidPrimary(ResourceLocation recipeId);

    int getInputFluidTankCount();

    int getOutputFluidTankCount();

    FluidVariant getInputFluidVariant(int index);

    FluidVariant getOutputFluidVariant(int index);

    long getInputFluidMb(int index);

    long getOutputFluidMb(int index);

    default FluidVariant getInputFluidVariant() {
        return getInputFluidVariant(0);
    }

    default FluidVariant getOutputFluidVariant() {
        return getOutputFluidVariant(0);
    }

    default long getInputFluidMb() {
        return getInputFluidMb(0);
    }

    default long getOutputFluidMb() {
        return getOutputFluidMb(0);
    }
}
