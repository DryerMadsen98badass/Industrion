package net.mads.industron.block.breaking;

import net.mads.industron.machine.MachineTier;

import java.util.Set;

/**
 * Resolved mining data for one block state.
 *
 * @param toolTypeIds tool-family ids accepted by the block (pickaxe, axe, shovel, ...)
 * @param requiredTier minimum Industron tier required by the preferred/required tool
 * @param work block work amount; normally Minecraft destroy-speed/hardness
 * @param explicitIndustronProfile true when the profile came from an Industron definition/material source
 */
public record BlockBreakingProfile(
        Set<String> toolTypeIds,
        MachineTier requiredTier,
        float work,
        boolean explicitIndustronProfile
) {
    public BlockBreakingProfile {
        toolTypeIds = toolTypeIds == null ? Set.of() : Set.copyOf(toolTypeIds);
        if (requiredTier == null || requiredTier == MachineTier.NONE) {
            throw new IllegalArgumentException("Block breaking tier must be a real machine tier");
        }
        if (Float.isNaN(work)) {
            throw new IllegalArgumentException("Block breaking work cannot be NaN");
        }
    }

    public boolean unbreakable() {
        return work < 0.0F;
    }

    public boolean instantBreak() {
        return work == 0.0F;
    }
}
