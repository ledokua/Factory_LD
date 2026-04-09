package net.ledok.factory_ld.world.block.entity;

import net.ledok.factory_ld.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PowerEmitterBlockEntity extends BlockEntity {
    private static final double OUTPUT_PER_TICK_MJ = 2.0;

    public PowerEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_EMITTER, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PowerEmitterBlockEntity blockEntity) {
        blockEntity.tickServer();
    }

    private void tickServer() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (Direction direction : Direction.values()) {
            BlockEntity target = level.getBlockEntity(worldPosition.relative(direction));
            if (target instanceof ConstructorBlockEntity constructor) {
                constructor.addEnergy(OUTPUT_PER_TICK_MJ);
            }
            if (target instanceof AssemblerBlockEntity assembler) {
                assembler.addEnergy(OUTPUT_PER_TICK_MJ);
            }
            if (target instanceof RefineryBlockEntity refinery) {
                refinery.addEnergy(OUTPUT_PER_TICK_MJ);
            }
        }
    }
}
