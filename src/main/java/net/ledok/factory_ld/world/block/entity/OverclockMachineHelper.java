package net.ledok.factory_ld.world.block.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class OverclockMachineHelper {
    private OverclockMachineHelper() {
    }

    public static void applyPastedSettings(
        Inventory inventory,
        ResourceLocation recipeId,
        int scaledClock,
        boolean overclockUnlocked,
        OverclockMachineEntity machine
    ) {
        machine.setSelectedRecipeId(recipeId);
        double requestedClock = scaledClock / 10000.0;
        if (!overclockUnlocked) {
            machine.setClockSpeedPercent(Math.min(requestedClock, 100.0));
            return;
        }
        fillShardSlotsFromPlayer(inventory, machine, requiredShardSlotsFor(requestedClock, machine.getShardSlotCount()));
        machine.setClockSpeedPercent(Math.min(requestedClock, machine.getMaxClockSpeedPercent()));
    }

    public static int requiredShardSlotsFor(double clockPercent, int maxShardSlots) {
        if (clockPercent <= 100.0) {
            return 0;
        }
        return Math.min(maxShardSlots, (int)Math.ceil((clockPercent - 100.0) / 50.0));
    }

    public static void fillShardSlotsFromPlayer(Inventory inventory, OverclockMachineEntity machine, int requiredSlots) {
        int occupied = 0;
        for (int i = 0; i < machine.getShardSlotCount(); i++) {
            ItemStack machineStack = machine.getItems().get(machine.getShardSlotStart() + i);
            if (!machineStack.isEmpty()) {
                occupied++;
            }
        }
        int needed = Math.max(0, requiredSlots - occupied);
        if (needed <= 0) {
            return;
        }

        for (int slot = 0; slot < inventory.getContainerSize() && needed > 0; slot++) {
            ItemStack invStack = inventory.getItem(slot);
            if (invStack.isEmpty() || !invStack.is(Items.AMETHYST_SHARD)) {
                continue;
            }
            while (!invStack.isEmpty() && needed > 0) {
                int targetShardSlot = firstEmptyShardSlot(machine);
                if (targetShardSlot < 0) {
                    return;
                }
                machine.getItems().set(targetShardSlot, new ItemStack(Items.AMETHYST_SHARD, 1));
                invStack.shrink(1);
                needed--;
            }
        }
        machine.setChanged();
    }

    private static int firstEmptyShardSlot(OverclockMachineEntity machine) {
        for (int i = 0; i < machine.getShardSlotCount(); i++) {
            int index = machine.getShardSlotStart() + i;
            if (machine.getItems().get(index).isEmpty()) {
                return index;
            }
        }
        return -1;
    }
}
