package net.ledok.factory_ld.world.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import net.ledok.factory_ld.world.block.entity.AbstractMachineBlockEntity;
import net.ledok.factory_ld.world.player.PlayerOverclockAccess;
import net.ledok.factory_ld.world.recipe.view.MachineRecipeView;
import net.ledok.factory_ld.world.research.ResearchManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public abstract class AbstractMachineScreenHandler<M extends AbstractMachineBlockEntity, R extends Recipe<?>>
    extends AbstractContainerMenu implements MachineRecipeMenu, MachineMenuContext {

    protected final M blockEntity;
    protected final Level level;

    private final ContainerLevelAccess access;
    private final Set<ResourceLocation> unlockedRecipes;
    private final boolean overclockUnlocked;
    private boolean machineSlotsActive = true;
    private boolean playerSlotsActive = true;

    protected AbstractMachineScreenHandler(
        MenuType<?> menuType,
        int syncId,
        Inventory playerInventory,
        M blockEntity,
        List<ResourceLocation> unlocked,
        boolean overclockUnlocked
    ) {
        super(menuType, syncId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.level = playerInventory.player.level();
        this.unlockedRecipes = unlocked == null ? Set.of() : Set.copyOf(unlocked);
        this.overclockUnlocked = overclockUnlocked;
    }

    protected static List<ResourceLocation> getUnlockedFor(Inventory playerInventory) {
        Player player = playerInventory.player;
        if (player instanceof ServerPlayer serverPlayer && player.level() instanceof ServerLevel serverLevel) {
            return new ArrayList<>(ResearchManager.getUnlocked(serverLevel, serverPlayer));
        }
        return List.of();
    }

    protected static <T extends AbstractMachineBlockEntity> T resolveBlockEntity(
        Inventory playerInventory,
        BlockPos pos,
        Class<T> type,
        String machineName
    ) {
        Level level = playerInventory.player.level();
        if (type.isInstance(level.getBlockEntity(pos))) {
            return type.cast(level.getBlockEntity(pos));
        }
        throw new IllegalStateException(machineName + " block entity not found at " + pos);
    }

    protected abstract RecipeType<R> recipeType();

    protected abstract MachineRecipeView toView(ResourceLocation id, R recipe);

    protected final Optional<R> getSelectedRecipeFromId() {
        if (level == null || getSelectedRecipeId() == null) {
            return Optional.empty();
        }
        Optional<RecipeHolder<?>> entry = level.getRecipeManager().byKey(getSelectedRecipeId());
        if (entry.isEmpty() || entry.get().value().getType() != recipeType()) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        R recipe = (R) entry.get().value();
        return Optional.of(recipe);
    }

    public final List<RecipeHolder<R>> getAvailableRecipes() {
        if (level == null) {
            return List.of();
        }
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<RecipeHolder<R>> recipes = new ArrayList<>(level.getRecipeManager().getAllRecipesFor((RecipeType) recipeType()));
        recipes = recipes.stream().filter(entry -> unlockedRecipes.contains(entry.id())).toList();
        List<RecipeHolder<R>> sorted = new ArrayList<>(recipes);
        sorted.sort(Comparator.comparing(entry -> entry.id().toString()));
        return sorted;
    }

    @Override
    public final List<MachineRecipeView> getAvailableRecipeViews() {
        List<RecipeHolder<R>> recipes = getAvailableRecipes();
        List<MachineRecipeView> views = new ArrayList<>(recipes.size());
        for (RecipeHolder<R> holder : recipes) {
            views.add(toView(holder.id(), holder.value()));
        }
        return views;
    }

    @Override
    public final Optional<MachineRecipeView> getRecipeView(int recipeIndex) {
        List<RecipeHolder<R>> recipes = getAvailableRecipes();
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) {
            return Optional.empty();
        }
        RecipeHolder<R> holder = recipes.get(recipeIndex);
        return Optional.of(toView(holder.id(), holder.value()));
    }

    @Override
    public final Optional<MachineRecipeView> getSelectedRecipeView() {
        ResourceLocation selected = getSelectedRecipeId();
        if (selected == null) {
            return Optional.empty();
        }
        List<RecipeHolder<R>> recipes = getAvailableRecipes();
        for (RecipeHolder<R> holder : recipes) {
            if (holder.id().equals(selected)) {
                return Optional.of(toView(holder.id(), holder.value()));
            }
        }
        return Optional.empty();
    }

    @Override
    public final void setMachineSlotsActive(boolean active) {
        this.machineSlotsActive = active;
    }

    @Override
    public final void setPlayerSlotsActive(boolean active) {
        this.playerSlotsActive = active;
    }

    protected final boolean machineSlotsActive() {
        return machineSlotsActive;
    }

    @Override
    public final M getMachineBlockEntity() {
        return blockEntity;
    }

    public final ResourceLocation getSelectedRecipeId() {
        return blockEntity.getSelectedRecipeId();
    }

    @Override
    public final boolean isOverclockUnlocked() {
        return overclockUnlocked;
    }

    @Override
    public final boolean clickMenuButton(Player player, int id) {
        List<RecipeHolder<R>> recipes = getAvailableRecipes();
        if (id == 0) {
            access.execute((level, pos) -> {
                if (level.getBlockEntity(pos) == blockEntity) {
                    blockEntity.setSelectedRecipeId(null);
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
            if (level.getBlockEntity(pos) == blockEntity) {
                blockEntity.setSelectedRecipeId(recipeId);
            }
        });
        return true;
    }

    @Override
    public final boolean stillValid(Player player) {
        return blockEntity.stillValid(player);
    }

    @Override
    public final ItemStack quickMoveStack(Player player, int slot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slotItem = slots.get(slot);
        if (slotItem == null || !slotItem.hasItem()) {
            return newStack;
        }
        ItemStack originalStack = slotItem.getItem();
        newStack = originalStack.copy();

        int machineEnd = blockEntity.getContainerSize();
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
            } else if (!moveItemStackTo(originalStack, playerStart, hotbarStart, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slot < machineEnd) {
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

    protected final void addPlayerInventorySlots(Inventory playerInventory, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(createPlayerSlot(playerInventory, col + row * 9 + 9, 8 + col * 18, y + row * 18));
            }
        }
    }

    protected final void addPlayerHotbarSlots(Inventory playerInventory, int y) {
        for (int slot = 0; slot < 9; slot++) {
            addSlot(createPlayerSlot(playerInventory, slot, 8 + slot * 18, y));
        }
    }

    protected final Slot createPlayerSlot(Container inventory, int index, int x, int y) {
        return new Slot(inventory, index, x, y) {
            @Override
            public boolean isActive() {
                return playerSlotsActive;
            }
        };
    }

    protected final Slot createOutputSlot(Container inventory, int index, int x, int y) {
        return new Slot(inventory, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean isActive() {
                return machineSlotsActive;
            }
        };
    }

    protected final Slot createShardSlot(Container inventory, int index, int x, int y, Predicate<ItemStack> validator) {
        return new Slot(inventory, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return machineSlotsActive && overclockUnlocked && validator.test(stack);
            }

            @Override
            public boolean mayPickup(Player player) {
                return machineSlotsActive && overclockUnlocked && super.mayPickup(player);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean isActive() {
                return machineSlotsActive && overclockUnlocked;
            }
        };
    }
}
