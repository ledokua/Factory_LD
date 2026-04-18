package net.ledok.factory_ld.world.power;

import net.ledok.factory_ld.config.FactoryLdConfig;
import net.ledok.factory_ld.registry.ModNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PowerNetworkManager {
    private PowerNetworkManager() {
    }

    public enum ToggleResult {
        CONNECTED,
        DISCONNECTED,
        TOO_FAR,
        INVALID
    }

    public static ToggleResult toggleConnection(Level level, BlockPos a, BlockPos b) {
        if (!(level instanceof ServerLevel serverLevel) || a == null || b == null || a.equals(b)) {
            return ToggleResult.INVALID;
        }
        PowerConnectable aNode = connectableAt(level, a);
        PowerConnectable bNode = connectableAt(level, b);
        if (aNode == null || bNode == null) {
            return ToggleResult.INVALID;
        }
        int maxDistance = Math.max(1, FactoryLdConfig.load().maxPowerLinkDistance());
        double maxDistanceSq = (double) maxDistance * maxDistance;
        if (a.distSqr(b) > maxDistanceSq) {
            return ToggleResult.TOO_FAR;
        }
        PowerNetworkSavedData data = get(serverLevel);
        long pa = a.asLong();
        long pb = b.asLong();
        Set<Long> aLinks = data.adjacency().computeIfAbsent(pa, ignored -> new HashSet<>());
        Set<Long> bLinks = data.adjacency().computeIfAbsent(pb, ignored -> new HashSet<>());

        if (aLinks.contains(pb) && bLinks.contains(pa)) {
            aLinks.remove(pb);
            bLinks.remove(pa);
            cleanupIfEmpty(data, pa);
            cleanupIfEmpty(data, pb);
            data.setDirty();
            PowerGridManager.markDirty(level, a);
            ModNetworking.broadcastPowerLinkDelta(serverLevel, pa, pb, false);
            return ToggleResult.DISCONNECTED;
        }

        if (aLinks.size() >= aNode.maxPowerConnections() || bLinks.size() >= bNode.maxPowerConnections()) {
            return ToggleResult.INVALID;
        }
        aLinks.add(pb);
        bLinks.add(pa);
        data.setDirty();
        PowerGridManager.markDirty(level, a);
        ModNetworking.broadcastPowerLinkDelta(serverLevel, pa, pb, true);
        return ToggleResult.CONNECTED;
    }

    public static void removeNode(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || pos == null) {
            return;
        }
        PowerNetworkSavedData data = get(serverLevel);
        long packed = pos.asLong();
        Set<Long> links = data.adjacency().remove(packed);
        if (links == null) {
            return;
        }
        for (long other : links) {
            Set<Long> otherLinks = data.adjacency().get(other);
            if (otherLinks != null) {
                otherLinks.remove(packed);
                if (otherLinks.isEmpty()) {
                    data.adjacency().remove(other);
                }
            }
            ModNetworking.broadcastPowerLinkDelta(serverLevel, packed, other, false);
        }
        data.setDirty();
        PowerGridManager.markDirty(level, pos);
    }

    public static List<PowerLink> getLinks(ServerLevel level) {
        PowerNetworkSavedData data = get(level);
        List<PowerLink> links = new ArrayList<>();
        Set<PowerLink> dedupe = new HashSet<>();
        for (Map.Entry<Long, Set<Long>> entry : data.adjacency().entrySet()) {
            long a = entry.getKey();
            for (long b : entry.getValue()) {
                PowerLink link = a <= b ? new PowerLink(a, b) : new PowerLink(b, a);
                if (dedupe.add(link)) {
                    links.add(link);
                }
            }
        }
        return links;
    }

    public static Set<Long> connectedComponent(Level level, BlockPos origin) {
        if (level == null || origin == null) {
            return Set.of();
        }
        PowerConnectable originNode = connectableAt(level, origin);
        if (originNode == null) {
            return Set.of();
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return Set.of(origin.asLong());
        }
        PowerNetworkSavedData data = get(serverLevel);
        long start = origin.asLong();
        Set<Long> result = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            long current = queue.removeFirst();
            if (!result.add(current)) {
                continue;
            }
            BlockPos currentPos = BlockPos.of(current);
            if (connectableAt(level, currentPos) == null) {
                continue;
            }
            Set<Long> links = data.adjacency().get(current);
            if (links == null) {
                continue;
            }
            for (long next : links) {
                if (!result.contains(next)) {
                    queue.add(next);
                }
            }
        }
        return result;
    }

    public static int connectionCount(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || pos == null) {
            return 0;
        }
        PowerNetworkSavedData data = get(serverLevel);
        Set<Long> links = data.adjacency().get(pos.asLong());
        return links == null ? 0 : links.size();
    }

    private static void cleanupIfEmpty(PowerNetworkSavedData data, long pos) {
        Set<Long> links = data.adjacency().get(pos);
        if (links != null && links.isEmpty()) {
            data.adjacency().remove(pos);
        }
    }

    private static PowerConnectable connectableAt(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof PowerConnectable connectable ? connectable : null;
    }

    private static PowerNetworkSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PowerNetworkSavedData.factory(), PowerNetworkSavedData.FILE_ID);
    }

    public record PowerLink(long a, long b) {
    }
}
