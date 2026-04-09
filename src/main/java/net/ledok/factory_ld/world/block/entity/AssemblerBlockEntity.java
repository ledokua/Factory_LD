package net.ledok.factory_ld.world.block.entity;

import java.util.Optional;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.ledok.factory_ld.registry.ModBlockEntities;
import net.ledok.factory_ld.registry.ModRecipes;
import net.ledok.factory_ld.world.player.PlayerOverclockAccess;
import net.ledok.factory_ld.world.recipe.AssemblerRecipe;
import net.ledok.factory_ld.world.recipe.ConstructorRecipe;
import net.ledok.factory_ld.world.research.ResearchManager;
import net.ledok.factory_ld.world.screen.AssemblerScreenData;
import net.ledok.factory_ld.world.screen.AssemblerScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.state.BlockState;

public class AssemblerBlockEntity extends AbstractMachineBlockEntity implements WorldlyContainer, ExtendedScreenHandlerFactory<AssemblerScreenData> {
    public static final int INPUT_SLOT_1 = 0;
    public static final int INPUT_SLOT_2 = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int SHARD_SLOT_START = 3;
    public static final int SHARD_SLOT_COUNT = 3;
    public static final int SLOT_COUNT = SHARD_SLOT_START + SHARD_SLOT_COUNT;
    private static final int DEFAULT_CRAFT_TIME = 120;
    private static final double BASE_POWER_MW = 30.0;
    private static final double ENERGY_CAPACITY_MJ = 1000.0;
    private static final long FLUID_INPUT_CAPACITY = 0;
    private static final long FLUID_OUTPUT_CAPACITY = 0;

    private static final int[] SIDED_SLOTS = new int[] {
        INPUT_SLOT_1,
        INPUT_SLOT_2,
        OUTPUT_SLOT,
        SHARD_SLOT_START,
        SHARD_SLOT_START + 1,
        SHARD_SLOT_START + 2
    };

    private final SingleVariantStorage<FluidVariant> inputTank = new SingleVariantStorage<>() {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return FLUID_INPUT_CAPACITY;
        }

