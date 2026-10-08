package net.mads.industron.tool;

import net.mads.industron.machine.MachineTier;

/** Immutable contribution made by one permanent tool part. */
public record ToolPartStats(
        MachineTier tier,
        int durability,
        double efficiencySeconds,
        double damage
) {
    public ToolPartStats {
        if (tier == null || tier == MachineTier.NONE) throw new IllegalArgumentException("Tool part requires a real tier");
        if (durability < 1) throw new IllegalArgumentException("Tool part durability must be positive");
        if (!Double.isFinite(efficiencySeconds) || efficiencySeconds <= 0.0D) {
            throw new IllegalArgumentException("Tool part efficiency must be positive");
        }
        if (!Double.isFinite(damage) || damage < 0.0D) throw new IllegalArgumentException("Tool part damage cannot be negative");
    }
}
