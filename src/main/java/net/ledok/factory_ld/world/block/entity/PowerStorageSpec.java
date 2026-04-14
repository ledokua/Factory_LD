package net.ledok.factory_ld.world.block.entity;

public record PowerStorageSpec(
    String id,
    double capacityMwh,
    double maxChargeMw,
    double maxDischargeMw
) {
    public PowerStorageSpec {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Power storage id must not be blank");
        }
        if (capacityMwh <= 0.0) {
            throw new IllegalArgumentException("Power storage capacity must be > 0");
        }
        if (maxChargeMw < 0.0 || maxDischargeMw < 0.0) {
            throw new IllegalArgumentException("Power storage charge/discharge rates must be >= 0");
        }
    }
}
