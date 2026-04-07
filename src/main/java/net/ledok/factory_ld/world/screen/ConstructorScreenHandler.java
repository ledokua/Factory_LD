package net.ledok.factory_ld.world.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.ledok.factory_ld.registry.ModRecipes;
import net.ledok.factory_ld.registry.ModScreenHandlers;
import net.ledok.factory_ld.world.block.entity.ConstructorBlockEntity;
import net.ledok.factory_ld.world.recipe.ConstructorRecipe;
import net.ledok.factory_ld.world.research.ResearchManager;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ConstructorScreenHandler extends AbstractContainerMenu {
    private final ConstructorBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final Level level;
    private final Set<ResourceLocation> unlockedRecipes;
    private boolean machineSlotsActive = true;

    public ConstructorScreenHandler(int syncId, Inventory playerInventory, ConstructorScreenData data) {
        this(syncId, playerInventory, getBlockEntity(playerInventory, data.pos()), data.unlockedRecipes());
    }

    public ConstructorScreenHandler(int syncId, Inventory playerInventory, ConstructorBlockEntity blockEntity) {
        this(syncId, playerInventory, blockEntity, getUnlockedFor(playerInventory));
    }

    private ConstructorScreenHandler(int syncId, Inventory playerInventory, ConstructorBlockEntity blockEntity, List<ResourceLocation> unlocked) {
        super(ModScreenHandlers.CONSTRUCTOR, syncId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.level = playerInventory.player.level();
        this.unlockedRecipes = unlocked == null ? Set.of() : Set.copyOf(unlocked);

        addSlot(new InputSlot(blockEntity, ConstructorBlockEntity.INPUT_SLOT, 30, 34));
        addSlot(new OutputSlot(blockEntity, ConstructorBlockEntity.OUTPUT_SLOT, 90, 34));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private static List<ResourceLocation> getUnlockedFor(Inventory playerInventory) {
        Player player = playerInventory.player;
        if (player instanceof ServerPlayer serverPlayer && player.level() instanceof ServerLevel serverLevel) {
            return new ArrayList<>(ResearchManager.getUnlocked(serverLevel, serverPlayer));
        }
        return List.of();
    }

    private static ConstructorBlockEntity getBlockEntity(Inventory playerInventory, BlockPos pos) {
        Level level = playerInventory.player.level();
        if (level.getBlockEntity(pos) instanceof ConstructorBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("Constructor block entity not found at " + pos);
    }

    private List<RecipeHolder<ConstructorRecipe>> loadRecipes(Level level) {
        if (level == null) {
            return List.of();
        }
        RecipeManager manager = level.getRecipeManager();
        List<RecipeHolder<ConstructorRecipe>> recipes = new ArrayList<>(manager.getAllRecipesFor(ModRecipes.CONSTRUCTOR_TYPE));
        recipes = recipes.stream()
            .filter(entry -> unlockedRecipes.contains(entry.id()))
            .toList();
        List<RecipeHolder<ConstructorRecipe>> result = new ArrayList<>(recipes);
        result.sort(Comparator.comparing(entry -> entry.id().toString()));
        return result;
    }

    public List<RecipeHolder<ConstructorRecipe>> getAvailableRecipes() {
        return loadRecipes(level);
    }

    public void setMachineSlotsActive(boolean active) {
        this.machineSlotsActive = active;
    }

    public ResourceLocation getSelectedRecipeId() {
        return blockEntity.getSelectedRecipeId();
    }

    public Optional<ConstructorRecipe> getSelectedRecipe() {
        return blockEntity.getSelectedRecipe();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        List<RecipeHolder<ConstructorRecipe>> recipes = getAvailableRecipes();
        if (id == 0) {
            access.execute((level, pos) -> {
                if (level.getBlockEntity(pos) instanceof ConstructorBlockEntity be) {
                    be.setSelectedRecipeId(null);
                }
            });
            return true;
        }
        int recipeIndex = id - 1;
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) {
            return false;
        }
        ResourceLocation recipeId = recipes.get(recipeIndex).id();
        access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof ConstructorBlockEntity be) {
                be.setSelectedRecipeId(recipeId);
            }
        });
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slotItem = slots.get(slot);
        if (slotItem == null || !slotItem.hasItem()) {
            return newStack;
        }

        ItemStack originalStack = slotItem.getItem();
        newStack = originalStack.copy();

        int inventoryEnd = slots.size();
        if (slot < ConstructorBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(originalStack, ConstructorBlockEntity.SLOT_COUNT, inventoryEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(originalStack, 0, ConstructorBlockEntity.SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (originalStack.isEmpty()) {
            slotItem.set(ItemStack.EMPTY);
        } else {
            slotItem.setChanged();
        }
        return newStack;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 92 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(playerInventory, slot, 8 + slot * 18, 150));
        }
    }

    private class InputSlot extends Slot {
        private final ConstructorBlockEntity blockEntity;

        private InputSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
            this.blockEntity = (ConstructorBlockEntity) inventory;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return blockEntity.isValidInput(stack);
        }

        @Override
        public boolean isActive() {
            return ConstructorScreenHandler.this.machineSlotsActive;
        }
    }

    private class OutputSlot extends Slot {
        private OutputSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean isActive() {
            return ConstructorScreenHandler.this.machineSlotsActive;
        }
    }
}
