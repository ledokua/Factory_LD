package net.ledok.factory_ld.world.block;

import net.ledok.factory_ld.registry.ModBlockEntities;
import net.ledok.factory_ld.world.block.entity.GenericPowerStorageBlockEntity;
import net.ledok.factory_ld.world.item.PowerLineToolItem;
import net.ledok.factory_ld.world.power.PowerGridManager;
import net.ledok.factory_ld.world.power.PowerNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class GenericPowerStorageBlock extends Block implements EntityBlock {
    private final String storageId;

    public GenericPowerStorageBlock(Properties properties, String storageId) {
        super(properties);
        this.storageId = storageId;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GenericPowerStorageBlockEntity(pos, state, storageId);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == ModBlockEntities.requirePowerStorageBlockEntityType(storageId)
            ? (lvl, pos, blockState, blockEntity) -> GenericPowerStorageBlockEntity.serverTick(
                lvl,
                pos,
                blockState,
                (GenericPowerStorageBlockEntity) blockEntity
            )
            : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (stack.getItem() instanceof PowerLineToolItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            PowerGridManager.markDirty(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide) {
            PowerNetworkManager.removeNode(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
