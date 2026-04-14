package net.ledok.factory_ld.world.screen;

public record MachineGuiLayout(
    int playerInvY,
    int playerHotbarY,
    int shardSlotY,
    int[] shardSlotXs,
    int[] itemInputXs,
    int[] itemInputYs,
    int[] fluidInputXs,
    int[] fluidInputYs,
    int[] itemOutputXs,
    int[] itemOutputYs,
    int[] fluidOutputXs,
    int[] fluidOutputYs
) {
    public MachineGuiLayout {
        shardSlotXs = shardSlotXs == null ? new int[0] : shardSlotXs.clone();
        itemInputXs = itemInputXs == null ? new int[0] : itemInputXs.clone();
        itemInputYs = itemInputYs == null ? new int[0] : itemInputYs.clone();
        fluidInputXs = fluidInputXs == null ? new int[0] : fluidInputXs.clone();
        fluidInputYs = fluidInputYs == null ? new int[0] : fluidInputYs.clone();
        itemOutputXs = itemOutputXs == null ? new int[0] : itemOutputXs.clone();
        itemOutputYs = itemOutputYs == null ? new int[0] : itemOutputYs.clone();
        fluidOutputXs = fluidOutputXs == null ? new int[0] : fluidOutputXs.clone();
        fluidOutputYs = fluidOutputYs == null ? new int[0] : fluidOutputYs.clone();
    }

    public int shardSlotX(int index) {
        return shardSlotXs[index];
    }

    public int itemInputX(int index) {
        return itemInputXs[index];
    }

    public int itemInputY(int index) {
        return itemInputYs[index];
    }

    public int fluidInputX(int index) {
        return fluidInputXs[index];
    }

    public int fluidInputY(int index) {
        return fluidInputYs[index];
    }

    public int itemOutputX(int index) {
        return itemOutputXs[index];
    }

    public int itemOutputY(int index) {
        return itemOutputYs[index];
    }

    public int fluidOutputX(int index) {
        return fluidOutputXs[index];
    }

    public int fluidOutputY(int index) {
        return fluidOutputYs[index];
    }

    public int itemInputCount() {
        return itemInputXs.length;
    }

    public int fluidInputCount() {
        return fluidInputXs.length;
    }

    public int itemOutputCount() {
        return itemOutputXs.length;
    }

    public int fluidOutputCount() {
        return fluidOutputXs.length;
    }
}
