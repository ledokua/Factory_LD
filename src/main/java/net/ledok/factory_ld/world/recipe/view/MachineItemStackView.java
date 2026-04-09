package net.ledok.factory_ld.world.recipe.view;

import net.minecraft.world.item.ItemStack;

public record MachineItemStackView(ItemStack stack, int amountPerCraft) {
    public MachineItemStackView {
        stack = stack == null ? ItemStack.EMPTY : stack.copy();
        amountPerCraft = Math.max(0, amountPerCraft);
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }
}
