package net.ledok.factory_ld.world.block;

import net.ledok.factory_ld.registry.ModBlockEntities;
import net.ledok.factory_ld.world.block.entity.PowerEmitterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PowerEmitterBlock extends Block implements EntityBlock {
    public PowerEmitterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PowerEmitterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == ModBlockEntities.POWER_EMITTER
            ? (lvl, pos, blockState, blockEntity) -> PowerEmitterBlockEntity.serverTick(lvl, pos, blockState, (PowerEmitterBlockEntity) blockEntity)
            : null;
    }
}
