package net.ledok.factory_ld.world.power;

public final class PowerUnits {
    public static final double TICKS_PER_SECOND = 20.0;
    public static final double SECONDS_PER_HOUR = 3600.0;
    public static final double MJ_PER_MWH = 3600.0;

    private PowerUnits() {
    }

    public static double mwToMjPerTick(double powerMw) {
        return powerMw / TICKS_PER_SECOND;
    }

    public static double mjPerTickToMw(double energyPerTickMj) {
        return energyPerTickMj * TICKS_PER_SECOND;
    }

    public static double mwhToMj(double energyMwh) {
        return energyMwh * MJ_PER_MWH;
    }

    public static double mjToMwh(double energyMj) {
        return energyMj / MJ_PER_MWH;
    }
}
