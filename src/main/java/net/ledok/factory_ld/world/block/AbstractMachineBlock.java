package net.ledok.factory_ld.world.block;

import java.util.function.Supplier;

import net.ledok.factory_ld.world.block.entity.AbstractMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public abstract class AbstractMachineBlock<T extends AbstractMachineBlockEntity> extends Block implements EntityBlock {
    private final Supplier<BlockEntityType<T>> blockEntityTypeSupplier;
    private final BlockEntityType.BlockEntitySupplier<T> blockEntityFactory;
    private final ServerTicker<T> serverTicker;

    protected AbstractMachineBlock(
        Properties properties,
        Supplier<BlockEntityType<T>> blockEntityTypeSupplier,
        BlockEntityType.BlockEntitySupplier<T> blockEntityFactory,
        ServerTicker<T> serverTicker
    ) {
        super(properties);
        this.blockEntityTypeSupplier = blockEntityTypeSupplier;
        this.blockEntityFactory = blockEntityFactory;
        this.serverTicker = serverTicker;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return blockEntityFactory.create(pos, state);
    }

    @Override
    public <E extends BlockEntity> BlockEntityTicker<E> getTicker(Level level, BlockState state, BlockEntityType<E> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != blockEntityTypeSupplier.get()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> serverTicker.tick(lvl, pos, blockState, castBlockEntity(blockEntity));
    }

    @SuppressWarnings("unchecked")
    private T castBlockEntity(BlockEntity blockEntity) {
        return (T) blockEntity;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MenuProvider provider) {
            player.openMenu(provider);
        }
        return InteractionResult.CONSUME;
    }

    @FunctionalInterface
    protected interface ServerTicker<T extends BlockEntity> {
        void tick(Level level, BlockPos pos, BlockState state, T blockEntity);
    }
}
