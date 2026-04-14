package net.ledok.factory_ld.world.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record PowerEmitterScreenData(BlockPos pos) {
    public static final StreamCodec<RegistryFriendlyByteBuf, PowerEmitterScreenData> STREAM_CODEC =
        StreamCodec.of(PowerEmitterScreenData::encode, PowerEmitterScreenData::decode);

    private static PowerEmitterScreenData decode(RegistryFriendlyByteBuf buf) {
        return new PowerEmitterScreenData(buf.readBlockPos());
    }

    private static void encode(RegistryFriendlyByteBuf buf, PowerEmitterScreenData data) {
        buf.writeBlockPos(data.pos());
    }
}
