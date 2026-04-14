package net.ledok.factory_ld.world.block.entity;

public record MachineSpec(
    String id,
    String recipeTypeId,
    double basePowerMw,
    double energyCapacityMj,
    int itemInputSlots,
    int itemOutputSlots,
    boolean overclockEnabled,
    int fluidInputTanks,
    int fluidOutputTanks,
    long fluidTankCapacityDroplets
) {
    public MachineSpec {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Machine id must not be blank");
        }
        if (recipeTypeId == null || recipeTypeId.isBlank()) {
            throw new IllegalArgumentException("Recipe type id must not be blank");
        }
        if (basePowerMw < 0.0) {
            throw new IllegalArgumentException("Base power must be >= 0");
        }
        if (energyCapacityMj <= 0.0) {
            throw new IllegalArgumentException("Energy capacity must be > 0");
        }
        if (itemInputSlots < 0 || itemOutputSlots < 0) {
            throw new IllegalArgumentException("Item slot counts must be >= 0");
        }
        if (fluidInputTanks < 0 || fluidOutputTanks < 0) {
            throw new IllegalArgumentException("Fluid tank counts must be >= 0");
        }
        if ((fluidInputTanks > 0 || fluidOutputTanks > 0) && fluidTankCapacityDroplets <= 0L) {
            throw new IllegalArgumentException("Fluid tank capacity must be > 0 when fluid tanks exist");
        }
        if (fluidInputTanks == 0 && fluidOutputTanks == 0 && fluidTankCapacityDroplets != 0L) {
            throw new IllegalArgumentException("Fluid tank capacity must be 0 when machine has no fluid tanks");
        }
    }

    public int totalItemSlots() {
        return itemInputSlots + itemOutputSlots + shardSlots();
    }

    public int shardSlots() {
        return overclockEnabled ? 3 : 0;
    }
}
