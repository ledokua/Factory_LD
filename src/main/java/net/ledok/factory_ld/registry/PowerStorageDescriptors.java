package net.ledok.factory_ld.registry;

import net.ledok.factory_ld.world.block.GenericPowerStorageBlock;
import net.ledok.factory_ld.world.block.entity.GenericPowerStorageBlockEntity;
import net.ledok.factory_ld.world.block.entity.PowerStorageSpec;
import net.ledok.factory_ld.world.block.entity.PowerStorageSpecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;

public final class PowerStorageDescriptors {
    public record Descriptor(PowerStorageSpec spec) {
        public String id() {
            return spec.id();
        }

        public Block createBlock() {
            return new GenericPowerStorageBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK), id());
        }

        public BlockEntityType<? extends BlockEntity> createBlockEntityType(Block block) {
            return BlockEntityType.Builder.of((pos, state) -> new GenericPowerStorageBlockEntity(pos, state, id()), block).build(null);
        }
    }

    public static final Descriptor POWER_STORAGE = new Descriptor(PowerStorageSpecs.POWER_STORAGE);
    public static final List<Descriptor> ALL = PowerStorageSpecs.ALL.stream().map(Descriptor::new).toList();

    private PowerStorageDescriptors() {
    }
}
