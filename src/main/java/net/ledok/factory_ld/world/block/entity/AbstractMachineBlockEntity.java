package net.ledok.factory_ld.world.block.entity;

import java.util.Optional;

import net.ledok.factory_ld.inventory.ImplementedInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ledok.factory_ld.world.power.PowerConnectable;
import net.ledok.factory_ld.world.power.PowerGridManager;
import net.ledok.factory_ld.world.power.PowerUnits;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractMachineBlockEntity extends BlockEntity implements ImplementedInventory, OverclockMachineEntity, PowerConnectable {
    protected final NonNullList<ItemStack> items;
    protected final MachineSpec machineSpec;
    protected ResourceLocation selectedRecipeId;
    protected double progress;
    protected double clockSpeedPercent = 100.0;
    protected boolean lastPowerAvailable = true;

    protected AbstractMachineBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState state,
        int slotCount,
        MachineSpec machineSpec
    ) {
        super(type, pos, state);
        this.machineSpec = machineSpec;
        this.items = NonNullList.withSize(slotCount, ItemStack.EMPTY);
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
    public void clearRemoved() {
        super.clearRemoved();
        if (level != null && !level.isClientSide) {
            PowerGridManager.markDirty(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            PowerGridManager.markDirty(level, worldPosition);
        }
        super.setRemoved();
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

    public MachineSpec getMachineSpec() {
        return machineSpec;
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

    @Override
    public int maxPowerConnections() {
        return 1;
    }

    @Override
    public Vec3 powerConnectionOffset() {
        return new Vec3(0.5, 0.9, 0.5);
    }

    public int getShardCount() {
        if (!machineSpec.overclockEnabled()) {
            return 0;
        }
        int count = 0;
        int shardSlots = Math.min(getShardSlotCount(), machineSpec.shardSlots());
        for (int slot = getShardSlotStart(); slot < getShardSlotStart() + shardSlots; slot++) {
            if (!items.get(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public double getMaxClockSpeedPercent() {
        if (!machineSpec.overclockEnabled()) {
            return 100.0;
        }
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
        return 0.0;
    }

    public double getEnergyCapacity() {
        return 0.0;
    }

    public double addEnergy(double amountMj) {
        return 0.0;
    }

    public double getPowerUsageMw() {
        double speed = clockSpeedPercent / 100.0;
        return machineSpec.basePowerMw() * Math.pow(speed, 1.321928);
    }

    protected void resetProgress() {
        progress = 0.0;
    }

    protected double getClockSpeedMultiplier() {
        return Math.max(0.01, clockSpeedPercent / 100.0);
    }

    protected boolean consumePowerForTick() {
        double powerPerTick = PowerUnits.mwToMjPerTick(getPowerUsageMw());
        return level != null && PowerGridManager.consume(level, worldPosition, powerPerTick);
    }

    public boolean hasSufficientPowerForTick() {
        double powerPerTick = PowerUnits.mwToMjPerTick(getPowerUsageMw());
        return level != null && PowerGridManager.canProvide(level, worldPosition, powerPerTick);
    }

    public boolean wasPoweredLastTick() {
        return lastPowerAvailable;
    }

    protected void advanceProgressTick() {
        progress += getClockSpeedMultiplier();
    }

    protected boolean isCraftComplete(int craftTime) {
        return progress >= craftTime;
    }

    protected void consumeCraftProgress(int craftTime) {
        progress -= craftTime;
    }

    protected boolean processCraftingTick(int craftTimeTicks, boolean canProcess, Runnable onCraftCompleted) {
        if (!canProcess) {
            boolean powerChanged = !lastPowerAvailable;
            lastPowerAvailable = true;
            resetProgress();
            if (powerChanged) {
                setChanged();
            }
            return false;
        }
        if (!consumePowerForTick()) {
            boolean powerChanged = lastPowerAvailable;
            lastPowerAvailable = false;
            resetProgress();
            if (powerChanged) {
                setChanged();
            }
            return false;
        }

        lastPowerAvailable = true;
        advanceProgressTick();
        if (isCraftComplete(craftTimeTicks)) {
            consumeCraftProgress(craftTimeTicks);
            onCraftCompleted.run();
        }
        setChanged();
        return true;
    }

    protected void loadMachineData(CompoundTag nbt) {
        if (nbt.contains("SelectedRecipe")) {
            selectedRecipeId = ResourceLocation.tryParse(nbt.getString("SelectedRecipe"));
        } else {
            selectedRecipeId = null;
        }
        progress = nbt.getDouble("Progress");
        if (nbt.contains("ClockSpeed")) {
            clockSpeedPercent = nbt.getDouble("ClockSpeed");
        } else {
            clockSpeedPercent = 100.0;
        }
        if (nbt.contains("LastPowerAvailable")) {
            lastPowerAvailable = nbt.getBoolean("LastPowerAvailable");
        } else {
            lastPowerAvailable = true;
        }
        clampClockSpeed();
    }

    protected void saveMachineData(CompoundTag nbt) {
        if (selectedRecipeId != null) {
            nbt.putString("SelectedRecipe", selectedRecipeId.toString());
        }
        nbt.putDouble("Progress", progress);
        nbt.putDouble("ClockSpeed", clockSpeedPercent);
        nbt.putBoolean("LastPowerAvailable", lastPowerAvailable);
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

    protected boolean isRecipeIdValidForType(ResourceLocation id, RecipeType<?> recipeType) {
        if (level == null || id == null || recipeType == null) {
            return false;
        }
        Optional<RecipeHolder<?>> entry = level.getRecipeManager().byKey(id);
        return entry.isPresent() && entry.get().value().getType() == recipeType;
    }
}
