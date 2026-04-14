package net.ledok.factory_ld.world.block.entity;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;

import java.util.List;

public final class MachineSpecs {
    public static final MachineSpec CONSTRUCTOR = new MachineSpec(
        "constructor",
        "constructor",
        20.0,
        1000.0,
        1,
        1,
        true,
        0,
        0,
        0L
    );

    public static final MachineSpec ASSEMBLER = new MachineSpec(
        "assembler",
        "assembler",
        30.0,
        1000.0,
        2,
        1,
        true,
        0,
        0,
        0L
    );

    public static final MachineSpec REFINERY = new MachineSpec(
        "refinery",
        "refinery",
        45.0,
        1000.0,
        1,
        1,
        true,
        1,
        1,
        FluidConstants.BUCKET * 50L
    );

    public static final MachineSpec BLENDER = new MachineSpec(
        "blender",
        "blender",
        55.0,
        1000.0,
        2,
        1,
        true,
        2,
        1,
        FluidConstants.BUCKET * 50L
    );

    public static final MachineSpec FOUNDRY = new MachineSpec(
        "foundry",
        "foundry",
        40.0,
        1000.0,
        2,
        1,
        true,
        0,
        0,
        0L
    );

    public static final MachineSpec MANUFACTURER = new MachineSpec(
        "manufacturer",
        "manufacturer",
        75.0,
        1000.0,
        4,
        1,
        true,
        0,
        0,
        0L
    );

    public static final MachineSpec SMELTER = new MachineSpec(
        "smelter",
        "smelter",
        15.0,
        1000.0,
        1,
        1,
        true,
        0,
        0,
        0L
    );

    public static final List<MachineSpec> ALL = List.of(
        CONSTRUCTOR,
        ASSEMBLER,
        REFINERY,
        BLENDER,
        FOUNDRY,
        MANUFACTURER,
        SMELTER
    );

    private MachineSpecs() {
    }

    public static MachineSpec require(String id) {
        for (MachineSpec spec : ALL) {
            if (spec.id().equals(id)) {
                return spec;
            }
        }
        throw new IllegalStateException("Unknown machine spec: " + id);
    }
}
