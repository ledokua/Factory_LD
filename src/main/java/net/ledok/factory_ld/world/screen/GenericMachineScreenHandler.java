package net.ledok.factory_ld.world.screen;

import java.util.List;
import java.util.Optional;

import net.ledok.factory_ld.registry.ModRecipes;
import net.ledok.factory_ld.registry.ModScreenHandlers;
import net.ledok.factory_ld.world.block.entity.GenericMachineBlockEntity;
import net.ledok.factory_ld.world.block.entity.MachineSpec;
import net.ledok.factory_ld.world.block.entity.MachineSpecs;
import net.ledok.factory_ld.world.player.PlayerOverclockAccess;
import net.ledok.factory_ld.world.recipe.MachineRecipe;
import net.ledok.factory_ld.world.recipe.view.MachineRecipeView;
import net.ledok.factory_ld.world.recipe.view.MachineRecipeViewFactory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;

public class GenericMachineScreenHandler extends AbstractMachineScreenHandler<GenericMachineBlockEntity, MachineRecipe>
    implements FluidMachineMenuContext, MachineScreenConfigProvider {

    private final String machineId;
    private final MachineSpec spec;
    private final MachineGuiLayout layout;
    private final int[] inputGhostSlotIndices;

    public GenericMachineScreenHandler(int syncId, Inventory playerInventory, GenericMachineScreenData data, String machineId) {
        this(
            syncId,
            playerInventory,
            resolveBlockEntity(playerInventory, data.pos(), GenericMachineBlockEntity.class, machineId),
            machineId,
            data.unlockedRecipes(),
            data.overclockUnlocked()
        );
    }

    public GenericMachineScreenHandler(int syncId, Inventory playerInventory, GenericMachineBlockEntity blockEntity) {
        this(syncId, playerInventory, blockEntity, blockEntity.machineId(), getUnlockedFor(playerInventory), PlayerOverclockAccess.isUnlocked(playerInventory.player));
    }

    private GenericMachineScreenHandler(
        int syncId,
        Inventory playerInventory,
        GenericMachineBlockEntity blockEntity,
        String machineId,
        List<ResourceLocation> unlocked,
        boolean overclockUnlocked
    ) {
        super(ModScreenHandlers.requireMachineMenuType(machineId), syncId, playerInventory, blockEntity, unlocked, overclockUnlocked);
        this.machineId = machineId;
        this.spec = MachineSpecs.require(machineId);
        this.layout = MachineGuiLayouts.fromSpec(spec);
        this.inputGhostSlotIndices = createInputGhostIndices();

        for (int i = 0; i < spec.itemInputSlots(); i++) {
            addSlot(new InputSlot(blockEntity, blockEntity.getItemInputSlot(i), layout.itemInputX(i), layout.itemInputY(i), i));
        }
        for (int i = 0; i < spec.itemOutputSlots(); i++) {
            addSlot(createOutputSlot(blockEntity, blockEntity.getItemOutputSlot(i), layout.itemOutputX(i), layout.itemOutputY(i)));
        }
        for (int i = 0; i < spec.shardSlots(); i++) {
            addSlot(createShardSlot(blockEntity, blockEntity.getShardSlotStart() + i, layout.shardSlotX(i), layout.shardSlotY(), blockEntity::isValidShard));
        }
        addPlayerInventorySlots(playerInventory, layout.playerInvY());
        addPlayerHotbarSlots(playerInventory, layout.playerHotbarY());
    }

    public Optional<MachineRecipe> getSelectedRecipe() {
        return getSelectedRecipeFromId();
    }

    @Override
    public boolean isFluidPrimary(ResourceLocation recipeId) {
        if (recipeId == null) {
            return false;
        }
        for (RecipeHolder<MachineRecipe> holder : getAvailableRecipes()) {
            if (holder.id().equals(recipeId)) {
                return holder.value().isFluidPrimaryOutput();
            }
        }
        return false;
    }

    @Override
    public int getInputFluidTankCount() {
        return spec.fluidInputTanks();
    }

    @Override
    public int getOutputFluidTankCount() {
        return spec.fluidOutputTanks();
    }

    @Override
    public net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant getInputFluidVariant(int index) {
        return blockEntity.getInputFluidVariant(index);
    }

    @Override
    public net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant getOutputFluidVariant(int index) {
        return blockEntity.getOutputFluidVariant(index);
    }

    @Override
    public long getInputFluidMb(int index) {
        return blockEntity.getInputFluidMb(index);
    }

    @Override
    public long getOutputFluidMb(int index) {
        return blockEntity.getOutputFluidMb(index);
    }

    @Override
    public int playerInventoryY() {
        return layout.playerInvY();
    }

    @Override
    public int shardSlotY() {
        return layout.shardSlotY();
    }

    @Override
    public int[] inputGhostSlotIndices() {
        return inputGhostSlotIndices.clone();
    }

    @Override
    public int outputGhostSlotIndex() {
        return spec.itemInputSlots();
    }

    @Override
    public String machineTypeKey() {
        return machineId;
    }

    @Override
    public boolean supportsFluidSlots() {
        return spec.fluidInputTanks() > 0 || spec.fluidOutputTanks() > 0;
    }

    @Override
    public int fluidInputSlotCount() {
        return spec.fluidInputTanks();
    }

    @Override
    public int fluidOutputSlotCount() {
        return spec.fluidOutputTanks();
    }

    @Override
    public int fluidInputSlotX() {
        return spec.fluidInputTanks() > 0 ? layout.fluidInputX(0) : 0;
    }

    @Override
    public int fluidInputSlotY() {
        return spec.fluidInputTanks() > 0 ? layout.fluidInputY(0) : 0;
    }

    @Override
    public int fluidOutputSlotX() {
        return spec.fluidOutputTanks() > 0 ? layout.fluidOutputX(0) : 0;
    }

    @Override
    public int fluidOutputSlotY() {
        return spec.fluidOutputTanks() > 0 ? layout.fluidOutputY(0) : 0;
    }

    @Override
    public int fluidInputSlotX(int index) {
        return index >= 0 && index < spec.fluidInputTanks() ? layout.fluidInputX(index) : 0;
    }

    @Override
    public int fluidInputSlotY(int index) {
        return index >= 0 && index < spec.fluidInputTanks() ? layout.fluidInputY(index) : 0;
    }

    @Override
    public int fluidOutputSlotX(int index) {
        return index >= 0 && index < spec.fluidOutputTanks() ? layout.fluidOutputX(index) : 0;
    }

    @Override
    public int fluidOutputSlotY(int index) {
        return index >= 0 && index < spec.fluidOutputTanks() ? layout.fluidOutputY(index) : 0;
    }

    @Override
    public UiProductionStatus getUiProductionStatus() {
        Optional<MachineRecipe> selectedRecipe = getSelectedRecipe();
        if (selectedRecipe.isEmpty()) {
            return UiProductionStatus.IDLE;
        }
        MachineRecipe recipe = selectedRecipe.get();

        for (int i = 0; i < spec.itemInputSlots(); i++) {
            MachineRecipe.InputEntry required = recipe.getInputEntry(i);
            if (required == null) {
                return UiProductionStatus.IDLE;
            }
            ItemStack input = blockEntity.getItems().get(blockEntity.getItemInputSlot(i));
            if (input.isEmpty() || input.getCount() < Math.max(1, required.count()) || !required.ingredient().test(input)) {
                return UiProductionStatus.IDLE;
            }
        }

        for (int i = 0; i < spec.fluidInputTanks(); i++) {
            MachineRecipe.FluidEntry required = recipe.getFluidInputEntry(i);
            if (required == null) {
                return UiProductionStatus.IDLE;
            }
            Fluid requiredFluid = BuiltInRegistries.FLUID.getOptional(required.fluidId()).orElse(null);
            if (requiredFluid == null) {
                return UiProductionStatus.IDLE;
            }
            long requiredMb = Math.max(1L, required.amountMb());
            var inputVariant = getInputFluidVariant(i);
            if (inputVariant.isBlank() || !inputVariant.isOf(requiredFluid) || getInputFluidMb(i) < requiredMb) {
                return UiProductionStatus.IDLE;
            }
        }

        for (int i = 0; i < spec.itemOutputSlots(); i++) {
            ItemStack result = recipe.getOutputEntry(i);
            if (result.isEmpty()) {
                return UiProductionStatus.IDLE;
            }
            ItemStack output = blockEntity.getItems().get(blockEntity.getItemOutputSlot(i));
            if (!output.isEmpty()) {
                if (!ItemStack.isSameItemSameComponents(output, result)) {
                    return UiProductionStatus.IDLE;
                }
                if (output.getCount() + result.getCount() > output.getMaxStackSize()) {
                    return UiProductionStatus.IDLE;
                }
            }
        }

        for (int i = 0; i < spec.fluidOutputTanks(); i++) {
            MachineRecipe.FluidEntry result = recipe.getFluidOutputEntry(i);
            if (result == null) {
                return UiProductionStatus.IDLE;
            }
            Fluid outputFluid = BuiltInRegistries.FLUID.getOptional(result.fluidId()).orElse(null);
            if (outputFluid == null) {
                return UiProductionStatus.IDLE;
            }
            long resultMb = Math.max(1L, result.amountMb());
            var outputVariant = getOutputFluidVariant(i);
            if (!outputVariant.isBlank() && !outputVariant.isOf(outputFluid)) {
                return UiProductionStatus.IDLE;
            }
            long capacityMb = spec.fluidTankCapacityDroplets() * 1000L / net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET;
            if (getOutputFluidMb(i) + resultMb > capacityMb) {
                return UiProductionStatus.IDLE;
            }
        }

        double powerPerTick = blockEntity.getPowerUsageMw() / 20.0;
        if (blockEntity.getEnergyStored() < powerPerTick) {
            return UiProductionStatus.NO_POWER;
        }
        return UiProductionStatus.WORKING;
    }

    @Override
    protected RecipeType<MachineRecipe> recipeType() {
        return ModRecipes.requireMachineRecipeType(machineId);
    }

    @Override
    protected MachineRecipeView toView(ResourceLocation id, MachineRecipe recipe) {
        return MachineRecipeViewFactory.fromRecipe(id, recipe);
    }

    private int[] createInputGhostIndices() {
        int[] indices = new int[spec.itemInputSlots()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        return indices;
    }

    private class InputSlot extends Slot {
        private final GenericMachineBlockEntity machine;
        private final int inputIndex;

        private InputSlot(Container inventory, int index, int x, int y, int inputIndex) {
            super(inventory, index, x, y);
            this.machine = (GenericMachineBlockEntity) inventory;
            this.inputIndex = inputIndex;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return machineSlotsActive() && machine.isValidInput(inputIndex, stack);
        }

        @Override
        public boolean isActive() {
            return machineSlotsActive();
        }
    }
}
