package net.ledok.factory_ld.world.block.entity;

import java.util.List;

public final class PowerStorageSpecs {
    public static final PowerStorageSpec POWER_STORAGE = new PowerStorageSpec(
        "power_storage",
        100.0,
        100.0,
        100.0
    );

    public static final List<PowerStorageSpec> ALL = List.of(POWER_STORAGE);

    private PowerStorageSpecs() {
    }

    public static PowerStorageSpec require(String id) {
        for (PowerStorageSpec spec : ALL) {
            if (spec.id().equals(id)) {
                return spec;
            }
        }
        throw new IllegalStateException("Unknown power storage spec: " + id);
    }
}