        @Override
        protected boolean canExtract(FluidVariant variant) {
            return false;
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    private final SingleVariantStorage<FluidVariant> outputTank = new SingleVariantStorage<>() {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return FLUID_OUTPUT_CAPACITY;
        }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            return false;
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    public AssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASSEMBLER, pos, state, SLOT_COUNT, BASE_POWER_MW, ENERGY_CAPACITY_MJ);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT_1 || slot == INPUT_SLOT_2) {
            return isValidInput(slot, stack);
        }
        if (slot >= SHARD_SLOT_START && slot < SHARD_SLOT_START + SHARD_SLOT_COUNT) {
            return isValidShard(stack);
        }
        return false;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty() && !canPlaceItem(slot, stack)) {
            return;
        }
        setMachineItem(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SIDED_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return (slot == INPUT_SLOT_1 || slot == INPUT_SLOT_2) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    public int getShardSlotStart() {
        return SHARD_SLOT_START;
    }

    @Override
    public int getShardSlotCount() {
        return SHARD_SLOT_COUNT;
    }

    public Optional<AssemblerRecipe> getSelectedRecipe() {
        if (level == null || selectedRecipeId == null) {
            return Optional.empty();
        }
        for (RecipeHolder<AssemblerRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.ASSEMBLER_TYPE)) {
            if (holder.id().equals(selectedRecipeId)) {
                return Optional.of(holder.value());
            }
        }
        return Optional.empty();
    }

    @Override
    protected boolean isRecipeIdValid(ResourceLocation id) {
        if (level == null) {
            return false;
        }
        for (RecipeHolder<AssemblerRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.ASSEMBLER_TYPE)) {
            if (holder.id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onRecipeChanged() {
        enforceInventoryValidity();
    }

    public boolean isValidInput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Optional<AssemblerRecipe> recipeOpt = getSelectedRecipe();
        if (recipeOpt.isEmpty()) {
            return false;
        }
        ConstructorRecipe.InputEntry entry = recipeOpt.get().getInputEntry(slot == INPUT_SLOT_1 ? 0 : 1);
        return entry != null && entry.ingredient().test(stack);
    }

    public boolean isValidShard(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.AMETHYST_SHARD);
    }

    public Storage<FluidVariant> getFluidStorage() {
        return new CombinedStorage<>(java.util.List.of(inputTank, outputTank));
    }

    private void enforceInventoryValidity() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockPos pos = getBlockPos();
        Optional<AssemblerRecipe> recipeOpt = getSelectedRecipe();
        AssemblerRecipe recipe = recipeOpt.orElse(null);

        for (int i = 0; i < 2; i++) {
            int slot = i == 0 ? INPUT_SLOT_1 : INPUT_SLOT_2;
            ItemStack inputStack = items.get(slot);
            if (inputStack.isEmpty()) {
                continue;
            }
            ConstructorRecipe.InputEntry entry = recipe == null ? null : recipe.getInputEntry(i);
            if (entry == null || !entry.ingredient().test(inputStack)) {
                items.set(slot, ItemStack.EMPTY);
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, inputStack);
            }
        }

        ItemStack outputStack = items.get(OUTPUT_SLOT);
        if (!outputStack.isEmpty()) {
            ItemStack expected = recipe == null ? ItemStack.EMPTY : recipe.getOutput();
            if (expected.isEmpty() || !ItemStack.isSameItemSameComponents(outputStack, expected)) {
                items.set(OUTPUT_SLOT, ItemStack.EMPTY);
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, outputStack);
            }
        }
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, AssemblerBlockEntity blockEntity) {
        blockEntity.tickServer();
    }

    private void tickServer() {
        if (level == null || level.isClientSide) {
            return;
        }

        Optional<AssemblerRecipe> recipeOpt = getSelectedRecipe();
        if (recipeOpt.isEmpty()) {
            progress = 0;
            return;
        }
        AssemblerRecipe recipe = recipeOpt.get();
        ConstructorRecipe.InputEntry in1 = recipe.getInputEntry(0);
        ConstructorRecipe.InputEntry in2 = recipe.getInputEntry(1);
        if (in1 == null || in2 == null) {
            progress = 0;
            return;
        }

        ItemStack input1 = items.get(INPUT_SLOT_1);
        ItemStack input2 = items.get(INPUT_SLOT_2);
        if (input1.isEmpty()
            || input1.getCount() < Math.max(1, in1.count())
            || !in1.ingredient().test(input1)
            || input2.isEmpty()
            || input2.getCount() < Math.max(1, in2.count())
            || !in2.ingredient().test(input2)) {
            progress = 0;
            return;
        }

        ItemStack result = recipe.getOutput();
        if (result.isEmpty() || !canOutput(result)) {
            progress = 0;
            return;
        }

        int craftTime = recipe.getCraftTime() > 0 ? recipe.getCraftTime() : DEFAULT_CRAFT_TIME;
        double powerPerTick = getPowerUsageMw() / 20.0;
        if (energyStored < powerPerTick) {
            progress = 0;
            return;
        }
        energyStored = Math.max(0.0, energyStored - powerPerTick);

        progress += Math.max(0.01, clockSpeedPercent / 100.0);
        if (progress >= craftTime) {
            progress -= craftTime;
            craft(result, Math.max(1, in1.count()), Math.max(1, in2.count()));
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

    private void craft(ItemStack result, int inputCount1, int inputCount2) {
        ItemStack input1 = items.get(INPUT_SLOT_1);
        ItemStack input2 = items.get(INPUT_SLOT_2);
        ItemStack output = items.get(OUTPUT_SLOT);

        input1.shrink(inputCount1);
        if (input1.isEmpty()) {
            items.set(INPUT_SLOT_1, ItemStack.EMPTY);
        }
        input2.shrink(inputCount2);
        if (input2.isEmpty()) {
            items.set(INPUT_SLOT_2, ItemStack.EMPTY);
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
        loadMachineData(nbt);
        readTank(nbt, "InputFluid", inputTank);
        readTank(nbt, "OutputFluid", outputTank);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        ContainerHelper.saveAllItems(nbt, items, provider);
        saveMachineData(nbt);
        writeTank(nbt, "InputFluid", inputTank);
        writeTank(nbt, "OutputFluid", outputTank);
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
        return Component.translatable("block.factory_ld.assembler");
    }

    @Override
    public AssemblerScreenData getScreenOpeningData(ServerPlayer player) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            return new AssemblerScreenData(
                getBlockPos(),
                new java.util.ArrayList<>(ResearchManager.getUnlocked(serverLevel, player)),
                PlayerOverclockAccess.isUnlocked(player)
            );
        }
        return new AssemblerScreenData(getBlockPos(), java.util.List.of(), false);
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new AssemblerScreenHandler(syncId, playerInventory, this);
    }

    private static void readTank(CompoundTag root, String key, SingleVariantStorage<FluidVariant> tank) {
        if (!root.contains(key)) {
            return;
        }
        CompoundTag tag = root.getCompound(key);
        FluidVariant variant = FluidVariant.blank();
        if (tag.contains("variant")) {
            Tag variantTag = tag.get("variant");
            if (variantTag != null) {
                variant = FluidVariant.CODEC.parse(NbtOps.INSTANCE, variantTag)
                    .result()
                    .orElse(FluidVariant.blank());
            }
        }
        tank.variant = variant;
        tank.amount = Math.max(0L, tag.getLong("amount"));
    }

    private static void writeTank(CompoundTag root, String key, SingleVariantStorage<FluidVariant> tank) {
        CompoundTag tag = new CompoundTag();
        FluidVariant.CODEC.encodeStart(NbtOps.INSTANCE, tank.variant)
            .result()
            .ifPresent(encoded -> tag.put("variant", encoded));
        tag.putLong("amount", tank.amount);
        root.put(key, tag);
    }
}
