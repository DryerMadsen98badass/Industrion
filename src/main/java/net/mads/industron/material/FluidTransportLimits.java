package net.mads.industron.material;

/** Shared balancing limits for generated fluid transport equipment. */
public final class FluidTransportLimits {
    public static final int MAX_TANK_CAPACITY = 10_000_000;
    public static final int MAX_PUMP_MB_PER_TICK = MAX_TANK_CAPACITY / 100;
    public static final int REFERENCE_RPM = 256;
    public static final double MAX_PUMP_RATE = (double) MAX_PUMP_MB_PER_TICK / REFERENCE_RPM;
    public static final int MAX_PUMP_STRESS_IMPACT = 1024;

    private FluidTransportLimits() {}

    public static int tankCapacity(int perBlock, int blocks) {
        // The material limit applies per block; connected tanks grow up to int capacity.
        int blockCapacity = Math.max(1, Math.min(MAX_TANK_CAPACITY, perBlock));
        return (int) Math.min(Integer.MAX_VALUE,
                (long) blockCapacity * Math.max(1, blocks));
    }

    /** Geometric tier ceilings; only the final tier can reach the global maximum.
     * Material properties determine performance within 65-100% of that ceiling.
     */
    public static double tierCapacity(int tierIndex, int tierCount, int score,
                                      double firstTierCapacity, double maximum) {
        int lastTier = Math.max(1, tierCount - 1);
        int tier = Math.max(0, Math.min(lastTier, tierIndex));
        double progression = tier / (double) lastTier;
        double ceiling = firstTierCapacity * Math.pow(maximum / firstTierCapacity, progression);
        double intrinsic = Math.max(0.0D, Math.min(1.0D,
                (score - tierIndex * 65.0D - 5.0D) / 250.0D));
        return Math.min(maximum, ceiling * (0.65D + intrinsic * 0.35D));
    }

    /** Stress impact is SU per RPM; Create applies the actual shaft speed. */
    public static int pumpStress(int tierMultiplier, double rate, int frictionScore) {
        double flowLoad = Math.sqrt(Math.max(1.0D, rate / 4.0D));
        double frictionLoad = 1.0D + Math.max(0, Math.min(100, frictionScore)) / 100.0D;
        return (int) Math.max(1, Math.min(MAX_PUMP_STRESS_IMPACT,
                Math.round(4.0D * Math.max(1, tierMultiplier) * flowLoad * frictionLoad)));
    }
}
