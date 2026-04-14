package net.ledok.factory_ld.world.screen;

import net.ledok.factory_ld.world.block.entity.AbstractMachineBlockEntity;
import net.ledok.factory_ld.world.power.PowerGridManager;
import net.ledok.factory_ld.world.power.PowerGridStats;

public interface MachineMenuContext {
    AbstractMachineBlockEntity getMachineBlockEntity();

    boolean isOverclockUnlocked();

    default PowerGridStats getPowerGridStats() {
        AbstractMachineBlockEntity machine = getMachineBlockEntity();
        if (machine == null || machine.getLevel() == null) {
            return PowerGridStats.EMPTY;
        }
        return PowerGridManager.getStats(machine.getLevel(), machine.getBlockPos());
    }
}
