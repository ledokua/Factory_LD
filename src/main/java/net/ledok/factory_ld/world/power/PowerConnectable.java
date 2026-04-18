package net.ledok.factory_ld.world.power;

import net.minecraft.world.phys.Vec3;

public interface PowerConnectable {
    int maxPowerConnections();

    default Vec3 powerConnectionOffset() {
        return new Vec3(0.5, 0.75, 0.5);
    }
}
