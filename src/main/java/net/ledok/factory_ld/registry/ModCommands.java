package net.ledok.factory_ld.registry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.ledok.factory_ld.world.player.PlayerOverclockAccess;
import net.ledok.factory_ld.world.research.ResearchManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.brigadier.suggestion.SuggestionProvider;

public final class ModCommands {
    private ModCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register(ModCommands::registerCommands);
    }

    private static void registerCommands(
        CommandDispatcher<CommandSourceStack> dispatcher,
        net.minecraft.commands.CommandBuildContext registryAccess,
        net.minecraft.commands.Commands.CommandSelection environment
    ) {
        SuggestionProvider<CommandSourceStack> recipeSuggestions = (context, builder) -> {
            return net.minecraft.commands.SharedSuggestionProvider.suggest(
                context.getSource().getServer().getRecipeManager().getRecipeIds().map(ResourceLocation::toString), builder
            );
        };

        SuggestionProvider<CommandSourceStack> groupSuggestions = (context, builder) -> {
            return net.minecraft.commands.SharedSuggestionProvider.suggest(
                ResearchManager.getAllGroups(context.getSource().getLevel()), builder
            );
        };

        dispatcher.register(
            Commands.literal("factoryld")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("overclock")
                    .then(Commands.literal("unlock")
                        .then(Commands.argument("targets", EntityArgument.players())
                            .executes(context -> setOverclockUnlocked(
                                context.getSource(),
                                EntityArgument.getPlayers(context, "targets"),
                                true
                            ))))
                    .then(Commands.literal("lock")
                        .then(Commands.argument("targets", EntityArgument.players())
                            .executes(context -> setOverclockUnlocked(
                                context.getSource(),
                                EntityArgument.getPlayers(context, "targets"),
                                false
                            ))))
                )
                .then(Commands.literal("research")
                    .then(Commands.literal("unlock")
                        .then(Commands.argument("targets", EntityArgument.players())
                            .then(Commands.argument("recipe", ResourceLocationArgument.id())
                                .suggests(recipeSuggestions)
                                .executes(context -> unlockPlayers(context.getSource(),
                                    EntityArgument.getPlayers(context, "targets"),
                                    ResourceLocationArgument.getId(context, "recipe"))))))
                    .then(Commands.literal("lock")
                        .then(Commands.argument("targets", EntityArgument.players())
                            .then(Commands.argument("recipe", ResourceLocationArgument.id())
                                .suggests(recipeSuggestions)
                                .executes(context -> lockPlayers(context.getSource(),
                                    EntityArgument.getPlayers(context, "targets"),
                                    ResourceLocationArgument.getId(context, "recipe"))))))
                    .then(Commands.literal("unlock_group")
                        .then(Commands.argument("targets", EntityArgument.players())
                            .then(Commands.argument("group", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests(groupSuggestions)
                                .executes(context -> unlockGroupPlayers(context.getSource(),
                                    EntityArgument.getPlayers(context, "targets"),
                                    com.mojang.brigadier.arguments.StringArgumentType.getString(context, "group"))))))
                    .then(Commands.literal("lock_group")
                        .then(Commands.argument("targets", EntityArgument.players())
                            .then(Commands.argument("group", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests(groupSuggestions)
                                .executes(context -> lockGroupPlayers(context.getSource(),
                                    EntityArgument.getPlayers(context, "targets"),
                                    com.mojang.brigadier.arguments.StringArgumentType.getString(context, "group"))))))
                    .then(Commands.literal("unlock_global")
                        .then(Commands.argument("recipe", ResourceLocationArgument.id())
                            .suggests(recipeSuggestions)
                            .executes(context -> unlockGlobal(context.getSource(),
                                ResourceLocationArgument.getId(context, "recipe")))))
                    .then(Commands.literal("lock_global")
                        .then(Commands.argument("recipe", ResourceLocationArgument.id())
                            .suggests(recipeSuggestions)
                            .executes(context -> lockGlobal(context.getSource(),
                                ResourceLocationArgument.getId(context, "recipe")))))
                    .then(Commands.literal("unlock_group_global")
                        .then(Commands.argument("group", com.mojang.brigadier.arguments.StringArgumentType.word())
                            .suggests(groupSuggestions)
                            .executes(context -> unlockGroupGlobal(context.getSource(),
                                com.mojang.brigadier.arguments.StringArgumentType.getString(context, "group")))))
                    .then(Commands.literal("lock_group_global")
                        .then(Commands.argument("group", com.mojang.brigadier.arguments.StringArgumentType.word())
                            .suggests(groupSuggestions)
                            .executes(context -> lockGroupGlobal(context.getSource(),
                                com.mojang.brigadier.arguments.StringArgumentType.getString(context, "group")))))
                )
        );
    }

    private static int unlockPlayers(CommandSourceStack source, java.util.Collection<ServerPlayer> players, ResourceLocation recipeId) throws CommandSyntaxException {
        validateRecipe(source, recipeId);
        ServerLevel level = source.getLevel();
        for (ServerPlayer player : players) {
            ResearchManager.unlockPlayer(level, player, recipeId);
        }
        source.sendSuccess(() -> Component.literal("Unlocked " + recipeId + " for " + players.size() + " player(s)."), true);
        return players.size();
    }

    private static int lockPlayers(CommandSourceStack source, java.util.Collection<ServerPlayer> players, ResourceLocation recipeId) throws CommandSyntaxException {
        validateRecipe(source, recipeId);
        ServerLevel level = source.getLevel();
        for (ServerPlayer player : players) {
            ResearchManager.lockPlayer(level, player, recipeId);
        }
        source.sendSuccess(() -> Component.literal("Locked " + recipeId + " for " + players.size() + " player(s)."), true);
        return players.size();
    }

    private static int unlockGlobal(CommandSourceStack source, ResourceLocation recipeId) throws CommandSyntaxException {
        validateRecipe(source, recipeId);
        ResearchManager.unlockGlobal(source.getLevel(), recipeId);
        source.sendSuccess(() -> Component.literal("Globally unlocked " + recipeId + "."), true);
        return 1;
    }

    private static int lockGlobal(CommandSourceStack source, ResourceLocation recipeId) throws CommandSyntaxException {
        validateRecipe(source, recipeId);
        ResearchManager.lockGlobal(source.getLevel(), recipeId);
        source.sendSuccess(() -> Component.literal("Globally locked " + recipeId + "."), true);
        return 1;
    }

    private static int unlockGroupPlayers(CommandSourceStack source, java.util.Collection<ServerPlayer> players, String group) {
        ServerLevel level = source.getLevel();
        for (ServerPlayer player : players) {
            ResearchManager.unlockGroupPlayer(level, player, group);
        }
        source.sendSuccess(() -> Component.literal("Unlocked group " + group + " for " + players.size() + " player(s)."), true);
        return players.size();
    }

    private static int lockGroupPlayers(CommandSourceStack source, java.util.Collection<ServerPlayer> players, String group) {
        ServerLevel level = source.getLevel();
        for (ServerPlayer player : players) {
            ResearchManager.lockGroupPlayer(level, player, group);
        }
        source.sendSuccess(() -> Component.literal("Locked group " + group + " for " + players.size() + " player(s)."), true);
        return players.size();
    }

    private static int unlockGroupGlobal(CommandSourceStack source, String group) {
        ResearchManager.unlockGroupGlobal(source.getLevel(), group);
        source.sendSuccess(() -> Component.literal("Globally unlocked group " + group + "."), true);
        return 1;
    }

    private static int lockGroupGlobal(CommandSourceStack source, String group) {
        ResearchManager.lockGroupGlobal(source.getLevel(), group);
        source.sendSuccess(() -> Component.literal("Globally locked group " + group + "."), true);
        return 1;
    }

    private static void validateRecipe(CommandSourceStack source, ResourceLocation recipeId) throws CommandSyntaxException {
        if (source.getServer().getRecipeManager().byKey(recipeId).isEmpty()) {
            source.sendFailure(Component.literal("Unknown recipe: " + recipeId));
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
        }
    }

    private static int setOverclockUnlocked(CommandSourceStack source, java.util.Collection<ServerPlayer> players, boolean unlocked) {
        for (ServerPlayer player : players) {
            PlayerOverclockAccess.setUnlocked(player, unlocked);
        }
        String state = unlocked ? "unlocked" : "locked";
        source.sendSuccess(() -> Component.literal("Overclock " + state + " for " + players.size() + " player(s)."), true);
        return players.size();
    }
}
