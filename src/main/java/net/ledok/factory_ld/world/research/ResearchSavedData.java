package net.ledok.factory_ld.world.research;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class ResearchSavedData extends SavedData {
    public static final String FILE_ID = "factory_ld_research";
    private static final String TAG_GLOBAL = "Global";
    private static final String TAG_PLAYERS = "Players";

    private final Set<String> globalUnlocked = new HashSet<>();
    private final Map<UUID, Set<String>> playerUnlocked = new HashMap<>();

    public static SavedData.Factory<ResearchSavedData> factory() {
        return new SavedData.Factory<>(ResearchSavedData::new, ResearchSavedData::load, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    }

    public ResearchSavedData() {
    }

    public static ResearchSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ResearchSavedData data = new ResearchSavedData();
        ListTag global = tag.getList(TAG_GLOBAL, Tag.TAG_STRING);
        for (int i = 0; i < global.size(); i++) {
            data.globalUnlocked.add(global.getString(i));
        }

        CompoundTag players = tag.getCompound(TAG_PLAYERS);
        for (String key : players.getAllKeys()) {
            UUID uuid = UUID.fromString(key);
            ListTag list = players.getList(key, Tag.TAG_STRING);
            Set<String> recipes = new HashSet<>();
            for (int i = 0; i < list.size(); i++) {
                recipes.add(list.getString(i));
            }
            data.playerUnlocked.put(uuid, recipes);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag global = new ListTag();
        for (String id : globalUnlocked) {
            global.add(StringTag.valueOf(id));
        }
        tag.put(TAG_GLOBAL, global);

        CompoundTag players = new CompoundTag();
        for (Map.Entry<UUID, Set<String>> entry : playerUnlocked.entrySet()) {
            ListTag list = new ListTag();
            for (String id : entry.getValue()) {
                list.add(StringTag.valueOf(id));
            }
            players.put(entry.getKey().toString(), list);
        }
        tag.put(TAG_PLAYERS, players);
        return tag;
    }

    public Set<String> getGlobalUnlocked() {
        return globalUnlocked;
    }

    public Map<UUID, Set<String>> getPlayerUnlocked() {
        return playerUnlocked;
    }
}
