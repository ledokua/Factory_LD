package net.ledok.factory_ld.world.screen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record GenericMachineScreenData(BlockPos pos, List<ResourceLocation> unlockedRecipes, boolean overclockUnlocked) {
    public static final StreamCodec<RegistryFriendlyByteBuf, GenericMachineScreenData> STREAM_CODEC =
        StreamCodec.of(GenericMachineScreenData::encode, GenericMachineScreenData::decode);

    private static GenericMachineScreenData decode(RegistryFriendlyByteBuf buf) {
        BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
        int size = buf.readVarInt();
        List<ResourceLocation> unlocked = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            unlocked.add(ResourceLocation.STREAM_CODEC.decode(buf));
        }
        boolean overclockUnlocked = buf.readBoolean();
        return new GenericMachineScreenData(pos, unlocked, overclockUnlocked);
    }

    private static void encode(RegistryFriendlyByteBuf buf, GenericMachineScreenData data) {
        BlockPos.STREAM_CODEC.encode(buf, data.pos);
        buf.writeVarInt(data.unlockedRecipes.size());
        for (ResourceLocation id : data.unlockedRecipes) {
            ResourceLocation.STREAM_CODEC.encode(buf, id);
        }
        buf.writeBoolean(data.overclockUnlocked);
    }
}
