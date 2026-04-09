package net.ledok.factory_ld.world.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class PlayerOverclockAccess {
    private static final String OVERCLOCK_UNLOCKED_TAG = "factory_ld.overclock_unlocked";

    private PlayerOverclockAccess() {
    }

    public static boolean isUnlocked(Player player) {
        return player.getTags().contains(OVERCLOCK_UNLOCKED_TAG);
    }

    public static void setUnlocked(ServerPlayer player, boolean unlocked) {
        if (unlocked) {
            player.addTag(OVERCLOCK_UNLOCKED_TAG);
        } else {
            player.removeTag(OVERCLOCK_UNLOCKED_TAG);
        }
    }
}
