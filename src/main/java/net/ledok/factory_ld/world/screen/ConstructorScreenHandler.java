package net.ledok.factory_ld.world.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.ledok.factory_ld.registry.ModRecipes;
import net.ledok.factory_ld.registry.ModScreenHandlers;
import net.ledok.factory_ld.world.block.entity.ConstructorBlockEntity;
import net.ledok.factory_ld.world.player.PlayerOverclockAccess;
import net.ledok.factory_ld.world.recipe.ConstructorRecipe;
import net.ledok.factory_ld.world.research.ResearchManager;
import net.ledok.factory_ld.world.recipe.view.MachineItemStackView;
import net.ledok.factory_ld.world.recipe.view.MachineRecipeView;
import net.ledok.factory_ld.world.recipe.view.SimpleMachineRecipeView;

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

public class ConstructorScreenHandler extends AbstractContainerMenu implements MachineRecipeMenu {
    public static final int INPUT_SLOT_X = 12;
    public static final int INPUT_SLOT_Y = 102;
    public static final int OUTPUT_SLOT_X = 176;
    public static final int OUTPUT_SLOT_Y = 102;
    public static final int SHARD_SLOT_Y = 174;
    public static final int SHARD_SLOT_1_X = 180;
    public static final int SHARD_SLOT_2_X = 198;
    public static final int SHARD_SLOT_3_X = 216;
    public static final int PLAYER_INV_Y = 174;
    public static final int PLAYER_HOTBAR_Y = 232;

    private final ConstructorBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final Level level;
    private final Set<ResourceLocation> unlockedRecipes;
    private final boolean overclockUnlocked;
    private boolean machineSlotsActive = true;
    private boolean playerSlotsActive = true;

    public ConstructorScreenHandler(int syncId, Inventory playerInventory, ConstructorScreenData data) {
        this(syncId, playerInventory, getBlockEntity(playerInventory, data.pos()), data.unlockedRecipes(), data.overclockUnlocked());
    }

    public ConstructorScreenHandler(int syncId, Inventory playerInventory, ConstructorBlockEntity blockEntity) {
        this(syncId, playerInventory, blockEntity, getUnlockedFor(playerInventory), PlayerOverclockAccess.isUnlocked(playerInventory.player));
    }

