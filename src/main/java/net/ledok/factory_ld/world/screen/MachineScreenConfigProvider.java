package net.ledok.factory_ld.world.screen;

public interface MachineScreenConfigProvider {
    enum UiProductionStatus {
        IDLE("idle"),
        WORKING("working"),
        NO_POWER("no_power");

        private final String key;

        UiProductionStatus(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    int playerInventoryY();

    int shardSlotY();

    int[] inputGhostSlotIndices();

    int outputGhostSlotIndex();

    String machineTypeKey();

    default boolean supportsFluidSlots() {
        return false;
    }

    default int fluidInputSlotCount() {
        return supportsFluidSlots() ? 1 : 0;
    }

    default int fluidOutputSlotCount() {
        return supportsFluidSlots() ? 1 : 0;
    }

    default int fluidInputSlotX() {
        return 0;
    }

    default int fluidInputSlotY() {
        return 0;
    }

    default int fluidOutputSlotX() {
        return 0;
    }

    default int fluidOutputSlotY() {
        return 0;
    }

    default int fluidInputSlotX(int index) {
        return index == 0 ? fluidInputSlotX() : 0;
    }

    default int fluidInputSlotY(int index) {
        return index == 0 ? fluidInputSlotY() : 0;
    }

    default int fluidOutputSlotX(int index) {
        return index == 0 ? fluidOutputSlotX() : 0;
    }

    default int fluidOutputSlotY(int index) {
        return index == 0 ? fluidOutputSlotY() : 0;
    }

    UiProductionStatus getUiProductionStatus();
}
