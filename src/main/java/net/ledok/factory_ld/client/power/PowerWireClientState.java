package net.ledok.factory_ld.client.power;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PowerWireClientState {
    private static final Map<ResourceKey<Level>, Map<Long, Set<Long>>> LINKS_BY_LEVEL = new HashMap<>();

    private PowerWireClientState() {
    }

    public static void applySnapshot(ResourceKey<Level> levelKey, long[] links) {
        Map<Long, Set<Long>> adjacency = new HashMap<>();
        for (int i = 0; i + 1 < links.length; i += 2) {
            long a = links[i];
            long b = links[i + 1];
            adjacency.computeIfAbsent(a, ignored -> new HashSet<>()).add(b);
            adjacency.computeIfAbsent(b, ignored -> new HashSet<>()).add(a);
        }
        LINKS_BY_LEVEL.put(levelKey, adjacency);
    }

    public static void applyDelta(ResourceKey<Level> levelKey, long a, long b, boolean connected) {
        Map<Long, Set<Long>> adjacency = LINKS_BY_LEVEL.computeIfAbsent(levelKey, ignored -> new HashMap<>());
        if (connected) {
            adjacency.computeIfAbsent(a, ignored -> new HashSet<>()).add(b);
            adjacency.computeIfAbsent(b, ignored -> new HashSet<>()).add(a);
            return;
        }
        removeOne(adjacency, a, b);
        removeOne(adjacency, b, a);
    }

    public static Set<Long> linksFor(Level level, long pos) {
        if (level == null) {
            return Set.of();
        }
        Map<Long, Set<Long>> adjacency = LINKS_BY_LEVEL.get(level.dimension());
        if (adjacency == null) {
            return Set.of();
        }
        Set<Long> links = adjacency.get(pos);
        if (links == null || links.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(links);
    }

    public static void clear() {
        LINKS_BY_LEVEL.clear();
    }

    private static void removeOne(Map<Long, Set<Long>> adjacency, long from, long to) {
        Set<Long> links = adjacency.get(from);
        if (links == null) {
            return;
        }
        links.remove(to);
        if (links.isEmpty()) {
            adjacency.remove(from);
        }
    }
}
