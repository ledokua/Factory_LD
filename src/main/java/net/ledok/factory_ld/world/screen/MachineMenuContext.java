package net.ledok.factory_ld.world.screen;

import net.ledok.factory_ld.world.block.entity.AbstractMachineBlockEntity;

public interface MachineMenuContext {
    AbstractMachineBlockEntity getMachineBlockEntity();

    boolean isOverclockUnlocked();
}
