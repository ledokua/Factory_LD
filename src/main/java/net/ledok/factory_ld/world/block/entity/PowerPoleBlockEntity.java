package net.ledok.factory_ld.world.block.entity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.ledok.factory_ld.registry.ModBlockEntities;
import net.ledok.factory_ld.world.power.PowerConnectable;
import net.ledok.factory_ld.world.power.PowerGridManager;
import net.ledok.factory_ld.world.screen.PowerEmitterScreenData;
import net.ledok.factory_ld.world.screen.PowerEmitterScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PowerPoleBlockEntity extends BlockEntity implements PowerConnectable, ExtendedScreenHandlerFactory<PowerEmitterScreenData> {
    private final String poleId;
    private final int maxConnections;

    public PowerPoleBlockEntity(BlockPos pos, BlockState state, String poleId, int maxConnections) {
        super(ModBlockEntities.requirePowerPoleBlockEntityType(poleId), pos, state);
        this.poleId = poleId;
        this.maxConnections = maxConnections;
    }

    @Override
    public int maxPowerConnections() {
        return maxConnections;
    }

    @Override
    public Vec3 powerConnectionOffset() {
        return new Vec3(0.5, 1.0, 0.5);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factory_ld." + poleId);
    }

    @Override
    public PowerEmitterScreenData getScreenOpeningData(ServerPlayer player) {
        return new PowerEmitterScreenData(worldPosition);
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new PowerEmitterScreenHandler(syncId, playerInventory, new PowerEmitterScreenData(worldPosition));
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
