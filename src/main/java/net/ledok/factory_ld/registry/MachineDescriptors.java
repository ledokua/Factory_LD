package net.ledok.factory_ld.registry;

import java.util.List;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.ledok.factory_ld.world.block.GenericMachineBlock;
import net.ledok.factory_ld.world.block.entity.GenericMachineBlockEntity;
import net.ledok.factory_ld.world.block.entity.MachineSpec;
import net.ledok.factory_ld.world.block.entity.MachineSpecs;
import net.ledok.factory_ld.world.screen.GenericMachineScreenData;
import net.ledok.factory_ld.world.screen.GenericMachineScreenHandler;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MachineDescriptors {
    public record Descriptor(MachineSpec spec) {
        public String id() {
            return spec.id();
        }

        public Block createBlock() {
            return new GenericMachineBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK), id());
        }

        public BlockEntityType<? extends BlockEntity> createBlockEntityType(Block block) {
            return BlockEntityType.Builder.of((pos, state) -> new GenericMachineBlockEntity(pos, state, id()), block).build(null);
        }

        public MenuType<? extends AbstractContainerMenu> createMenuType() {
            return new ExtendedScreenHandlerType<>(
                (syncId, playerInventory, data) -> new GenericMachineScreenHandler(syncId, playerInventory, data, id()),
                GenericMachineScreenData.STREAM_CODEC
            );
        }

        @SuppressWarnings("unchecked")
        public void registerFluidStorage(BlockEntityType<? extends BlockEntity> type) {
            FluidStorage.SIDED.registerForBlockEntity(
                (GenericMachineBlockEntity blockEntity, net.minecraft.core.Direction direction) -> blockEntity.getFluidStorage(),
                (BlockEntityType<GenericMachineBlockEntity>) type
            );
        }
    }

    public static final Descriptor CONSTRUCTOR = new Descriptor(MachineSpecs.CONSTRUCTOR);
    public static final Descriptor ASSEMBLER = new Descriptor(MachineSpecs.ASSEMBLER);
    public static final Descriptor REFINERY = new Descriptor(MachineSpecs.REFINERY);
    public static final Descriptor BLENDER = new Descriptor(MachineSpecs.BLENDER);
    public static final Descriptor FOUNDRY = new Descriptor(MachineSpecs.FOUNDRY);
    public static final Descriptor MANUFACTURER = new Descriptor(MachineSpecs.MANUFACTURER);
    public static final Descriptor SMELTER = new Descriptor(MachineSpecs.SMELTER);

    public static final List<Descriptor> ALL = MachineSpecs.ALL.stream().map(Descriptor::new).toList();

    private MachineDescriptors() {
    }
}
