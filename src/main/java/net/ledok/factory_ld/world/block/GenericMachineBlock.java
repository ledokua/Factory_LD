package net.ledok.factory_ld.world.block;

import net.ledok.factory_ld.registry.ModBlockEntities;
import net.ledok.factory_ld.world.block.entity.GenericMachineBlockEntity;

public class GenericMachineBlock extends AbstractMachineBlock<GenericMachineBlockEntity> {
    public GenericMachineBlock(Properties properties, String machineId) {
        super(
            properties,
            () -> ModBlockEntities.requireMachineBlockEntityType(machineId),
            (pos, state) -> new GenericMachineBlockEntity(pos, state, machineId),
            GenericMachineBlockEntity::serverTick
        );
    }
}
