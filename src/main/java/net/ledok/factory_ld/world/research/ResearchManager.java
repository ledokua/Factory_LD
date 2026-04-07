package net.ledok.factory_ld.world.research;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.ledok.factory_ld.config.FactoryLdConfig;
import net.ledok.factory_ld.FactoryLdMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;

public final class ResearchManager {
    private ResearchManager() {
    }

    public static ResearchSavedData get(ServerLevel level) {
        ResearchSavedData data = level.getDataStorage().computeIfAbsent(ResearchSavedData.factory(), ResearchSavedData.FILE_ID);
        ensureDefaults(level, data);
        return data;
    }

    public static boolean isUnlocked(ServerLevel level, ServerPlayer player, ResourceLocation recipeId) {
        ResearchSavedData data = get(level);
        if (data.getGlobalUnlocked().contains(recipeId.toString())) {
            return true;
        }
        Set<String> playerSet = data.getPlayerUnlocked().get(player.getUUID());
        return playerSet != null && playerSet.contains(recipeId.toString());
    }

    public static Set<ResourceLocation> getUnlocked(ServerLevel level, ServerPlayer player) {
        ResearchSavedData data = get(level);
        Set<ResourceLocation> result = new HashSet<>();
        for (String id : data.getGlobalUnlocked()) {
            Optional.ofNullable(ResourceLocation.tryParse(id)).ifPresent(result::add);
        }
        Set<String> playerSet = data.getPlayerUnlocked().get(player.getUUID());
        if (playerSet != null) {
            for (String id : playerSet) {
                Optional.ofNullable(ResourceLocation.tryParse(id)).ifPresent(result::add);
            }
        }
        return result;
    }

    public static void unlockGlobal(ServerLevel level, ResourceLocation recipeId) {
        ResearchSavedData data = get(level);
        if (data.getGlobalUnlocked().add(recipeId.toString())) {
            data.setDirty();
        }
    }

    public static void lockGlobal(ServerLevel level, ResourceLocation recipeId) {
        ResearchSavedData data = get(level);
        if (data.getGlobalUnlocked().remove(recipeId.toString())) {
            data.setDirty();
        }
    }

    public static void unlockPlayer(ServerLevel level, ServerPlayer player, ResourceLocation recipeId) {
        ResearchSavedData data = get(level);
        data.getPlayerUnlocked()
            .computeIfAbsent(player.getUUID(), key -> new HashSet<>())
            .add(recipeId.toString());
        data.setDirty();
    }

    public static void lockPlayer(ServerLevel level, ServerPlayer player, ResourceLocation recipeId) {
        ResearchSavedData data = get(level);
        Set<String> playerSet = data.getPlayerUnlocked().get(player.getUUID());
        if (playerSet != null && playerSet.remove(recipeId.toString())) {
            if (playerSet.isEmpty()) {
                data.getPlayerUnlocked().remove(player.getUUID());
            }
            data.setDirty();
        }
    }

    public static Set<String> getAllGroups(ServerLevel level) {
        return level.getRecipeManager().getAllRecipesFor(net.ledok.factory_ld.registry.ModRecipes.CONSTRUCTOR_TYPE)
            .stream()
            .map(RecipeHolder::value)
            .map(net.ledok.factory_ld.world.recipe.ConstructorRecipe::getResearchGroup)
            .collect(Collectors.toSet());
    }

    public static Set<ResourceLocation> getRecipesByGroup(ServerLevel level, String group) {
        return level.getRecipeManager().getAllRecipesFor(net.ledok.factory_ld.registry.ModRecipes.CONSTRUCTOR_TYPE)
            .stream()
            .filter(entry -> entry.value().getResearchGroup().equals(group))
            .map(RecipeHolder::id)
            .collect(Collectors.toSet());
    }

    public static void unlockGroupGlobal(ServerLevel level, String group) {
        ResearchSavedData data = get(level);
        for (ResourceLocation id : getRecipesByGroup(level, group)) {
            data.getGlobalUnlocked().add(id.toString());
        }
        data.setDirty();
    }

    public static void lockGroupGlobal(ServerLevel level, String group) {
        ResearchSavedData data = get(level);
        for (ResourceLocation id : getRecipesByGroup(level, group)) {
            data.getGlobalUnlocked().remove(id.toString());
        }
        data.setDirty();
    }

    public static void unlockGroupPlayer(ServerLevel level, ServerPlayer player, String group) {
        ResearchSavedData data = get(level);
        Set<String> set = data.getPlayerUnlocked().computeIfAbsent(player.getUUID(), key -> new HashSet<>());
        for (ResourceLocation id : getRecipesByGroup(level, group)) {
            set.add(id.toString());
        }
        data.setDirty();
    }

    public static void lockGroupPlayer(ServerLevel level, ServerPlayer player, String group) {
        ResearchSavedData data = get(level);
        Set<String> set = data.getPlayerUnlocked().get(player.getUUID());
        if (set == null) {
            return;
        }
        for (ResourceLocation id : getRecipesByGroup(level, group)) {
            set.remove(id.toString());
        }
        if (set.isEmpty()) {
            data.getPlayerUnlocked().remove(player.getUUID());
        }
        data.setDirty();
    }

    private static void ensureDefaults(ServerLevel level, ResearchSavedData data) {
        if (!data.getGlobalUnlocked().isEmpty()) {
            return;
        }
        FactoryLdConfig config = FactoryLdConfig.load();
        for (String group : config.defaultUnlockedGroups()) {
            for (ResourceLocation id : getRecipesByGroup(level, group)) {
                data.getGlobalUnlocked().add(id.toString());
            }
        }
        if (data.getGlobalUnlocked().isEmpty()) {
            data.getGlobalUnlocked().add(FactoryLdMod.id("constructor_iron_plates").toString());
        }
        data.setDirty();
    }
}
