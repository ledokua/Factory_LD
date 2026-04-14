package net.ledok.factory_ld.world.block.entity;

import java.util.ArrayList;
import java.util.List;
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
import net.ledok.factory_ld.world.recipe.MachineRecipe;
import net.ledok.factory_ld.world.research.ResearchManager;
import net.ledok.factory_ld.world.screen.GenericMachineScreenData;
import net.ledok.factory_ld.world.screen.GenericMachineScreenHandler;
import net.ledok.factory_ld.world.power.PowerUnits;
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

public class GenericMachineBlockEntity extends AbstractMachineBlockEntity implements WorldlyContainer, ExtendedScreenHandlerFactory<GenericMachineScreenData> {
    private static final double STANDBY_POWER_MW = 0.1;
    private final String machineId;
    private final SingleVariantStorage<FluidVariant>[] inputTanks;
    private final SingleVariantStorage<FluidVariant>[] outputTanks;
    private final Storage<FluidVariant> fluidStorage;
    private final int[] sidedSlots;

    public GenericMachineBlockEntity(BlockPos pos, BlockState state, String machineId) {
        super(
            ModBlockEntities.requireMachineBlockEntityType(machineId),
            pos,
            state,
            MachineSpecs.require(machineId).totalItemSlots(),
            MachineSpecs.require(machineId)
        );
        this.machineId = machineId;
        this.inputTanks = createInputTanks(machineSpec.fluidInputTanks());
        this.outputTanks = createOutputTanks(machineSpec.fluidOutputTanks());
        this.fluidStorage = new CombinedStorage<>(combinedTankList());
        this.sidedSlots = createSidedSlotIndices();
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, GenericMachineBlockEntity blockEntity) {
        blockEntity.tickServer();
    }

    public String machineId() {
        return machineId;
    }

    public int getItemInputSlot(int index) {
        return index;
    }

    public int getItemOutputSlot(int index) {
        return machineSpec.itemInputSlots() + index;
    }

    @Override
    public int getShardSlotStart() {
        return machineSpec.itemInputSlots() + machineSpec.itemOutputSlots();
    }

    @Override
    public int getShardSlotCount() {
        return machineSpec.shardSlots();
    }

    public Storage<FluidVariant> getFluidStorage() {
        return fluidStorage;
    }

    public FluidVariant getInputFluidVariant() {
        return getInputFluidVariant(0);
    }

    public FluidVariant getOutputFluidVariant() {
        return getOutputFluidVariant(0);
    }

    public long getInputFluidMb() {
        return getInputFluidMb(0);
    }

    public long getOutputFluidMb() {
        return getOutputFluidMb(0);
    }

    public FluidVariant getInputFluidVariant(int index) {
        return index >= 0 && index < inputTanks.length ? inputTanks[index].variant : FluidVariant.blank();
    }

    public FluidVariant getOutputFluidVariant(int index) {
        return index >= 0 && index < outputTanks.length ? outputTanks[index].variant : FluidVariant.blank();
    }

    public long getInputFluidMb(int index) {
        return index >= 0 && index < inputTanks.length ? dropletsToMb(inputTanks[index].amount) : 0L;
    }

    public long getOutputFluidMb(int index) {
        return index >= 0 && index < outputTanks.length ? dropletsToMb(outputTanks[index].amount) : 0L;
    }

    public Optional<MachineRecipe> getSelectedRecipe() {
        if (level == null || selectedRecipeId == null) {
            return Optional.empty();
        }
        Optional<RecipeHolder<?>> entry = level.getRecipeManager().byKey(selectedRecipeId);
        if (entry.isEmpty() || entry.get().value().getType() != ModRecipes.requireMachineRecipeType(machineSpec.id())) {
            return Optional.empty();
        }
        if (entry.get().value() instanceof MachineRecipe recipe) {
            return Optional.of(recipe);
        }
        return Optional.empty();
    }

    public boolean hasSelectedRecipeForPowerGrid() {
        return getSelectedRecipe().isPresent();
    }

