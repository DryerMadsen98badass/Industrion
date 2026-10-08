package net.mads.industron.block.breaking;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.MachineTierStats;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipetypes.assembly.ToolVariantDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** The one place that turns block work + tool efficiency + tier + vanilla modifiers into break progress. */
public final class BlockBreakingCalculator {
    public static final double FIST_EFFICIENCY_SECONDS = 20.0D;
    public static final MachineTier FIST_TIER = MachineTier.ULV;

    private static final double TICKS_PER_SECOND = 20.0D;
    private static final double TIER_WORK_STEP = 0.25D;
    private static final double MIN_OVERCLOCKED_WORK = 0.50D;
    private static final float MIN_PROGRESS = 0.0F;

    private BlockBreakingCalculator() {
    }

    /**
     * Industron owns breaking only for its explicit block definitions, or when a registered
     * Industron tool is being used. Everything else is left to Minecraft/the owning mod.
     */
    public static boolean shouldOverride(
            Player player,
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        if (player == null || state == null || level == null || pos == null) {
            return false;
        }
        BlockBreakingProfile profile = BlockBreakingResolver.resolve(state, level, pos);
        return profile.explicitIndustronProfile() || isRegisteredTool(player.getMainHandItem());
    }

    public static float destroyProgress(
            Player player,
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        if (player == null || state == null || level == null || pos == null) {
            return MIN_PROGRESS;
        }
        if (player.getAbilities().instabuild) {
            return 1.0F;
        }

        BlockBreakingProfile profile = BlockBreakingResolver.resolve(state, level, pos);
        if (profile.unbreakable()) {
            return MIN_PROGRESS;
        }
        if (profile.instantBreak()) {
            return 1.0F;
        }

        ItemStack stack = player.getMainHandItem();
        ToolVariantDefinition heldTool = findAnyTool(stack);
        ToolVariantDefinition acceptedTool = findAcceptedTool(profile, stack);

        MachineTier actualTier = heldTool == null
                ? FIST_TIER
                : heldTool.tier().recipeTier();
        double baseSeconds = acceptedTool == null
                ? FIST_EFFICIENCY_SECONDS
                : acceptedTool.efficiency();

        double adjustedWork = tierAdjustedWork(profile.work(), profile.requiredTier(), actualTier);
        double vanillaModifier = vanillaPlayerModifier(player, state, stack);
        double seconds = baseSeconds * adjustedWork;
        seconds /= Math.max(0.000001D, vanillaModifier);

        if (!Double.isFinite(seconds) || seconds <= 0.0D) {
            return 1.0F;
        }
        return (float) Math.min(1.0D, 1.0D / (seconds * TICKS_PER_SECOND));
    }

    /**
     * Tier affects the block work linearly instead of exponentially:
     * one tier above = -0.25 work, one tier below = +0.25 work.
     * The overclock floor never increases a naturally softer block.
     */
    static double tierAdjustedWork(float work, MachineTier requiredTier, MachineTier actualTier) {
        if (work <= 0.0F) {
            return work;
        }

        int required = MachineTierStats.tierIndex(requiredTier.recipeTier());
        int actual = MachineTierStats.tierIndex(actualTier.recipeTier());
        int difference = actual - required;
        double adjusted = work - (difference * TIER_WORK_STEP);

        if (difference > 0) {
            double floor = Math.min(work, MIN_OVERCLOCKED_WORK);
            adjusted = Math.max(floor, adjusted);
        }
        return adjusted;
    }

    /** Tier alone controls whether normal block loot/Assembly salvage is allowed. */
    public static boolean hasSufficientTier(ItemStack stack, BlockBreakingProfile profile) {
        ToolVariantDefinition tool = findAnyTool(stack);
        MachineTier actualTier = tool == null ? FIST_TIER : tool.tier().recipeTier();
        return MachineTierStats.isAtLeast(actualTier, profile.requiredTier().recipeTier());
    }

    public static boolean hasSufficientTier(
            ItemStack stack,
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return hasSufficientTier(stack, BlockBreakingResolver.resolve(state, level, pos));
    }

    public static boolean isRegisteredTool(ItemStack stack) {
        return findAnyTool(stack) != null;
    }

    private static ToolVariantDefinition findAcceptedTool(BlockBreakingProfile profile, ItemStack stack) {
        if (stack.isEmpty() || profile.toolTypeIds().isEmpty()) {
            return null;
        }
        ToolVariantDefinition best = null;
        for (String toolTypeId : profile.toolTypeIds()) {
            ToolVariantDefinition candidate = AssemblyTools.find(toolTypeId, stack);
            if (candidate == null) {
                continue;
            }
            if (best == null || candidate.efficiency() < best.efficiency()) {
                best = candidate;
            }
        }
        return best;
    }

    private static ToolVariantDefinition findAnyTool(ItemStack stack) {
        return AssemblyTools.findAny(stack);
    }

    /**
     * Retains Haste, Mining Fatigue, underwater/airborne penalties and other modifiers Minecraft
     * already contributes through Player#getDestroySpeed. For a correctly registered tool this
     * also retains Efficiency enchantment behavior without multiplying by the item's base speed twice.
     */
    private static double vanillaPlayerModifier(Player player, BlockState state, ItemStack stack) {
        float itemBase = stack.isEmpty() ? 1.0F : stack.getDestroySpeed(state);
        float playerSpeed = player.getDestroySpeed(state);
        if (!Float.isFinite(itemBase) || itemBase <= 0.0F || !Float.isFinite(playerSpeed) || playerSpeed <= 0.0F) {
            return 1.0D;
        }
        return Math.max(0.000001D, playerSpeed / itemBase);
    }
}
