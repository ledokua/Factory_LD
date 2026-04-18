package net.ledok.factory_ld.world.block.entity;

import net.ledok.factory_ld.registry.ModBlockEntities;
import net.ledok.factory_ld.world.power.PowerConnectable;
import net.ledok.factory_ld.world.power.PowerGridManager;
import net.ledok.factory_ld.world.power.PowerUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class GenericPowerStorageBlockEntity extends BlockEntity implements PowerConnectable {
    private final String storageId;
    private final PowerStorageSpec spec;
    private double storedEnergyMj = 0.0;

    public GenericPowerStorageBlockEntity(BlockPos pos, BlockState state, String storageId) {
        super(ModBlockEntities.requirePowerStorageBlockEntityType(storageId), pos, state);
        this.storageId = storageId;
        this.spec = PowerStorageSpecs.require(storageId);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GenericPowerStorageBlockEntity blockEntity) {
        // Power flow is coordinated centrally by PowerGridManager.
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

    public String storageId() {
        return storageId;
    }

    @Override
    public int maxPowerConnections() {
        return 2;
    }

    @Override
    public Vec3 powerConnectionOffset() {
        return new Vec3(0.5, 0.85, 0.5);
    }

    public double capacityMj() {
        return PowerUnits.mwhToMj(spec.capacityMwh());
    }

    public double maxChargePerTickMj() {
        return PowerUnits.mwToMjPerTick(spec.maxChargeMw());
    }

    public double maxDischargePerTickMj() {
        return PowerUnits.mwToMjPerTick(spec.maxDischargeMw());
    }

    public double storedEnergyMj() {
        return storedEnergyMj;
    }

    public double availableDischargePerTickMj() {
        return Math.min(storedEnergyMj, maxDischargePerTickMj());
    }

    public double availableChargePerTickMj() {
        return Math.min(Math.max(0.0, capacityMj() - storedEnergyMj), maxChargePerTickMj());
    }

    public double charge(double amountMj) {
        if (amountMj <= 0.0) {
            return 0.0;
        }
        double accepted = Math.min(amountMj, availableChargePerTickMj());
        if (accepted <= 0.0) {
            return 0.0;
        }
        storedEnergyMj += accepted;
        setChanged();
        return accepted;
    }

    public double discharge(double amountMj) {
        if (amountMj <= 0.0) {
            return 0.0;
        }
        double extracted = Math.min(amountMj, availableDischargePerTickMj());
        if (extracted <= 0.0) {
            return 0.0;
        }
        storedEnergyMj = Math.max(0.0, storedEnergyMj - extracted);
        setChanged();
        return extracted;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        if (nbt.contains("StoredEnergyMj")) {
            storedEnergyMj = Math.max(0.0, Math.min(capacityMj(), nbt.getDouble("StoredEnergyMj")));
        } else {
            storedEnergyMj = 0.0;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        nbt.putDouble("StoredEnergyMj", storedEnergyMj);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveCustomOnly(provider);
    }
}
