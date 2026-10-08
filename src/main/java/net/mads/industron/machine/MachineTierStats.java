package net.mads.industron.machine;

public final class MachineTierStats {
    private static final int[] ITEM_SLOTS = {1, 4, 9, 16, 25, 36, 49, 64, 81, 100};
    private static final long[] CE_VOLTAGES = {8L, 32L, 256L, 1_024L, 4_096L, 16_384L};
    private static final int MAX_FLUID_CAPACITY = 1_024_000;
    private static final int MAX_IO_ITEM_SLOTS = 49;
    private static final float MAX_KINETIC_STRESS_PER_RPM = 16_384.0F;

    private MachineTierStats() {
    }

    public static int tierIndex(MachineTier tier) {
        int index = MachineTier.ALL.indexOf(tier == null ? MachineTier.ULV : tier.recipeTier());
        return Math.max(index, 0);
    }


    /** Default block hardness for a breaking tier. Matches the former ULV-EV progression directly. */
    public static float blockHardness(MachineTier tier) {
        int index = tierIndex(tier == null ? MachineTier.ULV : tier.recipeTier());
        float hardness = (index + 1) * (8.0F / 3.0F);
        hardness = Math.min(12.0F, hardness);
        return Math.round(hardness * 100.0F) / 100.0F;
    }

    /** Default explosion resistance for a breaking tier. */
    public static float blockResistance(MachineTier tier) {
        int index = tierIndex(tier == null ? MachineTier.ULV : tier.recipeTier());
        return 4.0F * (index + 1);
    }

    public static boolean isAtLeast(MachineTier actual, MachineTier required) {
        return tierIndex(actual) >= tierIndex(required);
    }

    public static boolean isAtMost(MachineTier actual, MachineTier maximum) {
        return tierIndex(actual) <= tierIndex(maximum);
    }

    public static MachineTier max(MachineTier first, MachineTier second) {
        return tierIndex(first) >= tierIndex(second) ? first : second;
    }

    public static MachineTier offset(MachineTier tier, int offset) {
        int index = Math.max(0, Math.min(ALL_SIZE - 1, tierIndex(tier) + offset));
        return MachineTier.ALL.get(index);
    }

    public static MachineTier next(MachineTier tier) {
        return offset(tier, 1);
    }

    public static int itemBusSlots(MachineTier tier) {
        int index = Math.min(tierIndex(tier), ITEM_SLOTS.length - 1);
        return ITEM_SLOTS[index];
    }

    public static int mufflerSlots(MachineTier tier) {
        return itemBusSlots(tier);
    }

    public static float mufflerDurationMultiplier(MachineTier tier) {
        return Math.max(0.20F, 1.60F - 0.20F * tierIndex(tier));
    }

    public static int mufflerDurationPercent(MachineTier tier) {
        return Math.round(mufflerDurationMultiplier(tier) * 100.0F);
    }

    public static int ioInterfaceItemSlots(MachineTier tier) {
        return Math.min(itemBusSlots(tier), MAX_IO_ITEM_SLOTS);
    }

    public static int fluidTankCapacity(MachineTier tier) {
        long capacity = 4_000L << Math.min(tierIndex(tier), 30);
        return (int) Math.min(capacity, MAX_FLUID_CAPACITY);
    }

    public static int ioInterfaceFluidTanks() {
        return 4;
    }

    public static long ceCapacity(MachineTier tier) {
        return saturatedMultiply(ceTier(tier), 64L);
    }

    public static long ceTier(MachineTier tier) {
        int index = tierIndex(tier);
        if (index < CE_VOLTAGES.length) {
            return CE_VOLTAGES[index];
        }

        long voltage = CE_VOLTAGES[CE_VOLTAGES.length - 1];
        for (int current = CE_VOLTAGES.length; current <= index; current++) {
            voltage = saturatedMultiply(voltage, 4L);
        }
        return voltage;
    }

    /**
     * Scales a machine's ULV base usage by four for every concrete machine tier.
     */
    public static int machineEnergyUsage(int baseUsage, MachineTier tier) {
        if (baseUsage <= 0) {
            return 0;
        }

        long usage = baseUsage;
        for (int i = 0; i < tierIndex(tier.recipeTier()); i++) {
            usage = Math.min(Integer.MAX_VALUE, usage * 4L);
        }
        return (int) usage;
    }

    /**
     * Scales a kinetic machine's SU/RPM from its first generated tier.
     * Every real tier step multiplies the value by four, including tier gaps
     * created with {@code onlyTier(...)}.
     */
    public static double machineKineticSuPerRpm(
            double startSu,
            MachineTier startTier,
            MachineTier actualTier
    ) {
        if (!Double.isFinite(startSu) || startSu <= 0.0D) {
            return 0.0D;
        }

        int steps = Math.max(
                0,
                tierIndex(actualTier.recipeTier())
                        - tierIndex(startTier.recipeTier())
        );

        double su = startSu;
        for (int i = 0; i < steps; i++) {
            if (su > Double.MAX_VALUE / 4.0D) {
                return Double.MAX_VALUE;
            }
            su *= 4.0D;
        }
        return su;
    }

    public static int ceBaseAmps(MachineTier tier) {
        int index = Math.min(tierIndex(tier), 30);
        return 1 << index;
    }

    public static MachineTier tierForCEt(long cet) {
        long required = cet == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(cet);
        for (MachineTier tier : MachineTier.ALL) {
            if (ceTier(tier) >= required) {
                return tier;
            }
        }
        return MachineTier.ALL.get(MachineTier.ALL.size() - 1);
    }

    public static MachineTier tierForVoltage(long voltage) {
        long required = voltage == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(voltage);
        MachineTier result = MachineTier.ULV;
        for (MachineTier tier : MachineTier.ALL) {
            result = tier;
            if (ceTier(tier) >= required) {
                return tier;
            }
        }
        return result;
    }

    public static int tierOverclockFactor(MachineTier required, MachineTier actual) {
        int tierDifference = Math.max(0, tierIndex(actual) - tierIndex(required));
        return 1 << Math.min(tierDifference, 30);
    }

    public static int ceOverclockMultiplier(MachineTier required, MachineTier actual) {
        int tierDifference = Math.max(0, tierIndex(actual) - tierIndex(required));
        int multiplier = 1;
        for (int i = 0; i < tierDifference; i++) {
            if (multiplier > Integer.MAX_VALUE / 4) {
                return Integer.MAX_VALUE;
            }
            multiplier *= 4;
        }
        return multiplier;
    }

    private static final int ALL_SIZE = MachineTier.ALL.size();

    private static long saturatedMultiply(long value, long multiplier) {
        if (value > Long.MAX_VALUE / multiplier) {
            return Long.MAX_VALUE;
        }
        return value * multiplier;
    }

    public static float kineticStressPerRpm(MachineTier tier) {
        float stress = 16.0F;
        for (int i = 0; i < tierIndex(tier); i++) {
            stress *= 4.0F;
            if (stress >= MAX_KINETIC_STRESS_PER_RPM) {
                return MAX_KINETIC_STRESS_PER_RPM;
            }
        }
        return stress;
    }
}
