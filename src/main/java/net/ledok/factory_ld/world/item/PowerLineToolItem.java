package net.ledok.factory_ld.world.item;

import net.ledok.factory_ld.world.power.PowerConnectable;
import net.ledok.factory_ld.world.power.PowerNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PowerLineToolItem extends Item {
    private static final Map<UUID, PendingLink> PENDING = new HashMap<>();

    public PowerLineToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.FAIL;
        }

        BlockPos clickedPos = context.getClickedPos();
        if (!(level.getBlockEntity(clickedPos) instanceof PowerConnectable connectable)) {
            player.displayClientMessage(Component.translatable("item.factory_ld.power_line_tool.not_connector"), true);
            clearPending(player);
            return InteractionResult.CONSUME;
        }

        PendingLink pending = PENDING.get(player.getUUID());
        if (pending == null || !pending.levelKey().equals(level.dimension())) {
            PENDING.put(player.getUUID(), new PendingLink(level.dimension(), clickedPos));
            player.displayClientMessage(
                Component.translatable(
                    "item.factory_ld.power_line_tool.selected",
                    clickedPos.toShortString(),
                    PowerNetworkManager.connectionCount(level, clickedPos),
                    connectable.maxPowerConnections()
                ),
                true
            );
            return InteractionResult.CONSUME;
        }

        BlockPos firstPos = pending.pos();
        if (firstPos.equals(clickedPos)) {
            clearPending(player);
            player.displayClientMessage(Component.translatable("item.factory_ld.power_line_tool.cleared"), true);
            return InteractionResult.CONSUME;
        }

        PowerNetworkManager.ToggleResult result = PowerNetworkManager.toggleConnection(level, firstPos, clickedPos);
        clearPending(player);
        switch (result) {
            case CONNECTED -> player.displayClientMessage(Component.translatable("item.factory_ld.power_line_tool.connected"), true);
            case DISCONNECTED -> player.displayClientMessage(Component.translatable("item.factory_ld.power_line_tool.disconnected"), true);
            case INVALID -> player.displayClientMessage(Component.translatable("item.factory_ld.power_line_tool.invalid"), true);
        }
        return InteractionResult.CONSUME;
    }

    private static void clearPending(ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }

    private record PendingLink(ResourceKey<Level> levelKey, BlockPos pos) {
    }
}
