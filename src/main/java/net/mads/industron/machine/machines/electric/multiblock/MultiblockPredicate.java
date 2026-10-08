package net.mads.industron.machine.machines.electric.multiblock;

import net.mads.industron.machine.MachineTier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MultiblockPredicate {
    Match match(Level level, BlockPos pos, BlockState state);

    default MultiblockVisualization.SymbolInfo visualizationInfo() {
        return null;
    }

    default List<DisplayOption> displayOptions(MachineTier tier) {
        if (!allowsBuildTier(tier)) {
            return List.of();
        }
        MultiblockVisualization.SymbolInfo info = visualizationInfo();
        return info == null ? List.of() : List.of(DisplayOption.of(info));
    }

    default List<CountRequirement> countRequirements() {
        return List.of();
    }

    default MultiblockPredicate or(MultiblockPredicate other) {
        return net.mads.industron.machine.machines.electric.multiblock.MultiblockPredicates.or(this, other);
    }

    /**
     * Uses a block model as the formed overlay for matching machine ports.
     * Example: industron:block/bronze_machine_casing loads
     * assets/industron/models/block/bronze_machine_casing.json.
     */
    default MultiblockPredicate overlay(String model) {
        return net.mads.industron.machine.machines.electric.multiblock.MultiblockPredicates.overlay(this, model);
    }

    /** Uses a dynamic model resolved from the formed multiblock, for example {@code model(casing())}. */
    default MultiblockPredicate model(MultiblockModelSource source) {
        return net.mads.industron.machine.machines.electric.multiblock.MultiblockPredicates.model(this, source);
    }

    default MultiblockPredicate min(int minimum) {
        return net.mads.industron.machine.machines.electric.multiblock.MultiblockPredicates.min(this, minimum);
    }

    default MultiblockPredicate max(int maximum) {
        return net.mads.industron.machine.machines.electric.multiblock.MultiblockPredicates.max(this, maximum);
    }

    default MultiblockPredicate Tier(MachineTier tier) {
        return net.mads.industron.machine.machines.electric.multiblock.MultiblockPredicates.tierUpTo(this, tier);
    }

    /** Marks this ITEM_INPUT predicate as one ordered sequenced input position. */
    default MultiblockPredicate sequentialInput(int index) {
        return net.mads.industron.machine.machines.electric.multiblock.MultiblockPredicates.sequentialInput(this, index);
    }

    /** Returns the ordered sequenced-input index, or 0 when this predicate is not sequenced. */
    default int sequentialInputIndex() {
        return 0;
    }

    /** Marks this FLUID_OUTPUT predicate as one ordered sequenced output position. */
    default MultiblockPredicate sequentialOutput(int index) {
        return net.mads.industron.machine.machines.electric.multiblock.MultiblockPredicates.sequentialOutput(this, index);
    }

    /** Returns the ordered sequenced-output index, or 0 when this predicate is not sequenced. */
    default int sequentialOutputIndex() {
        return 0;
    }

    /** Returns whether this predicate permits a build candidate of the supplied machine tier. */
    default boolean allowsBuildTier(MachineTier tier) {
        return true;
    }

    record DisplayOption(
            MultiblockVisualization.SymbolInfo info,
            int min,
            int max,
            MachineTier tier,
            boolean exactTier,
            int sequentialInput,
            int sequentialOutput
    ) {
        public static DisplayOption of(MultiblockVisualization.SymbolInfo info) {
            return new DisplayOption(info, -1, -1, null, false, 0, 0);
        }

        public boolean hasMinimum() {
            return min >= 0;
        }

        public boolean hasMaximum() {
            return max >= 0;
        }

        public boolean hasTierRestriction() {
            return tier != null;
        }

        public DisplayOption withCount(int minimum, int maximum) {
            int mergedMin = minimum >= 0
                    ? (min >= 0 ? Math.max(min, minimum) : minimum)
                    : min;
            int mergedMax = maximum >= 0
                    ? (max >= 0 ? Math.min(max, maximum) : maximum)
                    : max;
            return new DisplayOption(info, mergedMin, mergedMax, tier, exactTier, sequentialInput, sequentialOutput);
        }

        public DisplayOption withTier(MachineTier limit, boolean exact) {
            return new DisplayOption(info, min, max, limit, exact, sequentialInput, sequentialOutput);
        }

        public DisplayOption withSequentialInput(int index) {
            return new DisplayOption(info, min, max, tier, exactTier, index, sequentialOutput);
        }

        public DisplayOption withSequentialOutput(int index) {
            return new DisplayOption(info, min, max, tier, exactTier, sequentialInput, index);
        }
    }

    record CountRequirement(String key, ResourceLocation blockId, int min, int max) {
        public boolean hasMinimum() {
            return min > 0;
        }

        public boolean hasMaximum() {
            return max >= 0;
        }
    }

    record Match(
            boolean matches,
            MachineTier tier,
            Set<MultiblockAbility> abilities,
            Map<String, Integer> counts,
            ResourceLocation overlayModel,
            MultiblockModelSource modelSource
    ) {
        public static Match failed() {
            return new Match(false, null, Set.of(), Map.of(), null, null);
        }

        public static Match success() {
            return new Match(true, null, Set.of(), Map.of(), null, null);
        }

        public static Match tiered(MachineTier tier) {
            return new Match(true, tier, Set.of(), Map.of(), null, null);
        }

        public static Match abilities(Set<MultiblockAbility> abilities, MachineTier tier) {
            return new Match(true, tier, abilities, Map.of(), null, null);
        }

        public static Match counted(MachineTier tier, Set<MultiblockAbility> abilities, String key) {
            return new Match(true, tier, abilities, Map.of(key, 1), null, null);
        }

        public Match withOverlay(ResourceLocation overlayModel) {
            return matches ? new Match(true, tier, abilities, counts, overlayModel, modelSource) : this;
        }

        public Match withModelSource(MultiblockModelSource source) {
            return matches ? new Match(true, tier, abilities, counts, overlayModel, source) : this;
        }
    }
}
