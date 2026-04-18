package net.ledok.factory_ld.registry;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.ledok.factory_ld.FactoryLdMod;
import net.ledok.factory_ld.world.block.entity.AbstractMachineBlockEntity;
import net.ledok.factory_ld.world.block.entity.OverclockMachineEntity;
import net.ledok.factory_ld.world.block.entity.OverclockMachineHelper;
import net.ledok.factory_ld.world.power.PowerNetworkManager;
import net.ledok.factory_ld.world.screen.MachineMenuContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class ModNetworking {
    public static final CustomPacketPayload.Type<SetClockSpeedPayload> SET_CLOCK_SPEED =
        new CustomPacketPayload.Type<>(FactoryLdMod.id("set_clock_speed"));
    public static final CustomPacketPayload.Type<PasteMachineSettingsPayload> PASTE_MACHINE_SETTINGS =
        new CustomPacketPayload.Type<>(FactoryLdMod.id("paste_machine_settings"));
    public static final CustomPacketPayload.Type<PowerLinksSnapshotPayload> POWER_LINKS_SNAPSHOT =
        new CustomPacketPayload.Type<>(FactoryLdMod.id("power_links_snapshot"));
    public static final CustomPacketPayload.Type<PowerLinkDeltaPayload> POWER_LINK_DELTA =
        new CustomPacketPayload.Type<>(FactoryLdMod.id("power_link_delta"));

    private ModNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(SET_CLOCK_SPEED, SetClockSpeedPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PASTE_MACHINE_SETTINGS, PasteMachineSettingsPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(POWER_LINKS_SNAPSHOT, PowerLinksSnapshotPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(POWER_LINK_DELTA, PowerLinkDeltaPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SET_CLOCK_SPEED, (payload, context) -> {
            handleSetClockSpeed(context.player(), payload.pos(), payload.scaled());
        });
        ServerPlayNetworking.registerGlobalReceiver(PASTE_MACHINE_SETTINGS, (payload, context) -> {
            handlePasteMachineSettings(context.player(), payload.pos(), payload.recipeIdOrNull(), payload.scaledClock());
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sendPowerLinksSnapshot(handler.player);
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

    public static void sendPowerLinksSnapshot(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        List<PowerNetworkManager.PowerLink> links = PowerNetworkManager.getLinks(level);
        long[] packedLinks = new long[links.size() * 2];
        for (int i = 0; i < links.size(); i++) {
            PowerNetworkManager.PowerLink link = links.get(i);
            packedLinks[i * 2] = link.a();
            packedLinks[i * 2 + 1] = link.b();
        }
        ServerPlayNetworking.send(player, new PowerLinksSnapshotPayload(level.dimension(), packedLinks));
    }

    public static void broadcastPowerLinkDelta(ServerLevel level, long a, long b, boolean connected) {
        if (level == null) {
            return;
        }
        PowerLinkDeltaPayload payload = new PowerLinkDeltaPayload(level.dimension(), a, b, connected);
        for (ServerPlayer player : level.players()) {
            ServerPlayNetworking.send(player, payload);
        }
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

    public record PowerLinksSnapshotPayload(ResourceKey<Level> levelKey, long[] links) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, PowerLinksSnapshotPayload> STREAM_CODEC =
            StreamCodec.of(PowerLinksSnapshotPayload::encode, PowerLinksSnapshotPayload::decode);

        private static PowerLinksSnapshotPayload decode(FriendlyByteBuf buf) {
            ResourceKey<Level> levelKey = buf.readResourceKey(Registries.DIMENSION);
            int size = buf.readVarInt();
            long[] links = new long[Math.max(0, size)];
            for (int i = 0; i < links.length; i++) {
                links[i] = buf.readLong();
            }
            return new PowerLinksSnapshotPayload(levelKey, links);
        }

        private static void encode(FriendlyByteBuf buf, PowerLinksSnapshotPayload payload) {
            buf.writeResourceKey(payload.levelKey());
            buf.writeVarInt(payload.links.length);
            for (long value : payload.links) {
                buf.writeLong(value);
            }
        }

        public List<long[]> toPairs() {
            List<long[]> pairs = new ArrayList<>(links.length / 2);
            for (int i = 0; i + 1 < links.length; i += 2) {
                pairs.add(new long[]{links[i], links[i + 1]});
            }
            return pairs;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return POWER_LINKS_SNAPSHOT;
        }
    }

    public record PowerLinkDeltaPayload(ResourceKey<Level> levelKey, long a, long b, boolean connected) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, PowerLinkDeltaPayload> STREAM_CODEC =
            StreamCodec.of(PowerLinkDeltaPayload::encode, PowerLinkDeltaPayload::decode);

        private static PowerLinkDeltaPayload decode(FriendlyByteBuf buf) {
            ResourceKey<Level> levelKey = buf.readResourceKey(Registries.DIMENSION);
            long a = buf.readLong();
            long b = buf.readLong();
            boolean connected = buf.readBoolean();
            return new PowerLinkDeltaPayload(levelKey, a, b, connected);
        }

        private static void encode(FriendlyByteBuf buf, PowerLinkDeltaPayload payload) {
            buf.writeResourceKey(payload.levelKey());
            buf.writeLong(payload.a());
            buf.writeLong(payload.b());
            buf.writeBoolean(payload.connected());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return POWER_LINK_DELTA;
        }
    }
}
