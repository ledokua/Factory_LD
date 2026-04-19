package net.ledok.factory_ld.world.power;

import net.ledok.factory_ld.world.block.entity.GenericMachineBlockEntity;
import net.ledok.factory_ld.world.block.entity.GenericPowerStorageBlockEntity;
import net.ledok.factory_ld.world.block.entity.PowerEmitterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class PowerGridManager {
    private static final Map<Level, LevelState> STATES = new WeakHashMap<>();

    private PowerGridManager() {
    }

    public static boolean consume(Level level, BlockPos origin, double amountMj) {
        if (level == null || origin == null || amountMj <= 0.0) {
            return false;
        }
        GridRuntime runtime = runtime(level, origin);
        if (runtime.tripped) {
            return false;
        }
        if (!runtime.poweredConsumers.contains(origin.asLong())) {
            return false;
        }
        double remaining = runtime.remainingConsumerBudgetMj.getOrDefault(origin.asLong(), 0.0);
        if (remaining + 1.0E-9 < amountMj) {
            return false;
        }
        runtime.remainingConsumerBudgetMj.put(origin.asLong(), Math.max(0.0, remaining - amountMj));
        return true;
    }

    public static boolean canProvide(Level level, BlockPos origin, double amountMj) {
        if (level == null || origin == null || amountMj <= 0.0) {
            return false;
        }
        GridRuntime runtime = runtime(level, origin);
        if (runtime.tripped) {
            return false;
        }
        return runtime.poweredConsumers.contains(origin.asLong());
    }

    public static PowerGridStats getStats(Level level, BlockPos origin) {
        if (level == null || origin == null) {
            return PowerGridStats.EMPTY;
        }
        return runtime(level, origin).stats;
    }

    public static boolean isGridTripped(Level level, BlockPos origin) {
        if (level == null || origin == null) {
            return false;
        }
        return runtime(level, origin).tripped;
    }

    public static boolean resetGrid(Level level, BlockPos origin) {
        if (level == null || origin == null) {
            return false;
        }
        LevelState levelState = state(level);
        GridSnapshot snapshot = resolveGridSnapshot(level, levelState, origin);
        boolean changed = levelState.trippedGrids.remove(snapshot.gridKey());
        levelState.runtimes.remove(snapshot.gridKey());
        return changed;
    }

    public static void markDirty(Level level, BlockPos changedPos) {
        if (level == null || changedPos == null) {
            return;
        }
        LevelState levelState = STATES.get(level);
        if (levelState == null) {
            return;
        }
        long changedPacked = changedPos.asLong();
        Long gridKey = levelState.gridKeyByPos.get(changedPacked);
        if (gridKey != null) {
            invalidateSnapshot(levelState, gridKey, levelState.gridSnapshotsByKey.get(gridKey));
            levelState.trippedGrids.remove(gridKey);
            return;
        }

        for (Map.Entry<Long, GridSnapshot> entry : new ArrayList<>(levelState.gridSnapshotsByKey.entrySet())) {
            GridSnapshot snapshot = entry.getValue();
            if (snapshot != null && snapshot.nodePositions().contains(changedPacked)) {
                invalidateSnapshot(levelState, entry.getKey(), snapshot);
                levelState.trippedGrids.remove(entry.getKey());
            }
        }
    }

    private static GridSnapshot scanGrid(Level level, BlockPos origin) {
        Set<Long> gridNodes = PowerNetworkManager.connectedComponent(level, origin);
        if (gridNodes.isEmpty()) {
            return new GridSnapshot(origin.asLong(), 0.0, 0, List.of(), List.of(), Set.of());
        }
        long minPos = origin.asLong();
        double productionPerTickMj = 0.0;
        int generatorCount = 0;
        List<GenericMachineBlockEntity> consumers = new ArrayList<>();
        List<GenericPowerStorageBlockEntity> storages = new ArrayList<>();

        for (long packed : gridNodes) {
            BlockPos pos = BlockPos.of(packed);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!isGridNode(blockEntity)) {
                continue;
            }
            if (packed < minPos) {
                minPos = packed;
            }
            if (blockEntity instanceof PowerEmitterBlockEntity) {
                if (PowerNetworkManager.connectionCount(level, pos) > 0) {
                    productionPerTickMj += PowerEmitterBlockEntity.outputPerTickMj();
                    generatorCount++;
                }
            }
            if (blockEntity instanceof GenericMachineBlockEntity machine) {
                consumers.add(machine);
            }
            if (blockEntity instanceof GenericPowerStorageBlockEntity storage) {
                storages.add(storage);
            }
        }
        return new GridSnapshot(minPos, productionPerTickMj, generatorCount, consumers, storages, gridNodes);
    }

    private static LevelState state(Level level) {
        LevelState levelState = STATES.computeIfAbsent(level, ignored -> new LevelState());
        long tick = level.getGameTime();
        if (levelState.tick != tick) {
            levelState.tick = tick;
            levelState.runtimes.clear();
        }
        return levelState;
    }

    private static GridRuntime runtime(Level level, BlockPos origin) {
        LevelState levelState = state(level);
        GridSnapshot snapshot = resolveGridSnapshot(level, levelState, origin);
        return levelState.runtimes.computeIfAbsent(snapshot.gridKey(), ignored -> solve(levelState, snapshot));
    }

    private static GridSnapshot resolveGridSnapshot(Level level, LevelState levelState, BlockPos origin) {
        long originPacked = origin.asLong();
        Long cachedGridKey = levelState.gridKeyByPos.get(originPacked);
        if (cachedGridKey != null) {
            GridSnapshot cached = levelState.gridSnapshotsByKey.get(cachedGridKey);
            if (cached != null && isSnapshotValid(level, cached)) {
                return cached;
            }
            invalidateSnapshot(levelState, cachedGridKey, cached);
        }

        GridSnapshot snapshot = scanGrid(level, origin);
        levelState.gridSnapshotsByKey.put(snapshot.gridKey(), snapshot);
        for (long nodePos : snapshot.nodePositions()) {
            levelState.gridKeyByPos.put(nodePos, snapshot.gridKey());
        }
        if (snapshot.nodePositions().isEmpty()) {
            levelState.gridKeyByPos.put(originPacked, snapshot.gridKey());
        }
        return snapshot;
    }

    private static boolean isSnapshotValid(Level level, GridSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        for (long packedPos : snapshot.nodePositions()) {
            BlockEntity blockEntity = level.getBlockEntity(BlockPos.of(packedPos));
            if (!isGridNode(blockEntity)) {
                return false;
            }
        }
        return true;
    }

    private static void invalidateSnapshot(LevelState levelState, long gridKey, GridSnapshot snapshot) {
        levelState.gridSnapshotsByKey.remove(gridKey);
        levelState.runtimes.remove(gridKey);
        if (snapshot != null) {
            for (long nodePos : snapshot.nodePositions()) {
                levelState.gridKeyByPos.remove(nodePos);
            }
        }
    }

    private static GridRuntime solve(LevelState levelState, GridSnapshot snapshot) {
        GridRuntime runtime = new GridRuntime(levelState.trippedGrids.contains(snapshot.gridKey()));

        double currentDemandPerTickMj = 0.0;
        double maxDemandPerTickMj = 0.0;
        double capacityPerTickMj = snapshot.productionPerTickMj();
        double storageStoredMj = 0.0;
        double storageCapacityMj = 0.0;
        for (GenericMachineBlockEntity machine : snapshot.consumers()) {
            boolean hasRecipe = machine.hasSelectedRecipeForPowerGrid();
            boolean canProcess = hasRecipe && machine.canProcessSelectedRecipeForPowerGrid();
            double activeDemand = machine.activeDemandPerTickMj();
            maxDemandPerTickMj += activeDemand;
            double demand = canProcess
                ? activeDemand
                : machine.standbyDemandPerTickMj();
            if (demand <= 0.0) {
                continue;
            }
            currentDemandPerTickMj += demand;
            if (canProcess) {
                runtime.poweredConsumers.add(machine.getBlockPos().asLong());
                runtime.remainingConsumerBudgetMj.put(machine.getBlockPos().asLong(), activeDemand);
            }
        }

        double totalAvailableStorageDischargeMj = 0.0;
        double totalAvailableStorageChargeMj = 0.0;
        for (GenericPowerStorageBlockEntity storage : snapshot.storages()) {
            storageStoredMj += storage.storedEnergyMj();
            storageCapacityMj += storage.capacityMj();
            totalAvailableStorageDischargeMj += storage.availableDischargePerTickMj();
            totalAvailableStorageChargeMj += storage.availableChargePerTickMj();
        }

        if (runtime.tripped) {
            runtime.poweredConsumers.clear();
            runtime.remainingConsumerBudgetMj.clear();
            runtime.stats = new PowerGridStats(
                PowerUnits.mjPerTickToMw(capacityPerTickMj),
                0.0,
                0.0,
                PowerUnits.mjPerTickToMw(maxDemandPerTickMj),
                PowerUnits.mjToMwh(storageStoredMj),
                PowerUnits.mjToMwh(storageCapacityMj),
                0.0,
                0.0,
                true,
                snapshot.generatorCount(),
                snapshot.consumers().size()
            );
            return runtime;
        }

        if (currentDemandPerTickMj <= capacityPerTickMj + totalAvailableStorageDischargeMj + 1.0E-9) {
            double deficitMj = Math.max(0.0, currentDemandPerTickMj - capacityPerTickMj);
            double chargedMj = 0.0;
            double dischargedMj = 0.0;
            if (deficitMj > 0.0) {
                dischargedMj = drainStorage(snapshot.storages(), deficitMj);
            } else {
                double surplusMj = Math.max(0.0, capacityPerTickMj - currentDemandPerTickMj);
                chargedMj = chargeStorage(snapshot.storages(), Math.min(surplusMj, totalAvailableStorageChargeMj));
            }
            double updatedStoredMj = storageStoredMj - dischargedMj + chargedMj;
            runtime.stats = new PowerGridStats(
                PowerUnits.mjPerTickToMw(capacityPerTickMj),
                snapshot.generatorCount() > 0 ? PowerUnits.mjPerTickToMw(capacityPerTickMj) : 0.0,
                PowerUnits.mjPerTickToMw(currentDemandPerTickMj),
                PowerUnits.mjPerTickToMw(maxDemandPerTickMj),
                PowerUnits.mjToMwh(updatedStoredMj),
                PowerUnits.mjToMwh(storageCapacityMj),
                PowerUnits.mjPerTickToMw(chargedMj),
                PowerUnits.mjPerTickToMw(dischargedMj),
                false,
                snapshot.generatorCount(),
                snapshot.consumers().size()
            );
            return runtime;
        }

        runtime.poweredConsumers.clear();
        runtime.remainingConsumerBudgetMj.clear();
        if (snapshot.generatorCount() > 0) {
            levelState.trippedGrids.add(snapshot.gridKey());
            runtime.tripped = true;
            runtime.stats = new PowerGridStats(
                PowerUnits.mjPerTickToMw(capacityPerTickMj),
                0.0,
                0.0,
                PowerUnits.mjPerTickToMw(maxDemandPerTickMj),
                PowerUnits.mjToMwh(storageStoredMj),
                PowerUnits.mjToMwh(storageCapacityMj),
                0.0,
                0.0,
                true,
                snapshot.generatorCount(),
                snapshot.consumers().size()
            );
            return runtime;
        }
        runtime.stats = new PowerGridStats(
            0.0,
            0.0,
            0.0,
            PowerUnits.mjPerTickToMw(maxDemandPerTickMj),
            PowerUnits.mjToMwh(storageStoredMj),
            PowerUnits.mjToMwh(storageCapacityMj),
            0.0,
            0.0,
            false,
            0,
            snapshot.consumers().size()
        );
        return runtime;
    }

    private static boolean isGridNode(BlockEntity blockEntity) {
        return blockEntity instanceof PowerConnectable;
    }

    private static double drainStorage(List<GenericPowerStorageBlockEntity> storages, double neededMj) {
        double remaining = Math.max(0.0, neededMj);
        double provided = 0.0;
        for (GenericPowerStorageBlockEntity storage : storages) {
            if (remaining <= 1.0E-9) {
                return provided;
            }
            double extracted = storage.discharge(remaining);
            provided += extracted;
            remaining -= extracted;
        }
        return provided;
    }

    private static double chargeStorage(List<GenericPowerStorageBlockEntity> storages, double availableMj) {
        double remaining = Math.max(0.0, availableMj);
        double accepted = 0.0;
        for (GenericPowerStorageBlockEntity storage : storages) {
            if (remaining <= 1.0E-9) {
                return accepted;
            }
            double charged = storage.charge(remaining);
            accepted += charged;
            remaining -= charged;
        }
        return accepted;
    }

    private record GridSnapshot(
        long gridKey,
        double productionPerTickMj,
        int generatorCount,
        List<GenericMachineBlockEntity> consumers,
        List<GenericPowerStorageBlockEntity> storages,
        Set<Long> nodePositions
    ) {
    }

    private static final class LevelState {
        private long tick = Long.MIN_VALUE;
        private final Map<Long, GridRuntime> runtimes = new HashMap<>();
        private final Map<Long, GridSnapshot> gridSnapshotsByKey = new HashMap<>();
        private final Map<Long, Long> gridKeyByPos = new HashMap<>();
        private final Set<Long> trippedGrids = new HashSet<>();
    }

    private static final class GridRuntime {
        private boolean tripped;
        private final Set<Long> poweredConsumers = new HashSet<>();
        private final Map<Long, Double> remainingConsumerBudgetMj = new HashMap<>();
        private PowerGridStats stats = PowerGridStats.EMPTY;

        private GridRuntime(boolean tripped) {
            this.tripped = tripped;
        }
    }
}
