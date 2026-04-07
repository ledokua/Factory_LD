package net.ledok.factory_ld.world.block.entity;

import java.util.Optional;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.ledok.factory_ld.inventory.ImplementedInventory;
import net.ledok.factory_ld.registry.ModBlockEntities;
import net.ledok.factory_ld.world.recipe.ConstructorRecipe;
import net.ledok.factory_ld.world.research.ResearchManager;
import net.ledok.factory_ld.world.screen.ConstructorScreenData;
import net.ledok.factory_ld.world.screen.ConstructorScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ConstructorBlockEntity extends BlockEntity implements ImplementedInventory, ExtendedScreenHandlerFactory<ConstructorScreenData> {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int SLOT_COUNT = 2;
    private static final int DEFAULT_CRAFT_TIME = 80;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private ResourceLocation selectedRecipeId;
    private int progress;

    public ConstructorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONSTRUCTOR, pos, state);
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public Optional<ConstructorRecipe> getSelectedRecipe() {
        if (level == null || selectedRecipeId == null) {
            return Optional.empty();
        }
        Optional<RecipeHolder<?>> entry = level.getRecipeManager().byKey(selectedRecipeId);
        if (entry.isEmpty()) {
            return Optional.empty();
        }
        if (entry.get().value() instanceof ConstructorRecipe recipe) {
            return Optional.of(recipe);
        }
        return Optional.empty();
    }

    public ResourceLocation getSelectedRecipeId() {
        return selectedRecipeId;
    }

    public void setSelectedRecipeId(ResourceLocation id) {
        if (id != null) {
            Optional<RecipeHolder<?>> entry = level == null ? Optional.empty() : level.getRecipeManager().byKey(id);
            if (entry.isEmpty() || !(entry.get().value() instanceof ConstructorRecipe)) {
                id = null;
            }
        }
        if ((selectedRecipeId == null && id == null) || (selectedRecipeId != null && selectedRecipeId.equals(id))) {
            return;
        }
        selectedRecipeId = id;
        enforceInventoryValidity();
        setChanged();
    }

    public boolean isValidInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Optional<ConstructorRecipe> recipe = getSelectedRecipe();
        return recipe.isPresent() && recipe.get().getInput().test(stack);
    }

    private void enforceInventoryValidity() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockPos pos = getBlockPos();
        ItemStack inputStack = items.get(INPUT_SLOT);
        if (!inputStack.isEmpty() && !isValidInput(inputStack)) {
            items.set(INPUT_SLOT, ItemStack.EMPTY);
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, inputStack);
        }

        ItemStack outputStack = items.get(OUTPUT_SLOT);
        if (!outputStack.isEmpty()) {
            ItemStack expected = ItemStack.EMPTY;
            Optional<ConstructorRecipe> recipe = getSelectedRecipe();
            if (recipe.isPresent()) {
                expected = recipe.get().getOutput();
            }
            if (expected.isEmpty() || !ItemStack.isSameItemSameComponents(outputStack, expected)) {
                items.set(OUTPUT_SLOT, ItemStack.EMPTY);
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, outputStack);
            }
        }
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, ConstructorBlockEntity blockEntity) {
        blockEntity.tickServer();
    }

    private void tickServer() {
        if (level == null || level.isClientSide) {
            return;
        }

        ItemStack input = items.get(INPUT_SLOT);
        ItemStack output = items.get(OUTPUT_SLOT);
        Optional<ConstructorRecipe> recipeOpt = getSelectedRecipe();

        if (recipeOpt.isEmpty()) {
            progress = 0;
            return;
        }

        ConstructorRecipe recipe = recipeOpt.get();
        int inputCount = Math.max(1, recipe.getInputCount());
        if (input.isEmpty() || input.getCount() < inputCount || !recipe.getInput().test(input)) {
            progress = 0;
            return;
        }

        ItemStack result = recipe.assemble(new SingleRecipeInput(input), level.registryAccess());
        if (result.isEmpty()) {
            progress = 0;
            return;
        }

        if (!canOutput(result)) {
            progress = 0;
            return;
        }

        int craftTime = recipe.getCraftTime() > 0 ? recipe.getCraftTime() : DEFAULT_CRAFT_TIME;
        progress++;
        if (progress >= craftTime) {
            progress = 0;
            craft(result, inputCount);
        }

        setChanged();
    }

    private boolean canOutput(ItemStack result) {
        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(output, result)) {
            return false;
        }
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void craft(ItemStack result, int inputCount) {
        ItemStack input = items.get(INPUT_SLOT);
        ItemStack output = items.get(OUTPUT_SLOT);

        input.shrink(inputCount);
        if (input.isEmpty()) {
            items.set(INPUT_SLOT, ItemStack.EMPTY);
        }

        if (output.isEmpty()) {
            items.set(OUTPUT_SLOT, result.copy());
        } else {
            output.grow(result.getCount());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        ContainerHelper.loadAllItems(nbt, items, provider);
        if (nbt.contains("SelectedRecipe")) {
            selectedRecipeId = ResourceLocation.tryParse(nbt.getString("SelectedRecipe"));
        } else {
            selectedRecipeId = null;
        }
        progress = nbt.getInt("Progress");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        ContainerHelper.saveAllItems(nbt, items, provider);
        if (selectedRecipeId != null) {
            nbt.putString("SelectedRecipe", selectedRecipeId.toString());
        }
        nbt.putInt("Progress", progress);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveCustomOnly(provider);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factory_ld.constructor");
    }

    @Override
    public ConstructorScreenData getScreenOpeningData(ServerPlayer player) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            return new ConstructorScreenData(getBlockPos(), new java.util.ArrayList<>(ResearchManager.getUnlocked(serverLevel, player)));
        }
        return new ConstructorScreenData(getBlockPos(), java.util.List.of());
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new ConstructorScreenHandler(syncId, playerInventory, this);
    }
}
