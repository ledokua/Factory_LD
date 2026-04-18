package net.ledok.factory_ld.world.block.entity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.ledok.factory_ld.registry.ModBlockEntities;
import net.ledok.factory_ld.world.screen.PowerEmitterScreenData;
import net.ledok.factory_ld.world.screen.PowerEmitterScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ledok.factory_ld.world.power.PowerConnectable;
import net.ledok.factory_ld.world.power.PowerGridManager;
import net.ledok.factory_ld.world.power.PowerGridStats;
import net.ledok.factory_ld.world.power.PowerUnits;
import net.minecraft.world.phys.Vec3;

public class PowerEmitterBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<PowerEmitterScreenData>, PowerConnectable {
    private static final double OUTPUT_MW = 30.0;
    private static final double OUTPUT_PER_TICK_MJ = PowerUnits.mwToMjPerTick(OUTPUT_MW);

    public PowerEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_EMITTER, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PowerEmitterBlockEntity blockEntity) {
        blockEntity.tickServer();
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

    private void tickServer() {
        if (level == null || level.isClientSide) {
            return;
        }
        // Grid power is consumed by connected machines via PowerGridManager.
    }

    public static double outputPerTickMj() {
        return OUTPUT_PER_TICK_MJ;
    }

    @Override
    public int maxPowerConnections() {
        return 1;
    }

    @Override
    public Vec3 powerConnectionOffset() {
        return new Vec3(0.5, 0.9, 0.5);
    }

    public PowerGridStats getPowerGridStats() {
        if (level == null) {
            return PowerGridStats.EMPTY;
        }
        return PowerGridManager.getStats(level, worldPosition);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factory_ld.power_emitter");
    }

    @Override
    public PowerEmitterScreenData getScreenOpeningData(ServerPlayer player) {
        return new PowerEmitterScreenData(worldPosition);
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new PowerEmitterScreenHandler(syncId, playerInventory, new PowerEmitterScreenData(worldPosition));
    }
}
