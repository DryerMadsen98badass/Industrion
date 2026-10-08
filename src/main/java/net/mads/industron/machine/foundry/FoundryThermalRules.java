package net.mads.industron.machine.foundry;

/** Pure deterministic balancing rules. Heat units are gameplay units per server tick. */
public final class FoundryThermalRules {
    public static final int ITEM_SLOTS_PER_INSIDE_VOLUME_BLOCK = 1;
    public static final double AMBIENT_C = 20.0;
    public static final int DAMAGE_INTERVAL_TICKS = 20;
    public static final double MASS_PER_INSIDE_BLOCK = 10.0;
    public static final double MASS_PER_BRICK = 1.0;
    public static final double HEAT_PER_COIL_DEGREE = 0.20;
    public static final double WARMUP_SPEED_DIVISOR = 10.0;
    public static final double LOSS_PER_BRICK_DEGREE = 0.0005;
    public static final double DAMAGE_THRESHOLD = 1.0;
    // 100 C over the limit destroys a brick after 120 seconds of continuous exposure.
    public static final double DAMAGE_PER_EXCESS_DEGREE_TICK = 1.0 / 240000.0;

    private FoundryThermalRules() {}

    public static int itemSlots(int insideVolume) {
        return (int) Math.min(Integer.MAX_VALUE - 27L,
                Math.max(0L, (long) insideVolume) * ITEM_SLOTS_PER_INSIDE_VOLUME_BLOCK);
    }

    public static double thermalMass(int volume, int bricks) {
        return Math.max(1.0, Math.max(0, volume) * MASS_PER_INSIDE_BLOCK
                + Math.max(0, bricks) * MASS_PER_BRICK);
    }

    public static double nextTemperature(double current, double heat, double sourceLimit,
                                         int volume, int bricks) {
        double mass = thermalMass(volume, bricks);
        double loss = Math.max(0.0, current - AMBIENT_C)
                * Math.max(1, bricks) * LOSS_PER_BRICK_DEGREE;
        // A cooler source cannot increase an already hotter foundry's temperature.
        double supplied = Math.min(Math.max(0.0, heat), Math.max(0.0, sourceLimit - current) * mass);
        double change = (supplied - loss) / mass;
        // Slow positive warmup without changing the temperature limit or cooling.
        if (change > 0.0) change /= WARMUP_SPEED_DIVISOR;
        return Math.max(AMBIENT_C, current + change);
    }

    public static double brickDamagePerTick(double temperature, int limit) {
        return Math.max(0.0, temperature - limit) * DAMAGE_PER_EXCESS_DEGREE_TICK;
    }
}
