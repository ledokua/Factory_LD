package net.ledok.factory_ld.world.block.entity;

import net.ledok.factory_ld.registry.ModBlockEntities;
import net.ledok.factory_ld.world.power.PowerConnectable;
import net.ledok.factory_ld.world.power.PowerGridManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PowerPoleBlockEntity extends BlockEntity implements PowerConnectable {
    private static final int MAX_CONNECTIONS = 4;

    public PowerPoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_POLE, pos, state);
    }

    @Override
    public int maxPowerConnections() {
        return MAX_CONNECTIONS;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level != null && !level.isClientSide) {
            PowerGridManager.markDirty(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            PowerGridManager.markDirty(level, worldPosition);
        }
        super.setRemoved();
    }
}
