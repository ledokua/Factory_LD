package net.ledok.factory_ld.world.research;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.ledok.factory_ld.config.FactoryLdConfig;
import net.ledok.factory_ld.FactoryLdMod;
import net.ledok.factory_ld.world.recipe.ConstructorRecipe;
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
        if (playerSet != null && playerSet.contains(recipeId.toString())) {
            return true;
        }
        Optional<String> group = getRecipeGroup(level, recipeId);
        if (group.isEmpty()) {
            return false;
        }
        if (data.getGlobalUnlockedGroups().contains(group.get())) {
            return true;
        }
        Set<String> playerGroups = data.getPlayerUnlockedGroups().get(player.getUUID());
        return playerGroups != null && playerGroups.contains(group.get());
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
        Set<String> unlockedGroups = new HashSet<>(data.getGlobalUnlockedGroups());
        Set<String> playerGroups = data.getPlayerUnlockedGroups().get(player.getUUID());
        if (playerGroups != null) {
            unlockedGroups.addAll(playerGroups);
        }
        if (!unlockedGroups.isEmpty()) {
            for (ResourceLocation id : getRecipesByAnyGroup(level, unlockedGroups)) {
                result.add(id);
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
        return allRecipes(level)
            .map(RecipeHolder::value)
            .filter(ConstructorRecipe.class::isInstance)
            .map(ConstructorRecipe.class::cast)
            .map(ConstructorRecipe::getResearchGroup)
            .collect(Collectors.toSet());
    }

    public static Set<ResourceLocation> getRecipesByGroup(ServerLevel level, String group) {
        return allRecipes(level)
            .filter(entry -> entry.value() instanceof ConstructorRecipe recipe && recipe.getResearchGroup().equals(group))
            .map(RecipeHolder::id)
            .collect(Collectors.toSet());
    }

    public static void unlockGroupGlobal(ServerLevel level, String group) {
        ResearchSavedData data = get(level);
        if (data.getGlobalUnlockedGroups().add(group)) {
            data.setDirty();
        }
    }

    public static void lockGroupGlobal(ServerLevel level, String group) {
        ResearchSavedData data = get(level);
        if (data.getGlobalUnlockedGroups().remove(group)) {
            data.setDirty();
        }
        for (ResourceLocation id : getRecipesByGroup(level, group)) {
            data.getGlobalUnlocked().remove(id.toString());
        }
        data.setDirty();
    }

    public static void unlockGroupPlayer(ServerLevel level, ServerPlayer player, String group) {
        ResearchSavedData data = get(level);
        Set<String> groups = data.getPlayerUnlockedGroups().computeIfAbsent(player.getUUID(), key -> new HashSet<>());
        if (groups.add(group)) {
            data.setDirty();
        }
    }

    public static void lockGroupPlayer(ServerLevel level, ServerPlayer player, String group) {
        ResearchSavedData data = get(level);
        Set<String> groups = data.getPlayerUnlockedGroups().get(player.getUUID());
        if (groups != null && groups.remove(group)) {
            if (groups.isEmpty()) {
                data.getPlayerUnlockedGroups().remove(player.getUUID());
            }
            data.setDirty();
        }
        Set<String> set = data.getPlayerUnlocked().get(player.getUUID());
        for (ResourceLocation id : getRecipesByGroup(level, group)) {
            if (set != null) {
                set.remove(id.toString());
            }
        }
        if (set != null && set.isEmpty()) {
            data.getPlayerUnlocked().remove(player.getUUID());
        }
        if (set != null) {
            data.setDirty();
        }
    }

    private static void ensureDefaults(ServerLevel level, ResearchSavedData data) {
        if (!data.getGlobalUnlocked().isEmpty()) {
            return;
        }
        FactoryLdConfig config = FactoryLdConfig.load();
        for (String group : config.defaultUnlockedGroups()) {
            data.getGlobalUnlockedGroups().add(group);
        }
        if (data.getGlobalUnlocked().isEmpty() && data.getGlobalUnlockedGroups().isEmpty()) {
            data.getGlobalUnlocked().add(FactoryLdMod.id("constructor_iron_plates").toString());
        }
        data.setDirty();
    }

    private static java.util.stream.Stream<RecipeHolder<?>> allRecipes(ServerLevel level) {
        return level.getRecipeManager().getRecipeIds()
            .map(level.getRecipeManager()::byKey)
            .flatMap(Optional::stream)
            .map(holder -> (RecipeHolder<?>)holder);
    }

    private static Optional<String> getRecipeGroup(ServerLevel level, ResourceLocation recipeId) {
        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(recipeId);
        if (holder.isEmpty()) {
            return Optional.empty();
        }
        if (holder.get().value() instanceof ConstructorRecipe recipe) {
            return Optional.of(recipe.getResearchGroup());
        }
        return Optional.empty();
    }

    private static Set<ResourceLocation> getRecipesByAnyGroup(ServerLevel level, Set<String> groups) {
        if (groups.isEmpty()) {
            return Set.of();
        }
        return allRecipes(level)
            .filter(entry -> entry.value() instanceof ConstructorRecipe recipe && groups.contains(recipe.getResearchGroup()))
            .map(RecipeHolder::id)
            .collect(Collectors.toSet());
    }
}
