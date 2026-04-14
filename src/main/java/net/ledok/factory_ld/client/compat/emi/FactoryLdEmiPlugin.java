package net.ledok.factory_ld.client.compat.emi;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.ledok.factory_ld.FactoryLdMod;
import net.ledok.factory_ld.client.screen.AbstractFluidMachineScreen;
import net.ledok.factory_ld.client.screen.FluidHoverInfo;
import net.ledok.factory_ld.registry.ModBlocks;
import net.ledok.factory_ld.registry.ModRecipes;
import net.ledok.factory_ld.world.block.entity.MachineSpec;
import net.ledok.factory_ld.world.block.entity.MachineSpecs;
import net.ledok.factory_ld.world.recipe.MachineRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class FactoryLdEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registerMachineRecipes(registry);
        registry.addGenericStackProvider((screen, mouseX, mouseY) -> {
            if (screen instanceof AbstractFluidMachineScreen<?> fluidScreen) {
                return fluidStackAt(fluidScreen, mouseX, mouseY);
            }
            return EmiStackInteraction.EMPTY;
        });
    }

    private void registerMachineRecipes(EmiRegistry registry) {
        for (MachineSpec spec : MachineSpecs.ALL) {
            EmiStack workstation = EmiStack.of(new ItemStack(ModBlocks.requireMachineBlock(spec.id())));
            EmiRecipeCategory category = new EmiRecipeCategory(machineCategoryId(spec.id()), workstation);
            registry.addCategory(category);
            registry.addWorkstation(category, workstation);
            for (RecipeHolder<MachineRecipe> holder : registry.getRecipeManager().getAllRecipesFor(ModRecipes.requireMachineRecipeType(spec.id()))) {
                registry.addRecipe(new FactoryLdMachineEmiRecipe(category, holder.id(), holder.value(), workstation, spec));
            }
        }
    }

    private static ResourceLocation machineCategoryId(String machineId) {
        return FactoryLdMod.id(machineId);
    }

    private EmiStackInteraction fluidStackAt(AbstractFluidMachineScreen<?> screen, int mouseX, int mouseY) {
        FluidHoverInfo hover = screen.getFluidHoverInfoAt(mouseX, mouseY);
        if (hover == null || hover.fluidId() == null) {
            return EmiStackInteraction.EMPTY;
        }
        Fluid fluid = BuiltInRegistries.FLUID.getOptional(hover.fluidId()).orElse(Fluids.EMPTY);
        if (fluid == Fluids.EMPTY) {
            return EmiStackInteraction.EMPTY;
        }
        long amountMb = hover.amountMb() == null ? 1000L : Math.max(1L, hover.amountMb());
        long amountDroplets = amountMb * FluidConstants.BUCKET / 1000L;
        return new EmiStackInteraction(EmiStack.of(fluid, Math.max(1L, amountDroplets)), null, true);
    }
}
