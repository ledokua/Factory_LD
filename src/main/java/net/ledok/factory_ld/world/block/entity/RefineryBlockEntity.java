package net.ledok.factory_ld.world.block.entity;

import java.util.Optional;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.ledok.factory_ld.registry.ModBlockEntities;
import net.ledok.factory_ld.registry.ModRecipes;
import net.ledok.factory_ld.world.player.PlayerOverclockAccess;
import net.ledok.factory_ld.world.recipe.ConstructorRecipe;
import net.ledok.factory_ld.world.recipe.RefineryRecipe;
import net.ledok.factory_ld.world.research.ResearchManager;
import net.ledok.factory_ld.world.screen.RefineryScreenData;
import net.ledok.factory_ld.world.screen.RefineryScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public class RefineryBlockEntity extends AbstractMachineBlockEntity implements WorldlyContainer, ExtendedScreenHandlerFactory<RefineryScreenData> {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int SHARD_SLOT_START = 2;
    public static final int SHARD_SLOT_COUNT = 3;
    public static final int SLOT_COUNT = SHARD_SLOT_START + SHARD_SLOT_COUNT;
    private static final int DEFAULT_CRAFT_TIME = 160;
    private static final double BASE_POWER_MW = 45.0;
    private static final double ENERGY_CAPACITY_MJ = 1000.0;
    private static final long TANK_CAPACITY = FluidConstants.BUCKET * 50L;

    private static final int[] SIDED_SLOTS = new int[] {
        INPUT_SLOT,
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
            return TANK_CAPACITY;
        }

        @Override
        protected boolean canExtract(FluidVariant variant) {
            return false;
        }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            return isValidInputFluid(variant);
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
            return TANK_CAPACITY;
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

    public RefineryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REFINERY, pos, state, SLOT_COUNT, BASE_POWER_MW, ENERGY_CAPACITY_MJ);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT) {
            return isValidInput(stack);
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
        return slot == INPUT_SLOT && canPlaceItem(slot, stack);
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

    public Optional<RefineryRecipe> getSelectedRecipe() {
        if (level == null || selectedRecipeId == null) {
            return Optional.empty();
        }
        for (RecipeHolder<RefineryRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.REFINERY_TYPE)) {
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
        for (RecipeHolder<RefineryRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.REFINERY_TYPE)) {
            if (holder.id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onRecipeChanged() {
        clearFluidTanks();
        enforceInventoryValidity();
    }

    public boolean isValidInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Optional<RefineryRecipe> recipeOpt = getSelectedRecipe();
        if (recipeOpt.isEmpty()) {
            return false;
        }
        ConstructorRecipe.InputEntry entry = recipeOpt.get().getInputEntry(0);
        return entry != null && entry.ingredient().test(stack);
    }

    public boolean isValidShard(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.AMETHYST_SHARD);
    }

    public boolean isValidInputFluid(FluidVariant variant) {
        if (variant == null || variant.isBlank()) {
            return false;
        }
        Optional<RefineryRecipe> recipeOpt = getSelectedRecipe();
        if (recipeOpt.isEmpty()) {
            return false;
        }
        RefineryRecipe.FluidEntry fluidIn = recipeOpt.get().getFluidInputEntry(0);
        if (fluidIn == null) {
            return false;
        }
        Fluid expected = getFluid(fluidIn.fluidId());
        return expected != null && variant.isOf(expected);
    }

    public Storage<FluidVariant> getFluidStorage() {
        return new CombinedStorage<>(java.util.List.of(inputTank, outputTank));
    }

    public long getInputFluidMb() {
        return dropletsToMb(inputTank.amount);
    }

    public long getOutputFluidMb() {
        return dropletsToMb(outputTank.amount);
    }

    public FluidVariant getInputFluidVariant() {
        return inputTank.variant;
    }

    public FluidVariant getOutputFluidVariant() {
        return outputTank.variant;
    }

    private void enforceInventoryValidity() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockPos pos = getBlockPos();
        Optional<RefineryRecipe> recipeOpt = getSelectedRecipe();
        RefineryRecipe recipe = recipeOpt.orElse(null);

        ItemStack inputStack = items.get(INPUT_SLOT);
        if (!inputStack.isEmpty()) {
            ConstructorRecipe.InputEntry entry = recipe == null ? null : recipe.getInputEntry(0);
            if (entry == null || !entry.ingredient().test(inputStack)) {
                items.set(INPUT_SLOT, ItemStack.EMPTY);
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

    private void clearFluidTanks() {
        inputTank.variant = FluidVariant.blank();
        inputTank.amount = 0L;
        outputTank.variant = FluidVariant.blank();
        outputTank.amount = 0L;
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, RefineryBlockEntity blockEntity) {
        blockEntity.tickServer();
    }

    private void tickServer() {
        if (level == null || level.isClientSide) {
            return;
        }

        Optional<RefineryRecipe> recipeOpt = getSelectedRecipe();
        if (recipeOpt.isEmpty()) {
            progress = 0;
            return;
        }
        RefineryRecipe recipe = recipeOpt.get();

        ConstructorRecipe.InputEntry itemIn = recipe.getInputEntry(0);
        RefineryRecipe.FluidEntry fluidIn = recipe.getFluidInputEntry(0);
        RefineryRecipe.FluidEntry fluidOut = recipe.getFluidOutputEntry(0);
        if (itemIn == null || fluidIn == null || fluidOut == null) {
            progress = 0;
            return;
        }

        Fluid inputFluid = getFluid(fluidIn.fluidId());
        Fluid outputFluid = getFluid(fluidOut.fluidId());
        if (inputFluid == null || outputFluid == null) {
            progress = 0;
            return;
        }

        ItemStack itemInput = items.get(INPUT_SLOT);
        if (itemInput.isEmpty() || itemInput.getCount() < Math.max(1, itemIn.count()) || !itemIn.ingredient().test(itemInput)) {
            progress = 0;
            return;
        }

        long requiredInputDroplets = mbToDroplets(fluidIn.amountMb());
        if (requiredInputDroplets <= 0L
            || inputTank.amount < requiredInputDroplets
            || inputTank.variant.isBlank()
            || !inputTank.variant.isOf(inputFluid)) {
            progress = 0;
            return;
        }

        ItemStack result = recipe.getOutput();
        if (result.isEmpty() || !canOutput(result)) {
            progress = 0;
            return;
        }

        long outputDroplets = mbToDroplets(fluidOut.amountMb());
        if (!canOutputFluid(outputFluid, outputDroplets)) {
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
            craft(result, Math.max(1, itemIn.count()), inputFluid, requiredInputDroplets, outputFluid, outputDroplets);
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

    private boolean canOutputFluid(Fluid fluid, long amountDroplets) {
        if (amountDroplets <= 0) {
            return false;
        }
        if (outputTank.amount + amountDroplets > TANK_CAPACITY) {
            return false;
        }
        return outputTank.variant.isBlank() || outputTank.variant.isOf(fluid);
    }

    private void craft(
        ItemStack result,
        int itemInputCount,
        Fluid inputFluid,
        long inputAmountDroplets,
        Fluid outputFluid,
        long outputAmountDroplets
    ) {
        ItemStack itemInput = items.get(INPUT_SLOT);
        ItemStack itemOutput = items.get(OUTPUT_SLOT);

        itemInput.shrink(itemInputCount);
        if (itemInput.isEmpty()) {
            items.set(INPUT_SLOT, ItemStack.EMPTY);
        }
        inputTank.amount = Math.max(0L, inputTank.amount - inputAmountDroplets);
        if (inputTank.amount == 0L) {
            inputTank.variant = FluidVariant.blank();
        } else if (!inputTank.variant.isOf(inputFluid)) {
            inputTank.variant = FluidVariant.blank();
            inputTank.amount = 0L;
        }

        if (itemOutput.isEmpty()) {
            items.set(OUTPUT_SLOT, result.copy());
        } else {
            itemOutput.grow(result.getCount());
        }

        if (outputTank.variant.isBlank()) {
            outputTank.variant = FluidVariant.of(outputFluid);
        }
        outputTank.amount = Math.min(TANK_CAPACITY, outputTank.amount + outputAmountDroplets);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        ContainerHelper.loadAllItems(nbt, items, provider);
        loadMachineData(nbt);
        readTank(nbt, "InputFluid", inputTank, TANK_CAPACITY);
        readTank(nbt, "OutputFluid", outputTank, TANK_CAPACITY);
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
        return Component.translatable("block.factory_ld.refinery");
    }

    @Override
    public RefineryScreenData getScreenOpeningData(ServerPlayer player) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            return new RefineryScreenData(
                getBlockPos(),
                new java.util.ArrayList<>(ResearchManager.getUnlocked(serverLevel, player)),
                PlayerOverclockAccess.isUnlocked(player)
            );
        }
        return new RefineryScreenData(getBlockPos(), java.util.List.of(), false);
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new RefineryScreenHandler(syncId, playerInventory, this);
    }

    private static Fluid getFluid(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.FLUID.getOptional(id).orElse(null);
    }

    private static long mbToDroplets(long amountMb) {
        return Math.max(0L, amountMb) * FluidConstants.BUCKET / 1000L;
    }

    private static long dropletsToMb(long amountDroplets) {
        return Math.max(0L, amountDroplets) * 1000L / FluidConstants.BUCKET;
    }

    private static void readTank(CompoundTag root, String key, SingleVariantStorage<FluidVariant> tank, long capacity) {
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
        tank.amount = Math.max(0L, Math.min(capacity, tag.getLong("amount")));
        if (tank.amount == 0L) {
            tank.variant = FluidVariant.blank();
        }
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
