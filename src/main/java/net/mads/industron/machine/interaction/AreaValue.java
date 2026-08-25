package net.mads.industron.machine.interaction;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.MachineTierStats;

/**
 * Integer used by a machine area. The value can stay fixed or grow once for
 * every generated machine tier above the machine family's start tier.
 *
 * <p>Example: {@code AreaValue.fixed(5).plusPerTier(2)} resolves to 5 for the
 * first generated tier, 7 for the next tier and 9 for the tier after that.</p>
 */
public record AreaValue(int base, int perTier) {
    public static AreaValue fixed(int value) {
        return new AreaValue(value, 0);
    }

    public AreaValue plusPerTier(int amount) {
        return new AreaValue(base, amount);
    }

    public int resolve(MachineTier actualTier, MachineTier startTier) {
        int actual = MachineTierStats.tierIndex(actualTier.recipeTier());
        int start = MachineTierStats.tierIndex(startTier.recipeTier());
        return base + Math.max(0, actual - start) * perTier;
    }
}
