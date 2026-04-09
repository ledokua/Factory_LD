package net.ledok.factory_ld.registry;

import net.ledok.factory_ld.FactoryLdMod;
import net.ledok.factory_ld.world.block.entity.AssemblerBlockEntity;
import net.ledok.factory_ld.world.block.entity.ConstructorBlockEntity;
import net.ledok.factory_ld.world.block.entity.PowerEmitterBlockEntity;
import net.ledok.factory_ld.world.block.entity.RefineryBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final BlockEntityType<ConstructorBlockEntity> CONSTRUCTOR = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        FactoryLdMod.id("constructor"),
        BlockEntityType.Builder.of(ConstructorBlockEntity::new, ModBlocks.CONSTRUCTOR).build(null)
    );
    public static final BlockEntityType<AssemblerBlockEntity> ASSEMBLER = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        FactoryLdMod.id("assembler"),
        BlockEntityType.Builder.of(AssemblerBlockEntity::new, ModBlocks.ASSEMBLER).build(null)
    );
    public static final BlockEntityType<RefineryBlockEntity> REFINERY = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        FactoryLdMod.id("refinery"),
        BlockEntityType.Builder.of(RefineryBlockEntity::new, ModBlocks.REFINERY).build(null)
    );
    public static final BlockEntityType<PowerEmitterBlockEntity> POWER_EMITTER = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        FactoryLdMod.id("power_emitter"),
        BlockEntityType.Builder.of(PowerEmitterBlockEntity::new, ModBlocks.POWER_EMITTER).build(null)
    );

    private ModBlockEntities() {
    }

    public static void register() {
    }
}
