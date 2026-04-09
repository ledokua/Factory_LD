package net.ledok.factory_ld.world.block.entity;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public interface OverclockMachineEntity {
    void setSelectedRecipeId(ResourceLocation id);

    void setClockSpeedPercent(double percent);

    double getMaxClockSpeedPercent();

    NonNullList<ItemStack> getItems();

    int getShardSlotStart();

    int getShardSlotCount();

    void setChanged();
}