    private ConstructorScreenHandler(int syncId, Inventory playerInventory, ConstructorBlockEntity blockEntity, List<ResourceLocation> unlocked, boolean overclockUnlocked) {
        super(ModScreenHandlers.CONSTRUCTOR, syncId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.level = playerInventory.player.level();
        this.unlockedRecipes = unlocked == null ? Set.of() : Set.copyOf(unlocked);
        this.overclockUnlocked = overclockUnlocked;

        addSlot(new InputSlot(blockEntity, ConstructorBlockEntity.INPUT_SLOT, INPUT_SLOT_X, INPUT_SLOT_Y));
        addSlot(new OutputSlot(blockEntity, ConstructorBlockEntity.OUTPUT_SLOT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y));
        addSlot(new ShardSlot(blockEntity, ConstructorBlockEntity.SHARD_SLOT_START, SHARD_SLOT_1_X, SHARD_SLOT_Y));
        addSlot(new ShardSlot(blockEntity, ConstructorBlockEntity.SHARD_SLOT_START + 1, SHARD_SLOT_2_X, SHARD_SLOT_Y));
        addSlot(new ShardSlot(blockEntity, ConstructorBlockEntity.SHARD_SLOT_START + 2, SHARD_SLOT_3_X, SHARD_SLOT_Y));

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

    @Override
    public List<MachineRecipeView> getAvailableRecipeViews() {
        List<RecipeHolder<ConstructorRecipe>> recipes = getAvailableRecipes();
        List<MachineRecipeView> views = new ArrayList<>(recipes.size());
        for (RecipeHolder<ConstructorRecipe> holder : recipes) {
            views.add(toView(holder.id(), holder.value()));
        }
        return views;
    }

    @Override
    public Optional<MachineRecipeView> getRecipeView(int recipeIndex) {
        List<RecipeHolder<ConstructorRecipe>> recipes = getAvailableRecipes();
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) {
            return Optional.empty();
        }
        RecipeHolder<ConstructorRecipe> holder = recipes.get(recipeIndex);
        return Optional.of(toView(holder.id(), holder.value()));
    }

    @Override
    public Optional<MachineRecipeView> getSelectedRecipeView() {
        ResourceLocation selected = getSelectedRecipeId();
        if (selected == null) {
            return Optional.empty();
        }
        List<RecipeHolder<ConstructorRecipe>> recipes = getAvailableRecipes();
        for (RecipeHolder<ConstructorRecipe> holder : recipes) {
            if (holder.id().equals(selected)) {
                return Optional.of(toView(holder.id(), holder.value()));
            }
        }
        return Optional.empty();
    }

    @Override
    public void setMachineSlotsActive(boolean active) {
        this.machineSlotsActive = active;
    }

    @Override
    public void setPlayerSlotsActive(boolean active) {
        this.playerSlotsActive = active;
    }

    public ConstructorBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public ResourceLocation getSelectedRecipeId() {
        return blockEntity.getSelectedRecipeId();
    }

    public Optional<ConstructorRecipe> getSelectedRecipe() {
        return blockEntity.getSelectedRecipe();
    }

    public boolean isOverclockUnlocked() {
        return overclockUnlocked;
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

        int machineEnd = ConstructorBlockEntity.SLOT_COUNT;
        int playerStart = machineEnd;
        int hotbarStart = playerStart + 27;
        int inventoryEnd = slots.size();

        if (!machineSlotsActive) {
            if (slot < machineEnd) {
                return ItemStack.EMPTY;
            }
            if (slot < hotbarStart) {
                if (!moveItemStackTo(originalStack, hotbarStart, inventoryEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!moveItemStackTo(originalStack, playerStart, hotbarStart, false)) {
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

        if (slot < machineEnd) {
            if (!moveItemStackTo(originalStack, machineEnd, inventoryEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(originalStack, 0, machineEnd, false)) {
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
                addSlot(new PlayerSlot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new PlayerSlot(playerInventory, slot, 8 + slot * 18, PLAYER_HOTBAR_Y));
        }
    }

    private MachineRecipeView toView(ResourceLocation id, ConstructorRecipe recipe) {
        List<MachineItemStackView> inputs = new ArrayList<>();
        for (ConstructorRecipe.InputEntry input : recipe.getItemInputs()) {
            ItemStack[] options = input.ingredient().getItems();
            if (options.length == 0) {
                continue;
            }
            ItemStack display = options[0].copy();
            display.setCount(1);
            inputs.add(new MachineItemStackView(display, input.count()));
        }

        List<MachineItemStackView> outputs = new ArrayList<>();
        for (ItemStack stack : recipe.getItemOutputs()) {
            ItemStack display = stack.copy();
            int amount = Math.max(1, display.getCount());
            display.setCount(1);
            outputs.add(new MachineItemStackView(display, amount));
        }

        return new SimpleMachineRecipeView(
            id,
            recipe.getCategory(),
            recipe.getRecipeName(),
            recipe.getRecipeNameKey(),
            recipe.getCraftTime(),
            inputs,
            List.of(),
            outputs,
            List.of()
        );
    }

    private class InputSlot extends Slot {
        private final ConstructorBlockEntity blockEntity;

        private InputSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
            this.blockEntity = (ConstructorBlockEntity) inventory;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ConstructorScreenHandler.this.machineSlotsActive && blockEntity.isValidInput(stack);
        }

        @Override
        public boolean isActive() {
            return ConstructorScreenHandler.this.machineSlotsActive;
        }
    }

    private class PlayerSlot extends Slot {
        private PlayerSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isActive() {
            return ConstructorScreenHandler.this.playerSlotsActive;
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

    private class ShardSlot extends Slot {
        private final ConstructorBlockEntity blockEntity;

        private ShardSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
            this.blockEntity = (ConstructorBlockEntity) inventory;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ConstructorScreenHandler.this.machineSlotsActive
                && ConstructorScreenHandler.this.overclockUnlocked
                && blockEntity.isValidShard(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return ConstructorScreenHandler.this.machineSlotsActive
                && ConstructorScreenHandler.this.overclockUnlocked
                && super.mayPickup(player);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean isActive() {
            return ConstructorScreenHandler.this.machineSlotsActive && ConstructorScreenHandler.this.overclockUnlocked;
        }
    }
}
