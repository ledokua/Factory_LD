package net.ledok.factory_ld.world.power;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PowerLinkInteraction {
    private static final Map<UUID, PendingLink> PENDING = new HashMap<>();

    private PowerLinkInteraction() {
    }

    public static void handle(ServerPlayer player, BlockPos clickedPos) {
        Level level = player.level();
        if (!(level.getBlockEntity(clickedPos) instanceof PowerConnectable)) {
            player.displayClientMessage(Component.literal("Not a power connector."), true);
            return;
        }

        PendingLink pending = PENDING.get(player.getUUID());
        if (pending == null || !pending.levelKey().equals(level.dimension())) {
            PENDING.put(player.getUUID(), new PendingLink(level.dimension(), clickedPos));
            player.displayClientMessage(Component.literal("Selected connector: " + clickedPos.toShortString()), true);
            return;
        }

        if (pending.pos().equals(clickedPos)) {
            PENDING.remove(player.getUUID());
            player.displayClientMessage(Component.literal("Connection selection cleared."), true);
            return;
        }

        PowerNetworkManager.ToggleResult result = PowerNetworkManager.toggleConnection(level, pending.pos(), clickedPos);
        PENDING.remove(player.getUUID());
        switch (result) {
            case CONNECTED -> player.displayClientMessage(Component.literal("Power link connected."), true);
            case DISCONNECTED -> player.displayClientMessage(Component.literal("Power link disconnected."), true);
            case INVALID -> player.displayClientMessage(Component.literal("Cannot link these connectors."), true);
        }
    }

    private record PendingLink(ResourceKey<Level> levelKey, BlockPos pos) {
    }
}