    public boolean canProcessSelectedRecipeForPowerGrid() {
        Optional<MachineRecipe> recipeOpt = getSelectedRecipe();
        return recipeOpt.isPresent() && canProcessRecipe(recipeOpt.get());
    }

    public double activeDemandPerTickMj() {
        return PowerUnits.mwToMjPerTick(getPowerUsageMw());
    }

    public double standbyDemandPerTickMj() {
        return PowerUnits.mwToMjPerTick(STANDBY_POWER_MW);
    }

    @Override
    protected boolean isRecipeIdValid(ResourceLocation id) {
        return isRecipeIdValidForType(id, ModRecipes.requireMachineRecipeType(machineSpec.id()));
    }

    @Override
    protected void onRecipeChanged() {
        clearFluidTanks();
        enforceInventoryValidity();
    }

    public boolean isValidInput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (slot < 0 || slot >= machineSpec.itemInputSlots()) {
            return false;
        }
        Optional<MachineRecipe> recipeOpt = getSelectedRecipe();
        if (recipeOpt.isEmpty()) {
            return false;
        }
        MachineRecipe.InputEntry input = recipeOpt.get().getInputEntry(slot);
        return input != null && input.ingredient().test(stack);
    }

    public boolean isValidShard(ItemStack stack) {
        return machineSpec.overclockEnabled() && !stack.isEmpty() && stack.is(Items.AMETHYST_SHARD);
    }

    public boolean isValidInputFluid(int tankIndex, FluidVariant variant) {
        if (tankIndex < 0 || tankIndex >= inputTanks.length || variant == null || variant.isBlank()) {
            return false;
        }
        Optional<MachineRecipe> recipeOpt = getSelectedRecipe();
        if (recipeOpt.isEmpty()) {
            return false;
        }
        MachineRecipe.FluidEntry fluidIn = recipeOpt.get().getFluidInputEntry(tankIndex);
        if (fluidIn == null) {
            return false;
        }
        Fluid expected = getFluid(fluidIn.fluidId());
        return expected != null && variant.isOf(expected);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < machineSpec.itemInputSlots()) {
            return isValidInput(slot, stack);
        }
        if (slot >= getShardSlotStart() && slot < getShardSlotStart() + getShardSlotCount()) {
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
        return sidedSlots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 0 && slot < machineSpec.itemInputSlots() && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= machineSpec.itemInputSlots() && slot < machineSpec.itemInputSlots() + machineSpec.itemOutputSlots();
    }

    private void tickServer() {
        if (level == null || level.isClientSide) {
            return;
        }
        Optional<MachineRecipe> recipeOpt = getSelectedRecipe();
        if (recipeOpt.isEmpty()) {
            resetProgress();
            return;
        }
        MachineRecipe recipe = recipeOpt.get();
        if (!canProcessRecipe(recipe)) {
            resetProgress();
            return;
        }
        int craftTime = recipe.getCraftTime() > 0 ? recipe.getCraftTime() : 100;
        processCraftingTick(craftTime, true, () -> craft(recipe));
    }

    private boolean canProcessRecipe(MachineRecipe recipe) {
        for (int i = 0; i < machineSpec.itemInputSlots(); i++) {
            MachineRecipe.InputEntry required = recipe.getInputEntry(i);
            ItemStack in = items.get(getItemInputSlot(i));
            if (required == null || in.isEmpty() || in.getCount() < Math.max(1, required.count()) || !required.ingredient().test(in)) {
                return false;
            }
        }

        for (int i = 0; i < machineSpec.fluidInputTanks(); i++) {
            MachineRecipe.FluidEntry required = recipe.getFluidInputEntry(i);
            if (required == null) {
                return false;
            }
            Fluid fluid = getFluid(required.fluidId());
            if (fluid == null) {
                return false;
            }
            long requiredDroplets = mbToDroplets(required.amountMb());
            SingleVariantStorage<FluidVariant> tank = inputTanks[i];
            if (requiredDroplets <= 0L || tank.amount < requiredDroplets || tank.variant.isBlank() || !tank.variant.isOf(fluid)) {
                return false;
            }
        }

        for (int i = 0; i < machineSpec.itemOutputSlots(); i++) {
            ItemStack result = recipe.getOutputEntry(i);
            if (result.isEmpty()) {
                return false;
            }
            ItemStack output = items.get(getItemOutputSlot(i));
            if (!output.isEmpty()) {
                if (!ItemStack.isSameItemSameComponents(output, result)) {
                    return false;
                }
                if (output.getCount() + result.getCount() > output.getMaxStackSize()) {
                    return false;
                }
            }
        }

        for (int i = 0; i < machineSpec.fluidOutputTanks(); i++) {
            MachineRecipe.FluidEntry result = recipe.getFluidOutputEntry(i);
            if (result == null) {
                return false;
            }
            Fluid fluid = getFluid(result.fluidId());
            if (fluid == null) {
                return false;
            }
            long outputDroplets = mbToDroplets(result.amountMb());
            SingleVariantStorage<FluidVariant> tank = outputTanks[i];
            if (outputDroplets <= 0L || tank.amount + outputDroplets > machineSpec.fluidTankCapacityDroplets()) {
                return false;
            }
            if (!tank.variant.isBlank() && !tank.variant.isOf(fluid)) {
                return false;
            }
        }
        return true;
    }

    private void craft(MachineRecipe recipe) {
        for (int i = 0; i < machineSpec.itemInputSlots(); i++) {
            MachineRecipe.InputEntry required = recipe.getInputEntry(i);
            int slot = getItemInputSlot(i);
            ItemStack stack = items.get(slot);
            stack.shrink(Math.max(1, required.count()));
            if (stack.isEmpty()) {
                items.set(slot, ItemStack.EMPTY);
            }
        }

        for (int i = 0; i < machineSpec.fluidInputTanks(); i++) {
            MachineRecipe.FluidEntry required = recipe.getFluidInputEntry(i);
            SingleVariantStorage<FluidVariant> tank = inputTanks[i];
            tank.amount = Math.max(0L, tank.amount - mbToDroplets(required.amountMb()));
            if (tank.amount == 0L) {
                tank.variant = FluidVariant.blank();
            }
        }

        for (int i = 0; i < machineSpec.itemOutputSlots(); i++) {
            ItemStack result = recipe.getOutputEntry(i);
            int slot = getItemOutputSlot(i);
            ItemStack output = items.get(slot);
            if (output.isEmpty()) {
                items.set(slot, result.copy());
            } else {
                output.grow(result.getCount());
            }
        }

        for (int i = 0; i < machineSpec.fluidOutputTanks(); i++) {
            MachineRecipe.FluidEntry result = recipe.getFluidOutputEntry(i);
            Fluid fluid = getFluid(result.fluidId());
            SingleVariantStorage<FluidVariant> tank = outputTanks[i];
            if (tank.variant.isBlank() && fluid != null) {
                tank.variant = FluidVariant.of(fluid);
            }
            tank.amount = Math.min(machineSpec.fluidTankCapacityDroplets(), tank.amount + mbToDroplets(result.amountMb()));
        }
    }

    private void enforceInventoryValidity() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockPos pos = getBlockPos();
        MachineRecipe recipe = getSelectedRecipe().orElse(null);

        for (int i = 0; i < machineSpec.itemInputSlots(); i++) {
            int slot = getItemInputSlot(i);
            ItemStack stack = items.get(slot);
            MachineRecipe.InputEntry expected = recipe == null ? null : recipe.getInputEntry(i);
            if (!stack.isEmpty() && (expected == null || !expected.ingredient().test(stack))) {
                items.set(slot, ItemStack.EMPTY);
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            }
        }

        for (int i = 0; i < machineSpec.itemOutputSlots(); i++) {
            int slot = getItemOutputSlot(i);
            ItemStack output = items.get(slot);
            ItemStack expected = recipe == null ? ItemStack.EMPTY : recipe.getOutputEntry(i);
            if (!output.isEmpty() && (expected.isEmpty() || !ItemStack.isSameItemSameComponents(output, expected))) {
                items.set(slot, ItemStack.EMPTY);
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, output);
            }
        }
    }

    private void clearFluidTanks() {
        for (SingleVariantStorage<FluidVariant> tank : inputTanks) {
            tank.variant = FluidVariant.blank();
            tank.amount = 0L;
        }
        for (SingleVariantStorage<FluidVariant> tank : outputTanks) {
            tank.variant = FluidVariant.blank();
            tank.amount = 0L;
        }
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        ContainerHelper.loadAllItems(nbt, items, provider);
        loadMachineData(nbt);
        for (int i = 0; i < inputTanks.length; i++) {
            readTank(nbt, "InputFluid" + i, inputTanks[i], machineSpec.fluidTankCapacityDroplets());
        }
        for (int i = 0; i < outputTanks.length; i++) {
            readTank(nbt, "OutputFluid" + i, outputTanks[i], machineSpec.fluidTankCapacityDroplets());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        ContainerHelper.saveAllItems(nbt, items, provider);
        saveMachineData(nbt);
        for (int i = 0; i < inputTanks.length; i++) {
            writeTank(nbt, "InputFluid" + i, inputTanks[i]);
        }
        for (int i = 0; i < outputTanks.length; i++) {
            writeTank(nbt, "OutputFluid" + i, outputTanks[i]);
        }
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
        return Component.translatable("block.factory_ld." + machineId);
    }

    @Override
    public GenericMachineScreenData getScreenOpeningData(ServerPlayer player) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            return new GenericMachineScreenData(
                getBlockPos(),
                new ArrayList<>(ResearchManager.getUnlocked(serverLevel, player)),
                PlayerOverclockAccess.isUnlocked(player)
            );
        }
        return new GenericMachineScreenData(getBlockPos(), List.of(), false);
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new GenericMachineScreenHandler(syncId, playerInventory, this);
    }

    private SingleVariantStorage<FluidVariant>[] createInputTanks(int count) {
        @SuppressWarnings("unchecked")
        SingleVariantStorage<FluidVariant>[] tanks = new SingleVariantStorage[count];
        for (int i = 0; i < count; i++) {
            final int tankIndex = i;
            tanks[i] = new SingleVariantStorage<>() {
                @Override
                protected FluidVariant getBlankVariant() {
                    return FluidVariant.blank();
                }

                @Override
                protected long getCapacity(FluidVariant variant) {
                    return machineSpec.fluidTankCapacityDroplets();
                }

                @Override
                protected boolean canExtract(FluidVariant variant) {
                    return false;
                }

                @Override
                protected boolean canInsert(FluidVariant variant) {
                    return isValidInputFluid(tankIndex, variant);
                }

                @Override
                protected void onFinalCommit() {
                    setChanged();
                }
            };
        }
        return tanks;
    }

    private SingleVariantStorage<FluidVariant>[] createOutputTanks(int count) {
        @SuppressWarnings("unchecked")
        SingleVariantStorage<FluidVariant>[] tanks = new SingleVariantStorage[count];
        for (int i = 0; i < count; i++) {
            tanks[i] = new SingleVariantStorage<>() {
                @Override
                protected FluidVariant getBlankVariant() {
                    return FluidVariant.blank();
                }

                @Override
                protected long getCapacity(FluidVariant variant) {
                    return machineSpec.fluidTankCapacityDroplets();
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
        }
        return tanks;
    }

    private List<Storage<FluidVariant>> combinedTankList() {
        List<Storage<FluidVariant>> storages = new ArrayList<>(inputTanks.length + outputTanks.length);
        for (SingleVariantStorage<FluidVariant> tank : inputTanks) {
            storages.add(tank);
        }
        for (SingleVariantStorage<FluidVariant> tank : outputTanks) {
            storages.add(tank);
        }
        return storages;
    }

    private int[] createSidedSlotIndices() {
        int[] slots = new int[getContainerSize()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
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
