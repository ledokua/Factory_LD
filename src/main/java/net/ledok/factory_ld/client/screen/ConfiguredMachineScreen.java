package net.ledok.factory_ld.client.screen;

import net.ledok.factory_ld.world.screen.AbstractMachineScreenHandler;
import net.ledok.factory_ld.world.screen.MachineScreenConfigProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ConfiguredMachineScreen<H extends AbstractMachineScreenHandler<?, ?> & MachineScreenConfigProvider>
    extends AbstractFluidMachineScreen<H> {
    public ConfiguredMachineScreen(H handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected int playerInventoryY() {
        return menu.playerInventoryY();
    }

    @Override
    protected int shardSlotY() {
        return menu.shardSlotY();
    }

    @Override
    protected int[] inputGhostSlotIndices() {
        return menu.inputGhostSlotIndices();
    }

    @Override
    protected int outputGhostSlotIndex() {
        return menu.outputGhostSlotIndex();
    }

    @Override
    protected boolean supportsFluidSlots() {
        return menu.supportsFluidSlots();
    }

    @Override
    protected int fluidInputSlotCount() {
        return menu.fluidInputSlotCount();
    }

    @Override
    protected int fluidOutputSlotCount() {
        return menu.fluidOutputSlotCount();
    }

    @Override
    protected int fluidInputSlotX() {
        return menu.fluidInputSlotX();
    }

    @Override
    protected int fluidInputSlotY() {
        return menu.fluidInputSlotY();
    }

    @Override
    protected int fluidOutputSlotX() {
        return menu.fluidOutputSlotX();
    }

    @Override
    protected int fluidOutputSlotY() {
        return menu.fluidOutputSlotY();
    }

    @Override
    protected int fluidInputSlotX(int index) {
        return menu.fluidInputSlotX(index);
    }

    @Override
    protected int fluidInputSlotY(int index) {
        return menu.fluidInputSlotY(index);
    }

    @Override
    protected int fluidOutputSlotX(int index) {
        return menu.fluidOutputSlotX(index);
    }

    @Override
    protected int fluidOutputSlotY(int index) {
        return menu.fluidOutputSlotY(index);
    }

    @Override
    protected String machineTypeKey() {
        return menu.machineTypeKey();
    }

    @Override
    protected ProductionStatus getProductionStatus() {
        return switch (menu.getUiProductionStatus()) {
            case WORKING -> ProductionStatus.WORKING;
            case NO_POWER -> ProductionStatus.NO_POWER;
            case IDLE -> ProductionStatus.IDLE;
        };
    }
}
