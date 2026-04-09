package net.ledok.factory_ld.registry;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.ledok.factory_ld.world.block.entity.AssemblerBlockEntity;
import net.ledok.factory_ld.world.block.entity.ConstructorBlockEntity;
import net.ledok.factory_ld.world.block.entity.RefineryBlockEntity;

public final class ModFluidStorages {
    private ModFluidStorages() {
    }

    public static void register() {
        FluidStorage.SIDED.registerForBlockEntity(
            (ConstructorBlockEntity blockEntity, net.minecraft.core.Direction direction) -> blockEntity.getFluidStorage(),
            ModBlockEntities.CONSTRUCTOR
        );
        FluidStorage.SIDED.registerForBlockEntity(
            (AssemblerBlockEntity blockEntity, net.minecraft.core.Direction direction) -> blockEntity.getFluidStorage(),
            ModBlockEntities.ASSEMBLER
        );
        FluidStorage.SIDED.registerForBlockEntity(
            (RefineryBlockEntity blockEntity, net.minecraft.core.Direction direction) -> blockEntity.getFluidStorage(),
            ModBlockEntities.REFINERY
        );
    }
}
