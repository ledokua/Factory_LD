package net.ledok.factory_ld.client.render;

import net.ledok.factory_ld.registry.MachineDescriptors;
import net.ledok.factory_ld.registry.ModBlockEntities;
import net.ledok.factory_ld.registry.PowerPoleDescriptors;
import net.ledok.factory_ld.registry.PowerStorageDescriptors;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PowerWireRenderers {
    private PowerWireRenderers() {
    }

    public static void register() {
        for (MachineDescriptors.Descriptor descriptor : MachineDescriptors.ALL) {
            registerType(ModBlockEntities.requireMachineBlockEntityType(descriptor.id()));
        }
        for (PowerStorageDescriptors.Descriptor descriptor : PowerStorageDescriptors.ALL) {
            registerType(ModBlockEntities.requirePowerStorageBlockEntityType(descriptor.id()));
        }
        for (PowerPoleDescriptors.Descriptor descriptor : PowerPoleDescriptors.ALL) {
            registerType(ModBlockEntities.requirePowerPoleBlockEntityType(descriptor.id()));
        }
        registerType(ModBlockEntities.POWER_EMITTER);
    }

    private static <T extends BlockEntity> void registerType(net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        BlockEntityRenderers.register(type, PowerWireBlockEntityRenderer::new);
    }
}
