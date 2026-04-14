package net.ledok.factory_ld.world.screen;

import net.ledok.factory_ld.registry.ModBlocks;
import net.ledok.factory_ld.registry.ModScreenHandlers;
import net.ledok.factory_ld.world.block.entity.PowerEmitterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

public class PowerEmitterScreenHandler extends AbstractContainerMenu {
    private static final int POWER_SCALE = 10;
    private static final int ENERGY_SCALE = 100;
    private final PowerEmitterBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData powerData;
    private final int[] syncedValues = new int[11];

    public PowerEmitterScreenHandler(int syncId, Inventory playerInventory, PowerEmitterScreenData data) {
        this(syncId, playerInventory, resolveBlockEntity(playerInventory, data.pos()));
    }

    public PowerEmitterScreenHandler(int syncId, Inventory playerInventory, PowerEmitterBlockEntity blockEntity) {
        super(ModScreenHandlers.POWER_EMITTER, syncId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.powerData = createPowerData();
        addDataSlots(powerData);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.POWER_EMITTER);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0) {
            return false;
        }
        access.execute((level, pos) -> net.ledok.factory_ld.world.power.PowerGridManager.resetGrid(level, pos));
        return true;
    }

    public double capacityMw() {
        return powerData.get(0) / (double) POWER_SCALE;
    }

    public double productionMw() {
        return powerData.get(1) / (double) POWER_SCALE;
    }

    public double consumptionMw() {
        return powerData.get(2) / (double) POWER_SCALE;
    }

    public double maxConsumptionMw() {
        return powerData.get(3) / (double) POWER_SCALE;
    }

    public double storageStoredMwh() {
        return powerData.get(4) / (double) ENERGY_SCALE;
    }

    public double storageCapacityMwh() {
        return powerData.get(5) / (double) ENERGY_SCALE;
    }

    public double storageChargeMw() {
        return powerData.get(6) / (double) POWER_SCALE;
    }

    public double storageDischargeMw() {
        return powerData.get(7) / (double) POWER_SCALE;
    }

    public boolean tripped() {
        return powerData.get(8) != 0;
    }

    public int generatorCount() {
        return powerData.get(9);
    }

    public int consumerCount() {
        return powerData.get(10);
    }

    private static PowerEmitterBlockEntity resolveBlockEntity(Inventory playerInventory, BlockPos pos) {
        var level = playerInventory.player.level();
        if (level.getBlockEntity(pos) instanceof PowerEmitterBlockEntity emitter) {
            return emitter;
        }
        throw new IllegalStateException("Power emitter block entity not found at " + pos);
    }

    private ContainerData createPowerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (blockEntity.getLevel() != null && blockEntity.getLevel().isClientSide) {
                    return index >= 0 && index < syncedValues.length ? syncedValues[index] : 0;
                }
                var stats = blockEntity.getPowerGridStats();
                return switch (index) {
                    case 0 -> scaledPower(stats.capacityMw());
                    case 1 -> scaledPower(stats.productionMw());
                    case 2 -> scaledPower(stats.consumptionMw());
                    case 3 -> scaledPower(stats.maxConsumptionMw());
                    case 4 -> scaledEnergy(stats.storageStoredMwh());
                    case 5 -> scaledEnergy(stats.storageCapacityMwh());
                    case 6 -> scaledPower(stats.storageChargeMw());
                    case 7 -> scaledPower(stats.storageDischargeMw());
                    case 8 -> stats.tripped() ? 1 : 0;
                    case 9 -> stats.generatorCount();
                    case 10 -> stats.consumerCount();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index >= 0 && index < syncedValues.length) {
                    syncedValues[index] = value;
                }
            }

            @Override
            public int getCount() {
                return 11;
            }
        };
    }

    private static int scaledPower(double valueMw) {
        return (int) Math.round(Math.max(0.0, valueMw) * POWER_SCALE);
    }

    private static int scaledEnergy(double valueMwh) {
        return (int) Math.round(Math.max(0.0, valueMwh) * ENERGY_SCALE);
    }
}
