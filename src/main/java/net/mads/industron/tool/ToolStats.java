package net.mads.industron.tool;

import net.mads.industron.machine.MachineTier;

/** Final derived gameplay stats for one composed tool stack. */
public record ToolStats(
        MachineTier tier,
        int durability,
        double efficiencySeconds,
        double damage
) {
    public ToolStats {
        if (tier == null || tier == MachineTier.NONE) throw new IllegalArgumentException("Tool requires a real tier");
        if (durability < 1) throw new IllegalArgumentException("Tool durability must be positive");
        if (!Double.isFinite(efficiencySeconds) || efficiencySeconds <= 0.0D) {
            throw new IllegalArgumentException("Tool efficiency must be positive");
        }
        if (!Double.isFinite(damage) || damage < 0.0D) throw new IllegalArgumentException("Tool damage cannot be negative");
    }
}
