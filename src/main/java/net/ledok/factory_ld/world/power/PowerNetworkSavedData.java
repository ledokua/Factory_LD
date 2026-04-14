package net.ledok.factory_ld.world.power;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PowerNetworkSavedData extends SavedData {
    public static final String FILE_ID = "factory_ld_power_network";
    private static final String TAG_NODES = "Nodes";
    private static final String TAG_POS = "Pos";
    private static final String TAG_LINKS = "Links";

    private final Map<Long, Set<Long>> adjacency = new HashMap<>();

    public static SavedData.Factory<PowerNetworkSavedData> factory() {
        return new SavedData.Factory<>(PowerNetworkSavedData::new, PowerNetworkSavedData::load, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    }

    public static PowerNetworkSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        PowerNetworkSavedData data = new PowerNetworkSavedData();
        ListTag nodes = tag.getList(TAG_NODES, Tag.TAG_COMPOUND);
        for (int i = 0; i < nodes.size(); i++) {
            CompoundTag nodeTag = nodes.getCompound(i);
            long pos = nodeTag.getLong(TAG_POS);
            ListTag links = nodeTag.getList(TAG_LINKS, Tag.TAG_LONG);
            Set<Long> set = data.adjacency.computeIfAbsent(pos, ignored -> new HashSet<>());
            for (int j = 0; j < links.size(); j++) {
                set.add(((LongTag) links.get(j)).getAsLong());
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag nodes = new ListTag();
        for (Map.Entry<Long, Set<Long>> entry : adjacency.entrySet()) {
            CompoundTag nodeTag = new CompoundTag();
            nodeTag.putLong(TAG_POS, entry.getKey());
            ListTag links = new ListTag();
            for (Long link : entry.getValue()) {
                links.add(LongTag.valueOf(link));
            }
            nodeTag.put(TAG_LINKS, links);
            nodes.add(nodeTag);
        }
        tag.put(TAG_NODES, nodes);
        return tag;
    }

    public Map<Long, Set<Long>> adjacency() {
        return adjacency;
    }
}
