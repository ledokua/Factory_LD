package net.ledok.factory_ld.registry;

public final class ModFluidStorages {
    private ModFluidStorages() {
    }

    public static void register() {
        for (MachineDescriptors.Descriptor descriptor : MachineDescriptors.ALL) {
            descriptor.registerFluidStorage(ModBlockEntities.requireMachineBlockEntityType(descriptor.id()));
        }
    }
}
