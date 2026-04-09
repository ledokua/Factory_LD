package net.ledok.factory_ld.world.block.entity;

import net.ledok.factory_ld.inventory.ImplementedInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractMachineBlockEntity extends BlockEntity implements ImplementedInventory, OverclockMachineEntity {
    protected final NonNullList<ItemStack> items;
    protected ResourceLocation selectedRecipeId;
    protected double progress;
    protected double clockSpeedPercent = 100.0;
    protected double energyStored;

    private final double basePowerMw;
    private final double energyCapacityMj;

    protected AbstractMachineBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState state,
        int slotCount,
        double basePowerMw,
        double energyCapacityMj
    ) {
        super(type, pos, state);
        this.items = NonNullList.withSize(slotCount, ItemStack.EMPTY);
        this.basePowerMw = basePowerMw;
        this.energyCapacityMj = energyCapacityMj;
        this.energyStored = energyCapacityMj;
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        clampClockSpeed();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public ResourceLocation getSelectedRecipeId() {
        return selectedRecipeId;
    }

    @Override
    public void setSelectedRecipeId(ResourceLocation id) {
        if (id != null && !isRecipeIdValid(id)) {
            id = null;
        }
        if ((selectedRecipeId == null && id == null) || (selectedRecipeId != null && selectedRecipeId.equals(id))) {
            return;
        }
        selectedRecipeId = id;
        onRecipeChanged();
        setChanged();
    }

    protected abstract boolean isRecipeIdValid(ResourceLocation id);

    protected void onRecipeChanged() {
    }

    public int getShardCount() {
        int count = 0;
        for (int slot = getShardSlotStart(); slot < getShardSlotStart() + getShardSlotCount(); slot++) {
            if (!items.get(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public double getMaxClockSpeedPercent() {
        double max = 100.0 + 50.0 * getShardCount();
        return Math.min(250.0, max);
    }

    public double getClockSpeedPercent() {
        return clockSpeedPercent;
    }

    protected void setMachineItem(int slot, ItemStack stack) {
        ImplementedInventory.super.setItem(slot, stack);
    }

    public double getProgress() {
        return progress;
    }

    @Override
    public void setClockSpeedPercent(double percent) {
        double clamped = clampClockSpeedPercent(percent);
        if (Math.abs(clamped - clockSpeedPercent) < 0.0001) {
            return;
        }
        clockSpeedPercent = clamped;
        setChanged();
    }

    public double getEnergyStored() {
        return energyStored;
    }

    public double getEnergyCapacity() {
        return energyCapacityMj;
    }

    public double addEnergy(double amountMj) {
        if (amountMj <= 0.0) {
            return 0.0;
        }
        double accepted = Math.min(amountMj, energyCapacityMj - energyStored);
        if (accepted <= 0.0) {
            return 0.0;
        }
        energyStored += accepted;
        setChanged();
        return accepted;
    }

    public double getPowerUsageMw() {
        double speed = clockSpeedPercent / 100.0;
        return basePowerMw * Math.pow(speed, 1.321928);
    }

    protected void loadMachineData(CompoundTag nbt) {
        if (nbt.contains("SelectedRecipe")) {
            selectedRecipeId = ResourceLocation.tryParse(nbt.getString("SelectedRecipe"));
            if (selectedRecipeId != null && !isRecipeIdValid(selectedRecipeId)) {
                selectedRecipeId = null;
            }
        } else {
            selectedRecipeId = null;
        }
        progress = nbt.getDouble("Progress");
        if (nbt.contains("ClockSpeed")) {
            clockSpeedPercent = nbt.getDouble("ClockSpeed");
        } else {
            clockSpeedPercent = 100.0;
        }
        if (nbt.contains("EnergyStored")) {
            energyStored = nbt.getDouble("EnergyStored");
        } else {
            energyStored = energyCapacityMj;
        }
        clampClockSpeed();
    }

    protected void saveMachineData(CompoundTag nbt) {
        if (selectedRecipeId != null) {
            nbt.putString("SelectedRecipe", selectedRecipeId.toString());
        }
        nbt.putDouble("Progress", progress);
        nbt.putDouble("ClockSpeed", clockSpeedPercent);
        nbt.putDouble("EnergyStored", energyStored);
    }

    protected void setProgress(double value) {
        this.progress = value;
    }

    protected void clampClockSpeed() {
        double clamped = clampClockSpeedPercent(clockSpeedPercent);
        if (Math.abs(clamped - clockSpeedPercent) > 0.0001) {
            clockSpeedPercent = clamped;
        }
    }

    protected double clampClockSpeedPercent(double percent) {
        double rounded = Math.round(percent * 10000.0) / 10000.0;
        double min = 1.0;
        double max = getMaxClockSpeedPercent();
        if (rounded < min) {
            return min;
        }
        if (rounded > max) {
            return max;
        }
        return rounded;
    }
}
