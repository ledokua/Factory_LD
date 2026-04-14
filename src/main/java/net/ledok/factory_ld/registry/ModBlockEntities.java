package net.ledok.factory_ld.registry;

import net.ledok.factory_ld.FactoryLdMod;
import net.ledok.factory_ld.world.block.entity.GenericMachineBlockEntity;
import net.ledok.factory_ld.world.block.entity.PowerEmitterBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static BlockEntityType<GenericMachineBlockEntity> CONSTRUCTOR;
    public static BlockEntityType<GenericMachineBlockEntity> ASSEMBLER;
    public static BlockEntityType<GenericMachineBlockEntity> REFINERY;
    public static BlockEntityType<GenericMachineBlockEntity> BLENDER;
    public static BlockEntityType<GenericMachineBlockEntity> FOUNDRY;
    public static BlockEntityType<GenericMachineBlockEntity> MANUFACTURER;
    public static BlockEntityType<GenericMachineBlockEntity> SMELTER;
    public static BlockEntityType<PowerEmitterBlockEntity> POWER_EMITTER;

    private ModBlockEntities() {
    }

    public static void register() {
        if (CONSTRUCTOR != null) {
            return;
        }
        for (MachineDescriptors.Descriptor descriptor : MachineDescriptors.ALL) {
            Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                FactoryLdMod.id(descriptor.id()),
                descriptor.createBlockEntityType(ModBlocks.requireMachineBlock(descriptor.id()))
            );
        }
        POWER_EMITTER = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            FactoryLdMod.id("power_emitter"),
            BlockEntityType.Builder.of(PowerEmitterBlockEntity::new, ModBlocks.POWER_EMITTER).build(null)
        );
        CONSTRUCTOR = requireMachineBlockEntityType("constructor");
        ASSEMBLER = requireMachineBlockEntityType("assembler");
        REFINERY = requireMachineBlockEntityType("refinery");
        BLENDER = requireMachineBlockEntityType("blender");
        FOUNDRY = requireMachineBlockEntityType("foundry");
        MANUFACTURER = requireMachineBlockEntityType("manufacturer");
        SMELTER = requireMachineBlockEntityType("smelter");
    }

    @SuppressWarnings("unchecked")
    public static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> requireMachineBlockEntityType(String id) {
        return (BlockEntityType<T>) BuiltInRegistries.BLOCK_ENTITY_TYPE.getOptional(FactoryLdMod.id(id))
            .orElseThrow(() -> new IllegalStateException("Machine block entity type not registered: " + id));
    }
}
