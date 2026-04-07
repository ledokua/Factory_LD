package net.ledok.factory_ld.registry;

import net.ledok.factory_ld.FactoryLdMod;
import net.ledok.factory_ld.world.block.entity.ConstructorBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final BlockEntityType<ConstructorBlockEntity> CONSTRUCTOR = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        FactoryLdMod.id("constructor"),
        BlockEntityType.Builder.of(ConstructorBlockEntity::new, ModBlocks.CONSTRUCTOR).build(null)
    );

    private ModBlockEntities() {
    }

    public static void register() {
    }
}
