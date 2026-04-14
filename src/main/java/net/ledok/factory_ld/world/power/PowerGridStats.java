package net.ledok.factory_ld.world.power;

public record PowerGridStats(
    double capacityMw,
    double productionMw,
    double consumptionMw,
    double maxConsumptionMw,
    double storageStoredMwh,
    double storageCapacityMwh,
    double storageChargeMw,
    double storageDischargeMw,
    boolean tripped,
    int generatorCount,
    int consumerCount
) {
    public static final PowerGridStats EMPTY = new PowerGridStats(
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        false,
        0,
        0
    );
}
