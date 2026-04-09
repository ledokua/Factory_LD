package net.ledok.factory_ld.registry;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.ledok.factory_ld.FactoryLdMod;
import net.ledok.factory_ld.world.block.entity.AssemblerBlockEntity;
import net.ledok.factory_ld.world.block.entity.ConstructorBlockEntity;
import net.ledok.factory_ld.world.block.entity.RefineryBlockEntity;
import net.ledok.factory_ld.world.screen.AssemblerScreenHandler;
import net.ledok.factory_ld.world.screen.ConstructorScreenHandler;
import net.ledok.factory_ld.world.screen.RefineryScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ModNetworking {
    public static final CustomPacketPayload.Type<SetClockSpeedPayload> SET_CLOCK_SPEED =
        new CustomPacketPayload.Type<>(FactoryLdMod.id("set_clock_speed"));
    public static final CustomPacketPayload.Type<PasteMachineSettingsPayload> PASTE_MACHINE_SETTINGS =
        new CustomPacketPayload.Type<>(FactoryLdMod.id("paste_machine_settings"));

    private ModNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(SET_CLOCK_SPEED, SetClockSpeedPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PASTE_MACHINE_SETTINGS, PasteMachineSettingsPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SET_CLOCK_SPEED, (payload, context) -> {
            handleSetClockSpeed(context.player(), payload.pos(), payload.scaled());
        });
        ServerPlayNetworking.registerGlobalReceiver(PASTE_MACHINE_SETTINGS, (payload, context) -> {
            handlePasteMachineSettings(context.player(), payload.pos(), payload.recipeIdOrNull(), payload.scaledClock());
        });
    }

    private static void handleSetClockSpeed(ServerPlayer player, BlockPos pos, int scaled) {
        if (player.level().getBlockEntity(pos) instanceof ConstructorBlockEntity be) {
            if (!(player.containerMenu instanceof ConstructorScreenHandler screenHandler)) {
                return;
            }
            if (screenHandler.getBlockEntity() != be) {
                return;
            }
            double percent = scaled / 10000.0;
            if (!screenHandler.isOverclockUnlocked()) {
                percent = Math.min(percent, 100.0);
            }
            be.setClockSpeedPercent(percent);
            return;
        }
        if (player.level().getBlockEntity(pos) instanceof AssemblerBlockEntity be) {
            if (!(player.containerMenu instanceof AssemblerScreenHandler screenHandler)) {
                return;
            }
            if (screenHandler.getBlockEntity() != be) {
                return;
            }
            double percent = scaled / 10000.0;
            if (!screenHandler.isOverclockUnlocked()) {
                percent = Math.min(percent, 100.0);
            }
            be.setClockSpeedPercent(percent);
            return;
        }
        if (player.level().getBlockEntity(pos) instanceof RefineryBlockEntity be) {
            if (!(player.containerMenu instanceof RefineryScreenHandler screenHandler)) {
                return;
            }
            if (screenHandler.getBlockEntity() != be) {
                return;
            }
            double percent = scaled / 10000.0;
            if (!screenHandler.isOverclockUnlocked()) {
                percent = Math.min(percent, 100.0);
            }
            be.setClockSpeedPercent(percent);
        }
    }

    private static void handlePasteMachineSettings(ServerPlayer player, BlockPos pos, ResourceLocation recipeId, int scaledClock) {
        if (player.containerMenu instanceof ConstructorScreenHandler constructorMenu
            && player.level().getBlockEntity(pos) instanceof ConstructorBlockEntity constructorBe
            && constructorMenu.getBlockEntity() == constructorBe) {
            applyPastedSettings(player.getInventory(), recipeId, scaledClock, constructorMenu.isOverclockUnlocked(), constructorBe);
            return;
        }
        if (player.containerMenu instanceof AssemblerScreenHandler assemblerMenu
            && player.level().getBlockEntity(pos) instanceof AssemblerBlockEntity assemblerBe
            && assemblerMenu.getBlockEntity() == assemblerBe) {
            applyPastedSettings(player.getInventory(), recipeId, scaledClock, assemblerMenu.isOverclockUnlocked(), assemblerBe);
            return;
        }
        if (player.containerMenu instanceof RefineryScreenHandler refineryMenu
            && player.level().getBlockEntity(pos) instanceof RefineryBlockEntity refineryBe
            && refineryMenu.getBlockEntity() == refineryBe) {
            applyPastedSettings(player.getInventory(), recipeId, scaledClock, refineryMenu.isOverclockUnlocked(), refineryBe);
        }
    }

    private static int requiredShardSlotsFor(double clockPercent) {
        if (clockPercent <= 100.0) {
            return 0;
        }
        return Math.min(
            ConstructorBlockEntity.SHARD_SLOT_COUNT,
            (int)Math.ceil((clockPercent - 100.0) / 50.0)
        );
    }

    private static void fillShardSlotsFromPlayer(Inventory inventory, ConstructorBlockEntity be, int requiredSlots) {
        int occupied = 0;
        for (int i = 0; i < ConstructorBlockEntity.SHARD_SLOT_COUNT; i++) {
            ItemStack machineStack = be.getItems().get(ConstructorBlockEntity.SHARD_SLOT_START + i);
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
                int targetShardSlot = firstEmptyShardSlot(be);
                if (targetShardSlot < 0) {
                    return;
                }
                be.getItems().set(targetShardSlot, new ItemStack(Items.AMETHYST_SHARD, 1));
                invStack.shrink(1);
                needed--;
            }
        }
        be.setChanged();
    }

    private static int firstEmptyShardSlot(ConstructorBlockEntity be) {
        for (int i = 0; i < ConstructorBlockEntity.SHARD_SLOT_COUNT; i++) {
            int index = ConstructorBlockEntity.SHARD_SLOT_START + i;
            if (be.getItems().get(index).isEmpty()) {
                return index;
            }
        }
        return -1;
    }

    private static void applyPastedSettings(
        Inventory inventory,
        ResourceLocation recipeId,
        int scaledClock,
        boolean overclockUnlocked,
        ConstructorBlockEntity be
    ) {
        be.setSelectedRecipeId(recipeId);
        double requestedClock = scaledClock / 10000.0;
        if (!overclockUnlocked) {
            be.setClockSpeedPercent(Math.min(requestedClock, 100.0));
            return;
        }
        fillShardSlotsFromPlayer(inventory, be, requiredShardSlotsFor(requestedClock));
        be.setClockSpeedPercent(Math.min(requestedClock, be.getMaxClockSpeedPercent()));
    }

    private static void applyPastedSettings(
        Inventory inventory,
        ResourceLocation recipeId,
        int scaledClock,
        boolean overclockUnlocked,
        AssemblerBlockEntity be
    ) {
        be.setSelectedRecipeId(recipeId);
        double requestedClock = scaledClock / 10000.0;
        if (!overclockUnlocked) {
            be.setClockSpeedPercent(Math.min(requestedClock, 100.0));
            return;
        }
        fillShardSlotsFromPlayer(inventory, be, requiredShardSlotsFor(requestedClock));
        be.setClockSpeedPercent(Math.min(requestedClock, be.getMaxClockSpeedPercent()));
    }

    private static void applyPastedSettings(
        Inventory inventory,
        ResourceLocation recipeId,
        int scaledClock,
        boolean overclockUnlocked,
        RefineryBlockEntity be
    ) {
        be.setSelectedRecipeId(recipeId);
        double requestedClock = scaledClock / 10000.0;
        if (!overclockUnlocked) {
            be.setClockSpeedPercent(Math.min(requestedClock, 100.0));
            return;
        }
        fillShardSlotsFromPlayer(inventory, be, requiredShardSlotsFor(requestedClock));
        be.setClockSpeedPercent(Math.min(requestedClock, be.getMaxClockSpeedPercent()));
    }

    private static void fillShardSlotsFromPlayer(Inventory inventory, AssemblerBlockEntity be, int requiredSlots) {
        int occupied = 0;
        for (int i = 0; i < AssemblerBlockEntity.SHARD_SLOT_COUNT; i++) {
            ItemStack machineStack = be.getItems().get(AssemblerBlockEntity.SHARD_SLOT_START + i);
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
                int targetShardSlot = firstEmptyShardSlot(be);
                if (targetShardSlot < 0) {
                    return;
                }
                be.getItems().set(targetShardSlot, new ItemStack(Items.AMETHYST_SHARD, 1));
                invStack.shrink(1);
                needed--;
            }
        }
        be.setChanged();
    }

    private static int firstEmptyShardSlot(AssemblerBlockEntity be) {
        for (int i = 0; i < AssemblerBlockEntity.SHARD_SLOT_COUNT; i++) {
            int index = AssemblerBlockEntity.SHARD_SLOT_START + i;
            if (be.getItems().get(index).isEmpty()) {
                return index;
            }
        }
        return -1;
    }

    private static void fillShardSlotsFromPlayer(Inventory inventory, RefineryBlockEntity be, int requiredSlots) {
        int occupied = 0;
        for (int i = 0; i < RefineryBlockEntity.SHARD_SLOT_COUNT; i++) {
            ItemStack machineStack = be.getItems().get(RefineryBlockEntity.SHARD_SLOT_START + i);
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
                int targetShardSlot = firstEmptyShardSlot(be);
                if (targetShardSlot < 0) {
                    return;
                }
                be.getItems().set(targetShardSlot, new ItemStack(Items.AMETHYST_SHARD, 1));
                invStack.shrink(1);
                needed--;
            }
        }
        be.setChanged();
    }

    private static int firstEmptyShardSlot(RefineryBlockEntity be) {
        for (int i = 0; i < RefineryBlockEntity.SHARD_SLOT_COUNT; i++) {
            int index = RefineryBlockEntity.SHARD_SLOT_START + i;
            if (be.getItems().get(index).isEmpty()) {
                return index;
            }
        }
        return -1;
    }

    public record SetClockSpeedPayload(BlockPos pos, int scaled) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, SetClockSpeedPayload> STREAM_CODEC =
            StreamCodec.of(SetClockSpeedPayload::encode, SetClockSpeedPayload::decode);

        private static SetClockSpeedPayload decode(FriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            int scaled = buf.readVarInt();
            return new SetClockSpeedPayload(pos, scaled);
        }

        private static void encode(FriendlyByteBuf buf, SetClockSpeedPayload payload) {
            buf.writeBlockPos(payload.pos);
            buf.writeVarInt(payload.scaled);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return SET_CLOCK_SPEED;
        }
    }

    public record PasteMachineSettingsPayload(BlockPos pos, ResourceLocation recipeIdOrNull, int scaledClock) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, PasteMachineSettingsPayload> STREAM_CODEC =
            StreamCodec.of(PasteMachineSettingsPayload::encode, PasteMachineSettingsPayload::decode);

        private static PasteMachineSettingsPayload decode(FriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            ResourceLocation recipeId = null;
            if (buf.readBoolean()) {
                recipeId = ResourceLocation.STREAM_CODEC.decode(buf);
            }
            int scaledClock = buf.readVarInt();
            return new PasteMachineSettingsPayload(pos, recipeId, scaledClock);
        }

        private static void encode(FriendlyByteBuf buf, PasteMachineSettingsPayload payload) {
            buf.writeBlockPos(payload.pos);
            buf.writeBoolean(payload.recipeIdOrNull() != null);
            if (payload.recipeIdOrNull() != null) {
                ResourceLocation.STREAM_CODEC.encode(buf, payload.recipeIdOrNull());
            }
            buf.writeVarInt(payload.scaledClock());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PASTE_MACHINE_SETTINGS;
        }
    }
}
