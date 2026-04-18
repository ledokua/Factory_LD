package net.ledok.factory_ld.registry;

import net.ledok.factory_ld.world.block.PowerPoleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;

public final class PowerPoleDescriptors {
    public record Descriptor(String id, int maxConnections) {
        public Block createBlock() {
            return new PowerPoleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK), id, maxConnections);
        }
    }

    public static final Descriptor POWER_POLE = new Descriptor("power_pole", 4);
    public static final Descriptor POWER_POLE_MK2 = new Descriptor("power_pole_mk2", 7);
    public static final Descriptor POWER_POLE_MK3 = new Descriptor("power_pole_mk3", 10);
    public static final List<Descriptor> ALL = List.of(POWER_POLE, POWER_POLE_MK2, POWER_POLE_MK3);

    private PowerPoleDescriptors() {
    }
}
