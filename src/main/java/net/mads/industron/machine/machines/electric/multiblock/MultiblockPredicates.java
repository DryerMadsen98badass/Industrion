package net.mads.industron.machine.machines.electric.multiblock;

import net.mads.industron.Industron;
import net.mads.industron.machine.MachineCasingBlock;
import net.mads.industron.machine.MachinePortBlockEntity;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.MachineTierStats;
import net.mads.industron.machine.MaterialMachineCasingBlock;
import net.mads.industron.material.recipes.CasingDefinition;
import net.mads.industron.block.coils.CoilBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class MultiblockPredicates {
    public static final NeededAbilities needed = new NeededAbilities();
    private static final Set<MultiblockAbility> MACHINE_IO_ABILITIES = EnumSet.of(
            MultiblockAbility.ITEM_INPUT,
            MultiblockAbility.ITEM_OUTPUT,
            MultiblockAbility.FLUID_INPUT,
            MultiblockAbility.FLUID_OUTPUT
    );

    private MultiblockPredicates() {
    }

    public static MultiblockPredicate air() {
        return (level, pos, state) -> state.isAir() ? MultiblockPredicate.Match.success() : MultiblockPredicate.Match.failed();
    }

    public static MultiblockPredicate controller() {
        return ControllerPredicate.INSTANCE;
    }

    static boolean isControllerPredicate(MultiblockPredicate predicate) {
        return predicate == ControllerPredicate.INSTANCE;
    }

    public static PredicateBuilder block(String blockId) {
        return new PredicateBuilder(MultiblockRegistry.id(blockId));
    }

    /** Matches any block in a vanilla/modded block tag. */
    public static MultiblockPredicate tag(String tagId) {
        ResourceLocation id = MultiblockRegistry.id(tagId);
        return new TagPredicate(TagKey.create(Registries.BLOCK, id));
    }

    public static MultiblockPredicate or(MultiblockPredicate first, MultiblockPredicate second) {
        return (first instanceof RecipeTypeAwarePredicate || second instanceof RecipeTypeAwarePredicate)
                ? new RecipeAwareOrPredicate(first, second)
                : new OrPredicate(first, second);
    }

    public static MultiblockPredicate overlay(MultiblockPredicate predicate, String model) {
        return new OverlayPredicate(predicate, MultiblockRegistry.id(model));
    }

    public static MultiblockPredicate model(MultiblockPredicate predicate, MultiblockModelSource source) {
        return new ModelSourcePredicate(Objects.requireNonNull(predicate), Objects.requireNonNull(source));
    }

    /** Shorthand used by definitions such as {@code ability(...).model(casing())}. */
    public static MultiblockModelSource casing() {
        return MultiblockModelSource.CASING;
    }

    public static MultiblockPredicate min(MultiblockPredicate predicate, int minimum) {
        return new CountedPredicate(predicate, minimum, -1);
    }

    public static MultiblockPredicate max(MultiblockPredicate predicate, int maximum) {
        return new CountedPredicate(predicate, -1, maximum);
    }

    public static MultiblockPredicate tierUpTo(MultiblockPredicate predicate, MachineTier tier) {
        return new TierRestrictedPredicate(predicate, Objects.requireNonNull(tier), false);
    }

    public static MultiblockPredicate sequentialInput(MultiblockPredicate predicate, int index) {
        if (index < 1) {
            throw new IllegalArgumentException("Sequential input index must be at least 1");
        }
        return new SequentialInputPredicate(Objects.requireNonNull(predicate), index);
    }

    public static MultiblockPredicate sequentialOutput(MultiblockPredicate predicate, int index) {
        if (index < 1) {
            throw new IllegalArgumentException("Sequential output index must be at least 1");
        }
        return new SequentialOutputPredicate(Objects.requireNonNull(predicate), index);
    }

    public static PredicateBuilder where(String blockId) {
        return new PredicateBuilder(MultiblockRegistry.id(blockId));
    }

    public static MultiblockPredicate ability(MultiblockAbility ability) {
        return new AbilityPredicate(EnumSet.of(ability), false);
    }

    public static MultiblockPredicate ability(NeededAbilities needed) {
        return new NeededAbilityPredicate(Set.of());
    }

    public static MultiblockPredicate anyAbility(MultiblockAbility... abilities) {
        return new AbilityPredicate(EnumSet.copyOf(Arrays.asList(abilities)), true);
    }

    public static MultiblockPredicate tieredBlocks(TieredBlock... blocks) {
        return new TieredBlocksPredicate(List.of(blocks), false);
    }

    public static TieredBlock tiered(String blockId, MachineTier tier) {
        return new TieredBlock(MultiblockRegistry.id(blockId), tier);
    }

    public static MultiblockPredicate tieredMachineCasings() {
        List<TieredBlock> blocks = new ArrayList<>();
        for (MachineTier tier : MachineTier.ALL) {
            blocks.add(tiered(Industron.MOD_ID + ":" + tier.casingRegistryName(), tier));
        }
        return new TieredBlocksPredicate(blocks, true);
    }

    public static MultiblockPredicate anyMachineCasing() {
        return new AnyMachineCasingPredicate();
    }

    /** Matches generated material casings belonging to one casing definition. */
    public static MachineCasingPredicateBuilder machineCasing(String definitionId) {
        return new MachineCasingPredicateBuilder(definitionId);
    }

    /** Type-safe overload for an existing generated casing definition. */
    public static MachineCasingPredicateBuilder machineCasing(CasingDefinition definition) {
        return machineCasing(Objects.requireNonNull(definition, "machine casing definition").id());
    }

    public static MultiblockPredicate coils() {
        return new CoilPredicate();
    }

    public record TieredBlock(ResourceLocation blockId, MachineTier tier) {
        boolean matches(BlockState state) {
            Block block = BuiltInRegistries.BLOCK.get(blockId);
            return block == state.getBlock();
        }
    }

    private record TagPredicate(TagKey<Block> tag) implements MultiblockPredicate {
        @Override
        public Match match(Level level, BlockPos pos, BlockState state) {
            return state.is(tag) ? Match.success() : Match.failed();
        }
    }

    private enum ControllerPredicate implements MultiblockPredicate {
        INSTANCE;

        @Override
        public MultiblockPredicate.Match match(Level level, BlockPos pos, BlockState state) {
            return MultiblockPredicate.Match.failed();
        }
    }

    public static final class MachineCasingPredicateBuilder implements MultiblockPredicate {
        private final String definitionId;
        private MachineTier minimumTier;
        private MachineTier maximumTier;

        private MachineCasingPredicateBuilder(String definitionId) {
            if (definitionId == null || definitionId.isBlank()) {
                throw new IllegalArgumentException("Machine casing definition id cannot be blank");
            }
            this.definitionId = definitionId;
        }

        public MachineCasingPredicateBuilder minTier(MachineTier tier) {
            this.minimumTier = requireElectricTier(tier);
            validateRange();
            return this;
        }

        public MachineCasingPredicateBuilder maxTier(MachineTier tier) {
            this.maximumTier = requireElectricTier(tier);
            validateRange();
            return this;
        }

        public MachineCasingPredicateBuilder exactTier(MachineTier tier) {
            MachineTier exact = requireElectricTier(tier);
            this.minimumTier = exact;
            this.maximumTier = exact;
            return this;
        }

        @Override
        public Match match(Level level, BlockPos pos, BlockState state) {
            if (!(state.getBlock() instanceof MaterialMachineCasingBlock casing)) {
                return Match.failed();
            }
            if (!casing.definition().id().equals(definitionId) || !matchesTierRange(casing.tier())) {
                return Match.failed();
            }
            return Match.tiered(casing.tier());
        }

        @Override
        public boolean allowsBuildTier(MachineTier candidateTier) {
            return candidateTier != null && MachineTier.ALL.contains(candidateTier) && matchesTierRange(candidateTier);
        }

        @Override
        public MultiblockVisualization.SymbolInfo visualizationInfo() {
            return MultiblockVisualization.SymbolInfo.materialMachineCasings(definitionId);
        }

        private boolean matchesTierRange(MachineTier actual) {
            if (minimumTier != null && !MachineTierStats.isAtLeast(actual, minimumTier)) {
                return false;
            }
            return maximumTier == null || MachineTierStats.isAtMost(actual, maximumTier);
        }

        private void validateRange() {
            if (minimumTier != null && maximumTier != null
                    && MachineTierStats.tierIndex(minimumTier) > MachineTierStats.tierIndex(maximumTier)) {
                throw new IllegalArgumentException(
                        "Machine casing minimum tier " + minimumTier.id()
                                + " cannot be above maximum tier " + maximumTier.id()
                );
            }
        }

        private static MachineTier requireElectricTier(MachineTier tier) {
            Objects.requireNonNull(tier, "machine casing tier");
            if (!MachineTier.ALL.contains(tier)) {
                throw new IllegalArgumentException("Machine casing tier must be an electric MachineTier: " + tier.id());
            }
            return tier;
        }
    }

    public static final class NeededAbilities {
        private NeededAbilities() {
        }
    }

    public interface RecipeTypeAwarePredicate {
        MultiblockPredicate bindRecipeAbilities(Set<MultiblockAbility> abilities);

        boolean requiresRecipeAbilities();
    }

    public static final class PredicateBuilder implements MultiblockPredicate {
        private final ResourceLocation blockId;
        private final String countKey;
        private final Set<MultiblockAbility> requiredAbilities = EnumSet.noneOf(MultiblockAbility.class);
        private final Set<MultiblockAbility> anyAbilities = EnumSet.noneOf(MultiblockAbility.class);
        private int minimum = -1;
        private int maximum = -1;
        private MachineTier tier;

        private PredicateBuilder(ResourceLocation blockId) {
            this.blockId = blockId;
            this.countKey = "block:" + blockId + ":" + Integer.toHexString(System.identityHashCode(this));
        }

        public PredicateBuilder tier(MachineTier tier) {
            this.tier = tier;
            return this;
        }

        public PredicateBuilder ability(MultiblockAbility... abilities) {
            requiredAbilities.addAll(Arrays.asList(abilities));
            return this;
        }

        public PredicateBuilder anyAbility(MultiblockAbility... abilities) {
            anyAbilities.addAll(Arrays.asList(abilities));
            return this;
        }

        public PredicateBuilder min(int minimum) {
            if (minimum < 0) {
                throw new IllegalArgumentException("Minimum cannot be negative");
            }
            this.minimum = minimum;
            return this;
        }

        public PredicateBuilder max(int maximum) {
            if (maximum < 0) {
                throw new IllegalArgumentException("Maximum cannot be negative");
            }
            this.maximum = maximum;
            return this;
        }

        @Override
        public MultiblockPredicate.Match match(Level level, BlockPos pos, BlockState state) {
            if (BuiltInRegistries.BLOCK.get(blockId) != state.getBlock()) {
                return MultiblockPredicate.Match.failed();
            }

            if (requiredAbilities.isEmpty() && anyAbilities.isEmpty()) {
                return countedMatch(tier, Set.of());
            }

            if (!(level.getBlockEntity(pos) instanceof MultiblockPart part)) {
                return MultiblockPredicate.Match.failed();
            }

            if (!part.abilities().containsAll(requiredAbilities)) {
                return MultiblockPredicate.Match.failed();
            }

            if (!anyAbilities.isEmpty() && anyAbilities.stream().noneMatch(part::hasAbility)) {
                return MultiblockPredicate.Match.failed();
            }

            return countedMatch(lowestTier(tier, part.partTier()), part.abilities());
        }

        @Override
        public MultiblockVisualization.SymbolInfo visualizationInfo() {
            if (tier != null) {
                return MultiblockVisualization.SymbolInfo.tieredBlock(new TieredBlock(blockId, tier));
            }
            return MultiblockVisualization.SymbolInfo.block(blockId);
        }

        @Override
        public List<MultiblockPredicate.DisplayOption> displayOptions(MachineTier candidateTier) {
            if (!allowsBuildTier(candidateTier)) {
                return List.of();
            }
            MultiblockPredicate.DisplayOption option = MultiblockPredicate.DisplayOption.of(visualizationInfo())
                    .withCount(minimum, maximum);
            if (tier != null) {
                option = option.withTier(tier, true);
            }
            return List.of(option);
        }

        @Override
        public boolean allowsBuildTier(MachineTier candidateTier) {
            return tier == null || tier == candidateTier;
        }

        @Override
        public List<MultiblockPredicate.CountRequirement> countRequirements() {
            if (minimum < 0 && maximum < 0) {
                return List.of();
            }
            return List.of(new MultiblockPredicate.CountRequirement(countKey, blockId, Math.max(0, minimum), maximum));
        }

        private MultiblockPredicate.Match countedMatch(MachineTier tier, Set<MultiblockAbility> abilities) {
            return minimum >= 0 || maximum >= 0
                    ? MultiblockPredicate.Match.counted(tier, abilities, countKey)
                    : abilities.isEmpty()
                    ? (tier == null ? MultiblockPredicate.Match.success() : MultiblockPredicate.Match.tiered(tier))
                    : MultiblockPredicate.Match.abilities(abilities, tier);
        }
    }

    private record TierRestrictedPredicate(MultiblockPredicate delegate, MachineTier tier, boolean exact) implements MultiblockPredicate, RecipeTypeAwarePredicate {
        @Override
        public Match match(Level level, BlockPos pos, BlockState state) {
            Match match = delegate.match(level, pos, state);
            if (!match.matches()) {
                return Match.failed();
            }

            MachineTier actualTier = match.tier();
            if (actualTier == null && level.getBlockEntity(pos) instanceof MachinePortBlockEntity port) {
                actualTier = port.tier();
            }
            return actualTier != null && matchesTier(actualTier, tier, exact)
                    ? match
                    : Match.failed();
        }

        @Override
        public boolean allowsBuildTier(MachineTier candidateTier) {
            return delegate.allowsBuildTier(candidateTier) && matchesTier(candidateTier, tier, exact);
        }

        @Override
        public MultiblockVisualization.SymbolInfo visualizationInfo() {
            return delegate.visualizationInfo();
        }

        @Override
        public List<MultiblockPredicate.DisplayOption> displayOptions(MachineTier candidateTier) {
            if (!matchesTier(candidateTier, tier, exact)) {
                return List.of();
            }
            return delegate.displayOptions(candidateTier).stream()
                    .map(option -> option.withTier(tier, exact))
                    .toList();
        }

        @Override
        public List<MultiblockPredicate.CountRequirement> countRequirements() {
            return delegate.countRequirements();
        }

        @Override
        public int sequentialInputIndex() {
            return delegate.sequentialInputIndex();
        }

        @Override
        public int sequentialOutputIndex() {
            return delegate.sequentialOutputIndex();
        }

        @Override
        public MultiblockPredicate bindRecipeAbilities(Set<MultiblockAbility> abilities) {
            MultiblockPredicate bound = delegate instanceof RecipeTypeAwarePredicate recipeAware
                    ? recipeAware.bindRecipeAbilities(abilities)
                    : delegate;
            return bound == delegate ? this : new TierRestrictedPredicate(bound, tier, exact);
        }

        @Override
        public boolean requiresRecipeAbilities() {
            return delegate instanceof RecipeTypeAwarePredicate recipeAware && recipeAware.requiresRecipeAbilities();
        }
    }

    private record SequentialInputPredicate(MultiblockPredicate delegate, int index) implements MultiblockPredicate, RecipeTypeAwarePredicate {
        @Override
        public Match match(Level level, BlockPos pos, BlockState state) {
            Match match = delegate.match(level, pos, state);
            if (!match.matches() || !match.abilities().contains(MultiblockAbility.ITEM_INPUT)) {
                return Match.failed();
            }
            return match;
        }

        @Override
        public boolean allowsBuildTier(MachineTier tier) {
            return delegate.allowsBuildTier(tier);
        }

        @Override
        public MultiblockVisualization.SymbolInfo visualizationInfo() {
            return delegate.visualizationInfo();
        }

        @Override
        public List<MultiblockPredicate.DisplayOption> displayOptions(MachineTier tier) {
            return delegate.displayOptions(tier).stream()
                    .map(option -> option.withSequentialInput(index))
                    .toList();
        }

        @Override
        public List<MultiblockPredicate.CountRequirement> countRequirements() {
            return delegate.countRequirements();
        }

        @Override
        public int sequentialInputIndex() {
            return index;
        }

        @Override
        public MultiblockPredicate bindRecipeAbilities(Set<MultiblockAbility> abilities) {
            MultiblockPredicate bound = bindIfRecipeAware(delegate, abilities);
            return bound == delegate ? this : new SequentialInputPredicate(bound, index);
        }

        @Override
        public boolean requiresRecipeAbilities() {
            return delegate instanceof RecipeTypeAwarePredicate recipeAware && recipeAware.requiresRecipeAbilities();
        }

        private static MultiblockPredicate bindIfRecipeAware(MultiblockPredicate predicate, Set<MultiblockAbility> abilities) {
            return predicate instanceof RecipeTypeAwarePredicate recipeAware ? recipeAware.bindRecipeAbilities(abilities) : predicate;
        }
    }

    private record SequentialOutputPredicate(MultiblockPredicate delegate, int index) implements MultiblockPredicate, RecipeTypeAwarePredicate {
        @Override
        public Match match(Level level, BlockPos pos, BlockState state) {
            Match match = delegate.match(level, pos, state);
            if (!match.matches() || !match.abilities().contains(MultiblockAbility.FLUID_OUTPUT)) {
                return Match.failed();
            }
            return match;
        }

        @Override
        public boolean allowsBuildTier(MachineTier tier) {
            return delegate.allowsBuildTier(tier);
        }

        @Override
        public MultiblockVisualization.SymbolInfo visualizationInfo() {
            return delegate.visualizationInfo();
        }

        @Override
        public List<MultiblockPredicate.DisplayOption> displayOptions(MachineTier tier) {
            return delegate.displayOptions(tier).stream()
                    .map(option -> option.withSequentialOutput(index))
                    .toList();
        }

        @Override
        public List<MultiblockPredicate.CountRequirement> countRequirements() {
            return delegate.countRequirements();
        }

        @Override
        public int sequentialOutputIndex() {
            return index;
        }

        @Override
        public MultiblockPredicate bindRecipeAbilities(Set<MultiblockAbility> abilities) {
            MultiblockPredicate bound = bindIfRecipeAware(delegate, abilities);
            return bound == delegate ? this : new SequentialOutputPredicate(bound, index);
        }

        @Override
        public boolean requiresRecipeAbilities() {
            return delegate instanceof RecipeTypeAwarePredicate recipeAware && recipeAware.requiresRecipeAbilities();
        }

        private static MultiblockPredicate bindIfRecipeAware(MultiblockPredicate predicate, Set<MultiblockAbility> abilities) {
            return predicate instanceof RecipeTypeAwarePredicate recipeAware ? recipeAware.bindRecipeAbilities(abilities) : predicate;
        }
    }

    private static boolean matchesTier(MachineTier actual, MachineTier limit, boolean exact) {
        if (exact) {
            return actual == limit;
        }

        if (actual.family() != limit.family()) {
            return false;
        }

        if (limit == MachineTier.NONE) {
            return actual == MachineTier.NONE;
        }

        List<MachineTier> family = limit.isSteam()
                ? MachineTier.STEAM_SINGLEBLOCK_TIERS
                : MachineTier.ELECTRIC_TIERS;
        int actualIndex = family.indexOf(actual);
        int limitIndex = family.indexOf(limit);
        return actualIndex >= 0 && limitIndex >= 0 && actualIndex <= limitIndex;
    }

    private record AbilityPredicate(Set<MultiblockAbility> abilities, boolean any) implements MultiblockPredicate {
        @Override
        public Match match(Level level, BlockPos pos, BlockState state) {
            if (!(level.getBlockEntity(pos) instanceof MultiblockPart part)) {
                return Match.failed();
            }

            boolean matches = any
                    ? abilities.stream().anyMatch(part::hasAbility)
                    : part.abilities().containsAll(abilities);
            if (!matches) {
                return Match.failed();
            }

            MachineTier tier = abilities.size() == 1 && abilities.contains(MultiblockAbility.REDSTONE)
                    ? null
                    : part.partTier();
            return Match.abilities(part.abilities(), tier);
        }

        @Override
        public MultiblockVisualization.SymbolInfo visualizationInfo() {
            return any
                    ? MultiblockVisualization.SymbolInfo.anyAbility(abilities)
                    : MultiblockVisualization.SymbolInfo.requiredAbility(abilities);
        }
    }

    private record NeededAbilityPredicate(Set<MultiblockAbility> acceptedAbilities, Set<MultiblockAbility> requiredAbilities) implements MultiblockPredicate, RecipeTypeAwarePredicate {
        private NeededAbilityPredicate(Set<MultiblockAbility> requiredAbilities) {
            this(MACHINE_IO_ABILITIES, requiredAbilities);
        }

        @Override
        public MultiblockPredicate bindRecipeAbilities(Set<MultiblockAbility> abilities) {
            Set<MultiblockAbility> accepted = abilities.isEmpty() ? MACHINE_IO_ABILITIES : EnumSet.copyOf(abilities);
            return new NeededAbilityPredicate(accepted, accepted);
        }

        @Override
        public boolean requiresRecipeAbilities() {
            return true;
        }

        @Override
        public Match match(Level level, BlockPos pos, BlockState state) {
            if (!(level.getBlockEntity(pos) instanceof MultiblockPart part)) {
                return Match.failed();
            }

            boolean matches = acceptedAbilities.stream().anyMatch(part::hasAbility);
            return matches ? Match.abilities(part.abilities(), part.partTier()) : Match.failed();
        }

        @Override
        public MultiblockVisualization.SymbolInfo visualizationInfo() {
            return MultiblockVisualization.SymbolInfo.anyAbility(acceptedAbilities);
        }
    }

    private record TieredBlocksPredicate(List<TieredBlock> blocks, boolean machineCasings) implements MultiblockPredicate {
        @Override
        public Match match(Level level, BlockPos pos, BlockState state) {
            for (TieredBlock entry : blocks) {
                if (entry.matches(state)) {
                    return Match.tiered(entry.tier());
                }
            }

            return Match.failed();
        }

        @Override
        public MultiblockVisualization.SymbolInfo visualizationInfo() {
            return machineCasings
                    ? MultiblockVisualization.SymbolInfo.machineCasings()
                    : MultiblockVisualization.SymbolInfo.tieredBlocks(blocks);
        }
    }

    private record OrPredicate(MultiblockPredicate left, MultiblockPredicate right) implements MultiblockPredicate {
        @Override
        public Match match(Level level, BlockPos pos, BlockState state) {
            Match leftMatch = left.match(level, pos, state);
            return leftMatch.matches() ? leftMatch : right.match(level, pos, state);
        }

        @Override
        public boolean allowsBuildTier(MachineTier tier) {
            return left.allowsBuildTier(tier) || right.allowsBuildTier(tier);
        }

        @Override
        public MultiblockVisualization.SymbolInfo visualizationInfo() {
            return mergeInfo(left.visualizationInfo(), right.visualizationInfo());
        }

        @Override
        public List<MultiblockPredicate.DisplayOption> displayOptions(MachineTier tier) {
            List<MultiblockPredicate.DisplayOption> options = new ArrayList<>(left.displayOptions(tier));
            options.addAll(right.displayOptions(tier));
            return List.copyOf(options);
        }

        @Override
        public List<MultiblockPredicate.CountRequirement> countRequirements() {
            return mergeRequirements(left, right);
        }

        @Override
        public int sequentialInputIndex() {
            return mergeSequentialInputIndex(left, right);
        }

        @Override
        public int sequentialOutputIndex() {
            return mergeSequentialOutputIndex(left, right);
        }
    }

    private static final class RecipeAwareOrPredicate implements MultiblockPredicate, RecipeTypeAwarePredicate {
        private final MultiblockPredicate left;
        private final MultiblockPredicate right;

        private RecipeAwareOrPredicate(MultiblockPredicate left, MultiblockPredicate right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public Match match(Level level, BlockPos pos, BlockState state) {
            Match leftMatch = left.match(level, pos, state);
            return leftMatch.matches() ? leftMatch : right.match(level, pos, state);
        }

        @Override
        public boolean allowsBuildTier(MachineTier tier) {
            return left.allowsBuildTier(tier) || right.allowsBuildTier(tier);
        }

        @Override
        public MultiblockVisualization.SymbolInfo visualizationInfo() {
            return mergeInfo(left.visualizationInfo(), right.visualizationInfo());
        }

        @Override
        public List<MultiblockPredicate.DisplayOption> displayOptions(MachineTier tier) {
            List<MultiblockPredicate.DisplayOption> options = new ArrayList<>(left.displayOptions(tier));
            options.addAll(right.displayOptions(tier));
            return List.copyOf(options);
        }

        @Override
        public MultiblockPredicate bindRecipeAbilities(Set<MultiblockAbility> abilities) {
            MultiblockPredicate boundLeft = bindIfRecipeAware(left, abilities);
            MultiblockPredicate boundRight = bindIfRecipeAware(right, abilities);
            return boundLeft == left && boundRight == right ? this : new RecipeAwareOrPredicate(boundLeft, boundRight);
        }

        @Override
        public boolean requiresRecipeAbilities() {
            return false;
        }

        private static MultiblockPredicate bindIfRecipeAware(MultiblockPredicate predicate, Set<MultiblockAbility> abilities) {
            return predicate instanceof RecipeTypeAwarePredicate recipeAware ? recipeAware.bindRecipeAbilities(abilities) : predicate;
        }

        private static boolean requiresRecipeAbilities(MultiblockPredicate predicate) {
            return predicate instanceof RecipeTypeAwarePredicate recipeAware && recipeAware.requiresRecipeAbilities();
        }

        @Override
        public List<MultiblockPredicate.CountRequirement> countRequirements() {
            return mergeRequirements(left, right);
        }

        @Override
        public int sequentialInputIndex() {
            return mergeSequentialInputIndex(left, right);
        }

        @Override
        public int sequentialOutputIndex() {
            return mergeSequentialOutputIndex(left, right);
        }
    }

    private static final class AnyMachineCasingPredicate implements MultiblockPredicate {
        @Override
        public Match match(Level level, BlockPos pos, BlockState state) {
            return state.getBlock() instanceof MachineCasingBlock casing
                    ? Match.tiered(casing.tier())
                    : Match.failed();
        }

        @Override
        public MultiblockVisualization.SymbolInfo visualizationInfo() {
            return MultiblockVisualization.SymbolInfo.machineCasings();
        }
    }

    private static final class CoilPredicate implements MultiblockPredicate {
        @Override
        public Match match(Level level, BlockPos pos, BlockState state) {
            return state.getBlock() instanceof CoilBlock
                    ? Match.success()
                    : Match.failed();
        }

        @Override
        public MultiblockVisualization.SymbolInfo visualizationInfo() {
            return MultiblockVisualization.SymbolInfo.coils();
        }
    }

    private record ModelSourcePredicate(
            MultiblockPredicate predicate,
            MultiblockModelSource source
    ) implements MultiblockPredicate, RecipeTypeAwarePredicate {
        @Override
        public Match match(Level level, BlockPos pos, BlockState state) {
            return predicate.match(level, pos, state).withModelSource(source);
        }

        @Override
        public boolean allowsBuildTier(MachineTier tier) {
            return predicate.allowsBuildTier(tier);
        }

        @Override
        public MultiblockVisualization.SymbolInfo visualizationInfo() {
            return predicate.visualizationInfo();
        }

        @Override
        public List<MultiblockPredicate.DisplayOption> displayOptions(MachineTier tier) {
            return predicate.displayOptions(tier);
        }

        @Override
        public List<MultiblockPredicate.CountRequirement> countRequirements() {
            return predicate.countRequirements();
        }

        @Override
        public int sequentialInputIndex() {
            return predicate.sequentialInputIndex();
        }

        @Override
        public int sequentialOutputIndex() {
            return predicate.sequentialOutputIndex();
        }

        @Override
        public MultiblockPredicate bindRecipeAbilities(Set<MultiblockAbility> abilities) {
            MultiblockPredicate bound = predicate instanceof RecipeTypeAwarePredicate recipeAware
                    ? recipeAware.bindRecipeAbilities(abilities)
                    : predicate;
            return bound == predicate ? this : new ModelSourcePredicate(bound, source);
        }

        @Override
        public boolean requiresRecipeAbilities() {
            return predicate instanceof RecipeTypeAwarePredicate recipeAware && recipeAware.requiresRecipeAbilities();
        }
    }

    private record OverlayPredicate(MultiblockPredicate predicate, ResourceLocation model) implements MultiblockPredicate, RecipeTypeAwarePredicate {
        @Override
        public Match match(Level level, BlockPos pos, BlockState state) {
            return predicate.match(level, pos, state).withOverlay(model);
        }

        @Override
        public boolean allowsBuildTier(MachineTier tier) {
            return predicate.allowsBuildTier(tier);
        }

        @Override
        public MultiblockVisualization.SymbolInfo visualizationInfo() {
            return predicate.visualizationInfo();
        }

        @Override
        public List<MultiblockPredicate.DisplayOption> displayOptions(MachineTier tier) {
            return predicate.displayOptions(tier);
        }

        @Override
        public List<MultiblockPredicate.CountRequirement> countRequirements() {
            return predicate.countRequirements();
        }

        @Override
        public int sequentialInputIndex() {
            return predicate.sequentialInputIndex();
        }

        @Override
        public int sequentialOutputIndex() {
            return predicate.sequentialOutputIndex();
        }

        @Override
        public MultiblockPredicate bindRecipeAbilities(Set<MultiblockAbility> abilities) {
            MultiblockPredicate bound = bindIfRecipeAware(predicate, abilities);
            return bound == predicate ? this : new OverlayPredicate(bound, model);
        }

        @Override
        public boolean requiresRecipeAbilities() {
            return predicate instanceof RecipeTypeAwarePredicate recipeAware && recipeAware.requiresRecipeAbilities();
        }

        private static MultiblockPredicate bindIfRecipeAware(MultiblockPredicate predicate, Set<MultiblockAbility> abilities) {
            return predicate instanceof RecipeTypeAwarePredicate recipeAware ? recipeAware.bindRecipeAbilities(abilities) : predicate;
        }
    }

    private record CountedPredicate(MultiblockPredicate predicate, int minimum, int maximum) implements MultiblockPredicate, RecipeTypeAwarePredicate {
        CountedPredicate {
            if (minimum < -1) {
                throw new IllegalArgumentException("Minimum cannot be less than -1");
            }
            if (maximum < -1) {
                throw new IllegalArgumentException("Maximum cannot be less than -1");
            }
        }

        @Override
        public Match match(Level level, BlockPos pos, BlockState state) {
            Match match = predicate.match(level, pos, state);
            if (!match.matches()) {
                return match;
            }
            return new Match(
                    true,
                    match.tier(),
                    match.abilities(),
                    mergeCount(match.counts(), countKey()),
                    match.overlayModel(),
                    match.modelSource()
            );
        }

        @Override
        public boolean allowsBuildTier(MachineTier tier) {
            return predicate.allowsBuildTier(tier);
        }

        @Override
        public MultiblockVisualization.SymbolInfo visualizationInfo() {
            return predicate.visualizationInfo();
        }

        @Override
        public List<MultiblockPredicate.DisplayOption> displayOptions(MachineTier tier) {
            return predicate.displayOptions(tier).stream()
                    .map(option -> option.withCount(minimum, maximum))
                    .toList();
        }

        @Override
        public List<MultiblockPredicate.CountRequirement> countRequirements() {
            List<MultiblockPredicate.CountRequirement> requirements = new ArrayList<>(predicate.countRequirements());
            requirements.add(new MultiblockPredicate.CountRequirement(countKey(), null, Math.max(0, minimum), maximum));
            return requirements;
        }

        @Override
        public int sequentialInputIndex() {
            return predicate.sequentialInputIndex();
        }

        @Override
        public int sequentialOutputIndex() {
            return predicate.sequentialOutputIndex();
        }

        @Override
        public MultiblockPredicate bindRecipeAbilities(Set<MultiblockAbility> abilities) {
            MultiblockPredicate bound = bindIfRecipeAware(predicate, abilities);
            return bound == predicate ? this : new CountedPredicate(bound, minimum, maximum);
        }

        @Override
        public boolean requiresRecipeAbilities() {
            return predicate instanceof RecipeTypeAwarePredicate recipeAware && recipeAware.requiresRecipeAbilities();
        }

        private String countKey() {
            return "predicate:" + Integer.toHexString(System.identityHashCode(this));
        }

        private static MultiblockPredicate bindIfRecipeAware(MultiblockPredicate predicate, Set<MultiblockAbility> abilities) {
            return predicate instanceof RecipeTypeAwarePredicate recipeAware ? recipeAware.bindRecipeAbilities(abilities) : predicate;
        }

        private static java.util.Map<String, Integer> mergeCount(java.util.Map<String, Integer> counts, String key) {
            java.util.Map<String, Integer> merged = new java.util.HashMap<>(counts);
            merged.merge(key, 1, Integer::sum);
            return java.util.Map.copyOf(merged);
        }
    }

    public static MachineTier lowestTier(MachineTier current, MachineTier next) {
        if (current == null) {
            return next;
        }
        if (next == null) {
            return current;
        }
        return MachineTierStats.tierIndex(next) < MachineTierStats.tierIndex(current) ? next : current;
    }

    private static MultiblockVisualization.SymbolInfo mergeInfo(MultiblockVisualization.SymbolInfo left, MultiblockVisualization.SymbolInfo right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.merge(right);
    }

    private static int mergeSequentialInputIndex(MultiblockPredicate left, MultiblockPredicate right) {
        int leftIndex = left.sequentialInputIndex();
        int rightIndex = right.sequentialInputIndex();
        if (leftIndex > 0 && rightIndex > 0 && leftIndex != rightIndex) {
            throw new IllegalStateException("Combined multiblock predicate cannot use two different sequential input indexes");
        }
        return leftIndex > 0 ? leftIndex : rightIndex;
    }

    private static int mergeSequentialOutputIndex(MultiblockPredicate left, MultiblockPredicate right) {
        int leftIndex = left.sequentialOutputIndex();
        int rightIndex = right.sequentialOutputIndex();
        if (leftIndex > 0 && rightIndex > 0 && leftIndex != rightIndex) {
            throw new IllegalStateException("Combined multiblock predicate cannot use two different sequential output indexes");
        }
        return leftIndex > 0 ? leftIndex : rightIndex;
    }

    private static List<MultiblockPredicate.CountRequirement> mergeRequirements(MultiblockPredicate left, MultiblockPredicate right) {
        List<MultiblockPredicate.CountRequirement> requirements = new ArrayList<>();
        requirements.addAll(left.countRequirements());
        requirements.addAll(right.countRequirements());
        return requirements.stream().filter(Objects::nonNull).toList();
    }
}
