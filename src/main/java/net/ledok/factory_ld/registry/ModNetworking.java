package net.ledok.factory_ld.registry;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.ledok.factory_ld.FactoryLdMod;
import net.ledok.factory_ld.world.block.entity.AbstractMachineBlockEntity;
import net.ledok.factory_ld.world.block.entity.OverclockMachineEntity;
import net.ledok.factory_ld.world.block.entity.OverclockMachineHelper;
import net.ledok.factory_ld.world.screen.MachineMenuContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

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
        if (!(player.containerMenu instanceof MachineMenuContext menu)) {
            return;
        }
        if (!(player.level().getBlockEntity(pos) instanceof AbstractMachineBlockEntity machine)) {
            return;
        }
        if (menu.getMachineBlockEntity() != machine) {
            return;
        }
        applySetClockSpeed(machine, scaled, menu.isOverclockUnlocked());
    }

    private static void handlePasteMachineSettings(ServerPlayer player, BlockPos pos, ResourceLocation recipeId, int scaledClock) {
        if (!(player.containerMenu instanceof MachineMenuContext menu)) {
            return;
        }
        if (!(player.level().getBlockEntity(pos) instanceof AbstractMachineBlockEntity machine)) {
            return;
        }
        if (menu.getMachineBlockEntity() != machine) {
            return;
        }
        OverclockMachineHelper.applyPastedSettings(player.getInventory(), recipeId, scaledClock, menu.isOverclockUnlocked(), machine);
    }

    private static void applySetClockSpeed(OverclockMachineEntity machine, int scaled, boolean overclockUnlocked) {
        double percent = scaled / 10000.0;
        if (!overclockUnlocked) {
            percent = Math.min(percent, 100.0);
        }
        machine.setClockSpeedPercent(percent);
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
