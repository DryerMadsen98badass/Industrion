package net.mads.industron.machine;

import net.mads.industron.block.BlockStrength;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.mads.industron.gui.ProgressBar;
import net.mads.industron.machine.interaction.BlockInteraction;
import net.mads.industron.machine.interaction.MachineCondition;
import net.mads.industron.machine.interaction.MachineModifier;
import net.mads.industron.machine.interaction.MachineArea;
import net.mads.industron.recipe.CERecipe;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.recipe.RecipeTypeDefinition;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Declarative definition of a machine that occupies one block.
 */
public final class SingleBlockDefinition {
    private final String id;
    private final String displayName;
    private final List<String> tooltips;
    private final SingleBlockMachinePower power;
    private final SingleBlockMachineResource resource;
    private final SingleBlockMachineResourceMode resourceMode;
    private final MachineTier startTier;
    private final List<MachineTier> generatedTiers;
    private final List<ResourceLocation> recipeTypes;
    private final Slots slots;
    private final int steamCapacity;
    private final int steamUsage;
    private final ResourceLocation steamConversionRecipe;
    private final int energyUsage;
    private final boolean noDurationReset;
    private final int maxParallel;
    private final ProcessingProfile processingProfile;
    private final boolean waterloggable;

    @Nullable
    private final MachineSide kineticInput;

    @Nullable
    private final MachineSide kineticOutput;

    private final double startSu;
    private final Optional<Integer> minRpm;
    private final Optional<Integer> maxRpm;
    private final Optional<Integer> outputRpm;
    private final List<StackRequirement> inputItems;
    private final List<StackRequirement> inputFluids;
    private final List<StackRequirement> outputItems;
    private final List<StackRequirement> outputFluids;
    private final List<BlockInteraction> blockInteractions;
    private final List<MachineCondition> conditions;
    private final List<MachineModifier> modifiers;
    private final List<MachineArea> areas;
    private final Map<MachineSide, String> sideTextures;
    private final Map<MachineSide, Integer> sideTextureColors;
    private final Map<MachineSide, List<String>> sideOverlays;

    @Nullable
    private final String model;
    @Nullable
    private final StoneTextureSource stoneTextureSource;
    private final MachineTier breakingTier;
    @Nullable
    private final BlockStrength strength;
    private final Set<ToolDefinition> miningTools;
    private final EnumSet<MachineSide> noItemInputSides;
    private final EnumSet<MachineSide> noItemOutputSides;
    private final EnumSet<MachineSide> noFluidInputSides;
    private final EnumSet<MachineSide> noFluidOutputSides;

    @Nullable
    private final ProgressBar progressBar;

    @Nullable
    private final TemperatureSettings temperature;

    private SingleBlockDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        List<String> tooltipLines = new ArrayList<>(builder.tooltips);
        if (builder.maxParallel > 1) tooltipLines.add("Max Parallels: " + builder.maxParallel);
        this.tooltips = List.copyOf(tooltipLines);
        this.power = builder.power;
        this.resource = builder.resource;
        this.resourceMode = builder.resourceMode;
        TierSelection tierSelection = resolveTierSelection(builder);
        this.startTier = tierSelection.startTier();
        this.generatedTiers = tierSelection.tiers();
        this.recipeTypes = List.copyOf(builder.recipeTypes);
        this.slots = builder.slots;
        this.steamCapacity = builder.steamCapacity;
        this.steamUsage = builder.steamUsage;
        this.steamConversionRecipe = builder.steamConversionRecipe;
        this.energyUsage = builder.energyUsage;
        this.noDurationReset = builder.noDurationReset;
        this.processingProfile = builder.processingProfile;
        this.maxParallel = Math.max(1, builder.maxParallel);
        this.waterloggable = builder.waterloggable;
        this.kineticInput = builder.kineticInput;
        this.kineticOutput = builder.kineticOutput;
        this.startSu = builder.startSu;
        this.minRpm = builder.minRpm;
        this.maxRpm = builder.maxRpm;
        this.outputRpm = builder.outputRpm;
        this.inputItems = List.copyOf(builder.inputItems);
        this.inputFluids = List.copyOf(builder.inputFluids);
        this.outputItems = List.copyOf(builder.outputItems);
        this.outputFluids = List.copyOf(builder.outputFluids);
        this.blockInteractions = List.copyOf(builder.blockInteractions);
        this.conditions = List.copyOf(builder.conditions);
        this.modifiers = List.copyOf(builder.modifiers);
        this.areas = List.copyOf(builder.areas);
        this.sideTextures = Collections.unmodifiableMap(
                new EnumMap<>(builder.sideTextures)
        );
        this.sideTextureColors = Collections.unmodifiableMap(
                new EnumMap<>(builder.sideTextureColors)
        );
        this.model = builder.model;
        this.stoneTextureSource = builder.stoneTextureSource;

        EnumMap<MachineSide, List<String>> overlays =
                new EnumMap<>(MachineSide.class);

        builder.sideOverlays.forEach(
                (side, frames) -> overlays.put(side, List.copyOf(frames))
        );

        this.sideOverlays = Collections.unmodifiableMap(overlays);
        this.breakingTier = builder.breakingTier.recipeTier();
        this.strength = builder.strength;
        this.miningTools = Set.copyOf(builder.miningTools);
        this.noItemInputSides = builder.noItemInputSides.clone();
        this.noItemOutputSides = builder.noItemOutputSides.clone();
        this.noFluidInputSides = builder.noFluidInputSides.clone();
        this.noFluidOutputSides = builder.noFluidOutputSides.clone();
        this.progressBar = builder.progressBar;
        this.temperature = builder.temperature;

        validate();
    }

    public static Builder machine() {
        return new Builder();
    }

    public List<SingleBlockMachineInstance> expand() {
        return generatedTiers.stream()
                .map(tier -> new SingleBlockMachineInstance(this, tier))
                .toList();
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public List<String> tooltips() { return tooltips; }
    public int maxParallel() { return maxParallel; }
    public boolean waterloggable() { return waterloggable; }

    public SingleBlockMachinePower power() {
        return power;
    }

    public SingleBlockMachineResource resource() {
        return resource;
    }

    public SingleBlockMachineResourceMode resourceMode() {
        return resourceMode;
    }

    public MachineTier startTier() {
        return startTier;
    }

    public List<MachineTier> generatedTiers() {
        return generatedTiers;
    }

    public List<ResourceLocation> recipeTypes() {
        return recipeTypes;
    }

    public boolean matchesRecipeType(ResourceLocation recipeType) {
        return recipeTypes.contains(recipeType);
    }

    public Slots slots() {
        return slots;
    }

    public boolean allowsItemInput(MachineSide side) { return !noItemInputSides.contains(side); }
    public boolean allowsItemOutput(MachineSide side) { return !noItemOutputSides.contains(side); }
    public boolean allowsFluidInput(MachineSide side) { return !noFluidInputSides.contains(side); }
    public boolean allowsFluidOutput(MachineSide side) { return !noFluidOutputSides.contains(side); }

    public ResourceLocation steamConversionRecipe() { return steamConversionRecipe; }

    public int steamCapacity() {
        return steamCapacity;
    }

    public int steamUsage() {
        return steamUsage;
    }

    public int energyUsage() {
        return energyUsage;
    }

    /**
     * When true, a running recipe keeps its current progress while this machine
     * is temporarily missing its required energy or steam.
     */
    public ProcessingProfile processingProfile(MachineTier tier) {
        return processingProfile != null ? processingProfile : tier.isSteam() ? ProcessingProfile.STEAM : ProcessingProfile.STANDARD;
    }

    public boolean noDurationReset() {
        return noDurationReset;
    }

    /**
     * Machine-relative side containing the Create shaft connection.
     * Horizontal sides rotate together with the machine facing.
     */
    @Nullable
    public MachineSide kineticInput() {
        return kineticInput;
    }

    public boolean usesKineticInput() {
        return kineticInput != null;
    }

    /** Machine-relative side containing the generated shaft output. */
    @Nullable
    public MachineSide kineticOutput() {
        return kineticOutput;
    }

    public boolean usesKineticOutput() {
        return kineticOutput != null;
    }

    public boolean usesKinetic() {
        return usesKineticInput() || usesKineticOutput();
    }

    @Nullable
    public MachineSide kineticSide() {
        return kineticInput != null ? kineticInput : kineticOutput;
    }

    public double startSu() {
        return startSu;
    }

    /** Backwards-compatible name for older input-only definitions. */
    public double kineticStressImpact() {
        return startSu;
    }

    public Optional<Integer> minRpm() {
        return minRpm;
    }

    public Optional<Integer> maxRpm() {
        return maxRpm;
    }

    public Optional<Integer> outputRpm() {
        return outputRpm;
    }

    public List<StackRequirement> inputItems() {
        return inputItems;
    }

    public List<StackRequirement> inputFluids() {
        return inputFluids;
    }

    public List<StackRequirement> outputItems() {
        return outputItems;
    }

    public List<StackRequirement> outputFluids() {
        return outputFluids;
    }

    public List<BlockInteraction> blockInteractions() {
        return blockInteractions;
    }

    public List<MachineCondition> conditions() {
        return conditions;
    }

    public List<MachineModifier> modifiers() {
        return modifiers;
    }

    /** Named controller-relative areas available to conditions and block interactions. */
    public List<MachineArea> areas() {
        return areas;
    }

    public Map<MachineSide, String> sideTextures() {
        return sideTextures;
    }

    @Nullable
    public String sideTexture(MachineSide side) {
        return sideTextures.get(side);
    }

    @Nullable
    public Integer sideTextureColor(MachineSide side) {
        return sideTextureColors.get(side);
    }

    public boolean hasSideTextureColor(MachineSide side) {
        return sideTextureColors.containsKey(side);
    }

    @Nullable
    public String model() {
        return model;
    }

    public Optional<StoneTextureSource> stoneTextureSource() {
        return Optional.ofNullable(stoneTextureSource);
    }

    public Map<MachineSide, List<String>> sideOverlays() {
        return sideOverlays;
    }

    public List<String> overlayFrames(MachineSide side) {
        return sideOverlays.getOrDefault(side, List.of());
    }

    public boolean hasOverlay(MachineSide side) {
        return !overlayFrames(side).isEmpty();
    }

    @Nullable
    public String idleOverlay(MachineSide side) {
        List<String> frames = overlayFrames(side);
        return frames.isEmpty() ? null : frames.getFirst();
    }

    public List<String> activeOverlays(MachineSide side) {
        List<String> frames = overlayFrames(side);

        return frames.size() <= 1
                ? List.of()
                : frames.subList(1, frames.size());
    }

    /**
     * Backwards-compatible access to the front overlay frames.
     */
    public List<String> overlayFrames() {
        return overlayFrames(MachineSide.FRONT);
    }

    /**
     * Backwards-compatible access to the front idle overlay.
     * Returns null when the machine does not define a front overlay.
     */
    @Nullable
    public String idleOverlay() {
        return idleOverlay(MachineSide.FRONT);
    }

    /**
     * Backwards-compatible access to the front active overlays.
     */
    public List<String> activeOverlays() {
        return activeOverlays(MachineSide.FRONT);
    }


    public MachineTier breakingTier() {
        return breakingTier;
    }

    public Set<ToolDefinition> miningTools() {
        return Collections.unmodifiableSet(miningTools);
    }

    @Nullable
    public ProgressBar progressBar() {
        return progressBar;
    }

    @Nullable
    public TemperatureSettings temperature() {
        return temperature;
    }

    public boolean hasTemperature() {
        return temperature != null;
    }

    public ProgressBar resolvedProgressBar() {
        if (progressBar != null) {
            return progressBar;
        }

        return recipeTypes.stream()
                .map(CERecipeTypes::byId)
                .filter(Objects::nonNull)
                .map(RecipeTypeDefinition::progressBar)
                .findFirst()
                .orElse(ProgressBar.ARROW);
    }

    public float hardness() {
        return strength != null ? strength.hardness() : MachineTierStats.blockHardness(breakingTier);
    }

    public float resistance() {
        return strength != null ? strength.resistance() : MachineTierStats.blockResistance(breakingTier);
    }

    public boolean hasExplicitStrength() {
        return strength != null;
    }

    private static TierSelection resolveTierSelection(Builder builder) {
        if (builder.tierDefined && !builder.onlyTiers.isEmpty()) {
            throw new IllegalArgumentException(
                    "Singleblock machine cannot combine tier(...) and onlyTier(...): "
                            + builder.id
            );
        }

        if (!builder.onlyTiers.isEmpty()) {
            List<MachineTier> tiers = builder.onlyTiers.stream()
                    .distinct()
                    .sorted(java.util.Comparator.comparingInt(SingleBlockDefinition::tierOrder))
                    .toList();
            validateOnlyTierFamily(builder.id, tiers);
            return new TierSelection(tiers.getFirst(), tiers);
        }

        MachineTier selectedTier = builder.startTier;
        if (builder.power == SingleBlockMachinePower.KINETIC
                && !builder.tierDefined
                && selectedTier == MachineTier.NONE) {
            selectedTier = MachineTier.ULV;
        }

        return new TierSelection(
                selectedTier,
                List.copyOf(MachineTier.expandSingleBlockTiers(selectedTier))
        );
    }

    private static void validateOnlyTierFamily(String id, List<MachineTier> tiers) {
        if (tiers.isEmpty()) {
            throw new IllegalArgumentException(
                    "onlyTier(...) must contain at least one tier: " + id
            );
        }

        MachineTier.Family family = tiers.getFirst().family();
        if (tiers.stream().anyMatch(tier -> tier.family() != family)) {
            throw new IllegalArgumentException(
                    "onlyTier(...) cannot mix tier families: " + id
            );
        }
    }

    private static int tierOrder(MachineTier tier) {
        if (tier == MachineTier.NONE) {
            return -1;
        }
        int electric = MachineTier.ELECTRIC_TIERS.indexOf(tier);
        if (electric >= 0) {
            return electric;
        }
        int steam = MachineTier.STEAM_SINGLEBLOCK_TIERS.indexOf(tier);
        return steam >= 0 ? 100 + steam : Integer.MAX_VALUE;
    }

    private void validate() {
        if (breakingTier == null || breakingTier == MachineTier.NONE) {
            throw new IllegalArgumentException("Singleblock breaking tier must be a real machine tier: " + id);
        }

        if (id == null || id.isBlank() || !ResourceLocation.isValidPath(id)) {
            throw new IllegalArgumentException(
                    "Invalid singleblock machine id: " + id
            );
        }

        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException(
                    "Singleblock machine display name cannot be blank: " + id
            );
        }

        if (power == null
                || resource == null
                || resourceMode == null
                || startTier == null) {
            throw new IllegalArgumentException(
                    "Singleblock machine has an incomplete resource definition: "
                            + id
            );
        }

        if (power == SingleBlockMachinePower.STEAM && !startTier.isSteam()) {
            throw new IllegalArgumentException(
                    "Steam machine must use a steam tier: " + id
            );
        }

        if (power == SingleBlockMachinePower.ELECTRIC
                && !startTier.isElectric()) {
            throw new IllegalArgumentException(
                    "Electric machine must use an electric tier: " + id
            );
        }

        if (power == SingleBlockMachinePower.KINETIC && !usesKinetic()) {
            throw new IllegalArgumentException(
                    "Kinetic machine must define a kinetic input or output side: " + id
            );
        }

        if (kineticInput != null && kineticOutput != null) {
            throw new IllegalArgumentException(
                    "A singleblock machine cannot use both kinetic input and output: " + id
            );
        }

        if (power != SingleBlockMachinePower.KINETIC && usesKinetic()) {
            throw new IllegalArgumentException(
                    "Only kinetic machines can define a kinetic input or output side: " + id
            );
        }

        if (power == SingleBlockMachinePower.KINETIC
                && generatedTiers.stream().anyMatch(tier -> !tier.isElectric())) {
            throw new IllegalArgumentException(
                    "Kinetic machine must use electric processing tiers: " + id
            );
        }

        if (!Double.isFinite(startSu) || startSu <= 0.0D) {
            throw new IllegalArgumentException(
                    "Kinetic start SU must be finite and positive: " + id
            );
        }

        validateRpm(minRpm, "minimum");
        validateRpm(maxRpm, "maximum");
        validateRpm(outputRpm, "output");

        if (minRpm.isPresent() && maxRpm.isPresent() && maxRpm.get() < minRpm.get()) {
            throw new IllegalArgumentException(
                    "Kinetic maximum RPM cannot be lower than minimum RPM: " + id
            );
        }

        if ((minRpm.isPresent() || maxRpm.isPresent()) && !usesKineticInput()) {
            throw new IllegalArgumentException(
                    "Only kinetic input machines can define minimum or maximum RPM: " + id
            );
        }

        if (outputRpm.isPresent() && !usesKineticOutput()) {
            throw new IllegalArgumentException(
                    "Only kinetic output machines can define output RPM: " + id
            );
        }

        /*
         * Overlays are optional.
         *
         * A machine without Option.overlay(...) simply uses its side textures
         * without an idle or active overlay.
         */

        for (Map.Entry<MachineSide, String> entry
                : sideTextures.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                throw new IllegalArgumentException(
                        "Blank texture for "
                                + entry.getKey()
                                + " on machine "
                                + id
                );
            }
        }

        if (model != null && model.isBlank()) {
            throw new IllegalArgumentException(
                    "Singleblock machine model cannot be blank: " + id
            );
        }

        for (Map.Entry<MachineSide, List<String>> entry
                : sideOverlays.entrySet()) {
            if (entry.getValue().isEmpty()) {
                throw new IllegalArgumentException(
                        "Empty overlay frame list for "
                                + entry.getKey()
                                + " on machine "
                                + id
                );
            }

            if (entry.getValue()
                    .stream()
                    .anyMatch(frame -> frame == null || frame.isBlank())) {
                throw new IllegalArgumentException(
                        "Blank overlay frame for "
                                + entry.getKey()
                                + " on machine "
                                + id
                );
            }
        }

        if (slots.itemInputs() < 0
                || slots.itemOutputs() < 0
                || slots.fluidInputs() < 0
                || slots.fluidOutputs() < 0) {
            throw new IllegalArgumentException(
                    "Singleblock machine cannot have negative slots: " + id
            );
        }

        if (steamConversionRecipe != null && (resource != SingleBlockMachineResource.STEAM
                || resourceMode != SingleBlockMachineResourceMode.PRODUCES || temperature == null)) {
            throw new IllegalArgumentException("Steam conversion requires a heated steam producer: " + id);
        }

        if (recipeTypes.stream().distinct().count() != recipeTypes.size()) {
            throw new IllegalArgumentException(
                    "Singleblock machine contains duplicate recipe types: "
                            + id
            );
        }

        for (ResourceLocation recipeTypeId : recipeTypes) {
            RecipeTypeDefinition recipeType =
                    CERecipeTypes.byId(recipeTypeId);

            if (recipeType == null) {
                throw new IllegalArgumentException(
                        "Unknown CE recipe type "
                                + recipeTypeId
                                + " on machine "
                                + id
                );
            }

            // RecipeType max IO describes the largest recipe/layout the type
            // permits. It is intentionally independent of the number of
            // physical inventory slots on a specific machine. A machine may
            // support the type while only being able to run the concrete
            // recipes that fit its own inventory/runtime model.
        }

        if (resource == SingleBlockMachineResource.STEAM
                && steamCapacity <= 0) {
            throw new IllegalArgumentException(
                    "Steam machine must have a positive steam capacity: " + id
            );
        }

        if (resource == SingleBlockMachineResource.STEAM
                && steamUsage <= 0) {
            throw new IllegalArgumentException(
                    "Steam machine must have a positive steam usage/output: "
                            + id
            );
        }

        if (resource == SingleBlockMachineResource.ENERGY
                && (energyUsage <= 0 || energyUsage >= 9)) {
            throw new IllegalArgumentException(
                    "Electric machine energy usage must be between 1 and 8: "
                            + id
            );
        }

        if (resource != SingleBlockMachineResource.ENERGY
                && energyUsage != 0) {
            throw new IllegalArgumentException(
                    "Only electric machines can define energy usage: " + id
            );
        }

        if (temperature != null) {
            if (temperature.minimumOperatingTemperature() < 0) {
                throw new IllegalArgumentException(
                        "Minimum temperature cannot be negative: " + id
                );
            }

            if (temperature.maximumTemperature()
                    < temperature.minimumOperatingTemperature()) {
                throw new IllegalArgumentException(
                        "Maximum temperature must be at least the minimum temperature: "
                                + id
                );
            }

            if (temperature.changeIntervalTicks() <= 0) {
                throw new IllegalArgumentException(
                        "Temperature interval must be positive: " + id
                );
            }

            if (temperature.heatingAmount() <= 0
                    || temperature.coolingAmount() <= 0) {
                throw new IllegalArgumentException(
                        "Temperature heating and cooling amounts must be positive: "
                                + id
                );
            }

            validateRequirements(
                    temperature.inputItems(),
                    "temperature input item"
            );

            validateRequirements(
                    temperature.inputFluids(),
                    "temperature input fluid"
            );

            validateRequirements(
                    temperature.outputItems(),
                    "temperature output item"
            );

            validateRequirements(
                    temperature.outputFluids(),
                    "temperature output fluid"
            );
        }
    }

    private void validateRpm(Optional<Integer> rpm, String name) {
        if (rpm.isPresent()
                && (rpm.get() < 1 || rpm.get() > CERecipe.DEFAULT_MAX_RPM)) {
            throw new IllegalArgumentException(
                    "Kinetic " + name + " RPM must be between 1 and "
                            + CERecipe.DEFAULT_MAX_RPM
                            + ": "
                            + id
            );
        }
    }

    private void validateRequirements(
            List<StackRequirement> requirements,
            String name
    ) {
        for (StackRequirement requirement : requirements) {
            if (requirement.id() == null || requirement.id().isBlank()) {
                throw new IllegalArgumentException(
                        "Blank " + name + " id on machine " + id
                );
            }

            ResourceLocation location =
                    ResourceLocation.tryParse(requirement.id());

            if (location == null) {
                throw new IllegalArgumentException(
                        "Invalid "
                                + name
                                + " id "
                                + requirement.id()
                                + " on machine "
                                + id
                );
            }

            if (requirement.amount() <= 0
                    || requirement.durationTicks() <= 0) {
                throw new IllegalArgumentException(
                        name
                                + " amount and duration must be positive on machine "
                                + id
                );
            }
        }
    }

    public enum MachineSide {
        FRONT,
        BACK,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM;

        public int tintIndex() {
            return ordinal();
        }

        @Nullable
        public static MachineSide fromTintIndex(int tintIndex) {
            MachineSide[] sides = values();
            return tintIndex >= 0 && tintIndex < sides.length ? sides[tintIndex] : null;
        }
    }

    private record TierSelection(
            MachineTier startTier,
            List<MachineTier> tiers
    ) {
        private TierSelection {
            tiers = List.copyOf(tiers);
        }
    }

    public record Slots(
            int itemInputs,
            int itemOutputs,
            int fluidInputs,
            int fluidOutputs
    ) {
    }

    public record StackRequirement(
            String id,
            int amount,
            int durationTicks
    ) {
    }

    public record TemperatureSettings(
            int minimumOperatingTemperature,
            int maximumTemperature,
            int changeIntervalTicks,
            int heatingAmount,
            int coolingAmount,
            List<StackRequirement> inputItems,
            List<StackRequirement> inputFluids,
            List<StackRequirement> outputItems,
            List<StackRequirement> outputFluids,
            List<MachineCondition> heatConditions
    ) {
        public TemperatureSettings {
            inputItems = List.copyOf(inputItems);
            inputFluids = List.copyOf(inputFluids);
            outputItems = List.copyOf(outputItems);
            outputFluids = List.copyOf(outputFluids);
            heatConditions = List.copyOf(heatConditions);
        }

        public boolean usesHeatConditions() {
            return !heatConditions.isEmpty();
        }
    }

    /**
     * Options shown by autocomplete inside
     * {@code .machineDefinition(Option...)} for singleblocks.
     */
    public record StoneTextureSource(
            StoneMaterial material,
            MaterialPart preferredPart,
            MaterialPart fallbackPart
    ) {
        public StoneTextureSource {
            Objects.requireNonNull(material, "Stone texture material");
            Objects.requireNonNull(preferredPart, "Preferred stone texture part");
            Objects.requireNonNull(fallbackPart, "Fallback stone texture part");
        }
    }

    @FunctionalInterface
    public interface Option {
        void apply(Builder builder);

        static Option id(String id) {
            return builder -> builder.id = id;
        }

        static Option displayName(String displayName) {
            return builder -> builder.displayName = displayName;
        }

        static Option tooltip(String tooltip) {
            return builder -> {
                if (tooltip == null || tooltip.isBlank()) {
                    throw new IllegalArgumentException(
                            "Machine tooltip cannot be blank"
                    );
                }

                builder.tooltips.add(tooltip);
            };
        }

        static Option parallel(int maxParallel) {
            return builder -> {
                if (maxParallel < 1) throw new IllegalArgumentException("Max parallels must be at least 1");
                builder.maxParallel = maxParallel;
            };
        }

        /** Registers this machine with a waterloggable singleblock implementation. */
        static Option waterloggable() {
            return builder -> builder.waterloggable = true;
        }

        static Option consumesSteam() {
            return builder -> {
                builder.power = SingleBlockMachinePower.STEAM;
                builder.resource = SingleBlockMachineResource.STEAM;
                builder.resourceMode =
                        SingleBlockMachineResourceMode.CONSUMES;
            };
        }

        static Option producesSteam() {
            return builder -> {
                builder.power = SingleBlockMachinePower.STEAM;
                builder.resource = SingleBlockMachineResource.STEAM;
                builder.resourceMode =
                        SingleBlockMachineResourceMode.PRODUCES;
            };
        }

        static Option consumesEnergy() {
            return builder -> {
                builder.power = SingleBlockMachinePower.ELECTRIC;
                builder.resource = SingleBlockMachineResource.ENERGY;
                builder.resourceMode =
                        SingleBlockMachineResourceMode.CONSUMES;
            };
        }

        /**
         * Adds one Create shaft input on a machine-relative side.
         */
        static Option kineticInput(MachineSide side) {
            return builder -> {
                builder.power = SingleBlockMachinePower.KINETIC;
                builder.resource = SingleBlockMachineResource.NONE;
                builder.resourceMode = SingleBlockMachineResourceMode.NONE;
                builder.kineticInput = Objects.requireNonNull(
                        side,
                        "Kinetic input side"
                );
            };
        }

        /**
         * Adds one generated Create shaft output on a machine-relative side.
         */
        static Option kineticOutput(MachineSide side) {
            return builder -> {
                builder.power = SingleBlockMachinePower.KINETIC;
                builder.resource = SingleBlockMachineResource.NONE;
                builder.resourceMode = SingleBlockMachineResourceMode.NONE;
                builder.kineticOutput = Objects.requireNonNull(
                        side,
                        "Kinetic output side"
                );
            };
        }

        /**
         * Defines SU/RPM for the first generated tier. Every real tier step
         * above it multiplies this value by four.
         */
        static Option startSu(double startSu) {
            return builder -> builder.startSu = startSu;
        }

        /** Backwards-compatible name for older kinetic input definitions. */
        static Option kineticStressImpact(double stressImpact) {
            return startSu(stressImpact);
        }

        static Option minRpm(int minRpm) {
            return builder -> builder.minRpm = Optional.of(minRpm);
        }

        static Option maxRpm(int maxRpm) {
            return builder -> builder.maxRpm = Optional.of(maxRpm);
        }

        static Option outputRpm(int outputRpm) {
            return builder -> builder.outputRpm = Optional.of(outputRpm);
        }

        static Option producesEnergy() {
            return builder -> {
                builder.power = SingleBlockMachinePower.ELECTRIC;
                builder.resource = SingleBlockMachineResource.ENERGY;
                builder.resourceMode =
                        SingleBlockMachineResourceMode.PRODUCES;
            };
        }

        static Option resource(
                SingleBlockMachinePower power,
                SingleBlockMachineResource resource,
                SingleBlockMachineResourceMode mode
        ) {
            return builder -> {
                builder.power = Objects.requireNonNull(power);
                builder.resource = Objects.requireNonNull(resource);
                builder.resourceMode = Objects.requireNonNull(mode);
            };
        }

        /**
         * Generates the selected tier and every higher tier in the same family.
         */
        static Option tier(MachineTier tier) {
            return builder -> {
                builder.startTier = Objects.requireNonNull(tier);
                builder.tierDefined = true;
            };
        }

        /**
         * Generates only the explicitly listed tiers. This cannot be combined
         * with {@link #tier(MachineTier)}.
         */
        static Option onlyTier(MachineTier... tiers) {
            return builder -> {
                Objects.requireNonNull(tiers, "Singleblock machine tiers");
                if (tiers.length == 0) {
                    throw new IllegalArgumentException(
                            "onlyTier(...) must contain at least one tier"
                    );
                }

                for (MachineTier tier : tiers) {
                    builder.onlyTiers.add(
                            Objects.requireNonNull(tier, "Singleblock machine tier")
                    );
                }
            };
        }

        static Option recipeType(RecipeTypeDefinition recipeType) {
            return builder ->
                    builder.recipeTypes.add(
                            Objects.requireNonNull(recipeType).id()
                    );
        }

        static Option slots(
                int itemInputs,
                int itemOutputs,
                int fluidInputs,
                int fluidOutputs
        ) {
            return builder ->
                    builder.slots = new Slots(
                            itemInputs,
                            itemOutputs,
                            fluidInputs,
                            fluidOutputs
                    );
        }

        static Option noItemInput(MachineSide... sides) { return builder -> addSides(builder.noItemInputSides, sides); }
        static Option noItemOutput(MachineSide... sides) { return builder -> addSides(builder.noItemOutputSides, sides); }
        static Option noFluidInput(MachineSide... sides) { return builder -> addSides(builder.noFluidInputSides, sides); }
        static Option noFluidOutput(MachineSide... sides) { return builder -> addSides(builder.noFluidOutputSides, sides); }

        private static void addSides(EnumSet<MachineSide> target, MachineSide... sides) {
            Objects.requireNonNull(sides, "Machine sides");
            for (MachineSide side : sides) target.add(Objects.requireNonNull(side, "Machine side"));
        }

        /** Binds a boiler to a separately registered CE evaporation recipe. */
        static Option steamConversionRecipe(String recipeId) {
            return builder -> builder.steamConversionRecipe = ResourceLocation.parse(recipeId);
        }

        static Option steamCapacity(int steamCapacity) {
            return builder -> builder.steamCapacity = steamCapacity;
        }

        static Option steamUsage(int steamUsage) {
            return builder -> builder.steamUsage = steamUsage;
        }

        static Option steamOutput(int steamPerTick) {
            return builder -> {
                builder.power = SingleBlockMachinePower.STEAM;
                builder.resource = SingleBlockMachineResource.STEAM;
                builder.resourceMode =
                        SingleBlockMachineResourceMode.PRODUCES;
                builder.steamUsage = steamPerTick;
            };
        }

        /**
         * Defines the ULV base CE/t used by this electric machine family.
         * Valid values are 1 through 8 and each generated tier multiplies it by four.
         */
        static Option energyUsage(int energyUsage) {
            return builder -> builder.energyUsage = energyUsage;
        }

        /**
         * Keeps recipe progress when the machine temporarily runs out of its
         * required energy or steam. Without this option, WAIT_FOR_RESOURCE resets
         * progress to zero, preserving the existing behavior for other machines.
         */
        static Option processing(double durationMultiplier) {
            return builder -> builder.processingProfile = new ProcessingProfile(durationMultiplier, 0);
        }

        static Option noDurationReset() {
            return builder -> builder.noDurationReset = true;
        }

        static Option inputItem(
                String itemId,
                int amount,
                int durationTicks
        ) {
            return builder ->
                    builder.inputItems.add(
                            new StackRequirement(
                                    itemId,
                                    amount,
                                    durationTicks
                            )
                    );
        }

        static Option inputFluid(
                String fluidId,
                int amount,
                int durationTicks
        ) {
            return builder ->
                    builder.inputFluids.add(
                            new StackRequirement(
                                    fluidId,
                                    amount,
                                    durationTicks
                            )
                    );
        }

        static Option outputItem(
                String itemId,
                int amount,
                int durationTicks
        ) {
            return builder ->
                    builder.outputItems.add(
                            new StackRequirement(
                                    itemId,
                                    amount,
                                    durationTicks
                            )
                    );
        }

        static Option outputFluid(
                String fluidId,
                int amount,
                int durationTicks
        ) {
            return builder ->
                    builder.outputFluids.add(
                            new StackRequirement(
                                    fluidId,
                                    amount,
                                    durationTicks
                            )
                    );
        }

        static Option temperature(
                int minimumOperatingTemperature,
                int maximumTemperature,
                int changeIntervalTicks,
                int heatingAmount,
                int coolingAmount,
                Option... operations
        ) {
            return builder -> {
                Builder operationBuilder = new Builder();

                if (operations != null) {
                    for (Option operation : operations) {
                        Objects.requireNonNull(
                                operation,
                                "Temperature operation"
                        ).apply(operationBuilder);
                    }
                }

                builder.temperature = new TemperatureSettings(
                        minimumOperatingTemperature,
                        maximumTemperature,
                        changeIntervalTicks,
                        heatingAmount,
                        coolingAmount,
                        operationBuilder.inputItems,
                        operationBuilder.inputFluids,
                        operationBuilder.outputItems,
                        operationBuilder.outputFluids,
                        operationBuilder.conditions
                );
            };
        }

        static Option blockInteraction(BlockInteraction interaction) {
            return builder ->
                    builder.blockInteractions.add(
                            Objects.requireNonNull(interaction)
                    );
        }

        static Option blockInteraction(
                BlockInteraction.Builder interaction
        ) {
            return blockInteraction(
                    Objects.requireNonNull(interaction).build()
            );
        }

        static Option condition(MachineCondition condition) {
            return builder ->
                    builder.conditions.add(
                            Objects.requireNonNull(condition)
                    );
        }

        static Option modifier(MachineModifier modifier) {
            return builder ->
                    builder.modifiers.add(
                            Objects.requireNonNull(modifier)
                    );
        }

        static Option modifier(MachineModifier.Builder modifier) {
            return modifier(
                    Objects.requireNonNull(modifier).build()
            );
        }

        /**
         * Adds a named controller-relative area. Use left/right, bottom/top and
         * front/back in the MachineArea box; the shape rotates with the block.
         */
        static Option area(MachineArea area) {
            return builder -> builder.areas.add(Objects.requireNonNull(area));
        }

        /** Adds a named controller-relative area from its fluent builder. */
        static Option area(MachineArea.Builder area) {
            return area(Objects.requireNonNull(area).build());
        }

        /**
         * Defines the idle front overlay followed by optional active
         * animation frames.
         */
        static Option frontOverlay(
                String idleOverlay,
                String... activeOverlays
        ) {
            return sideOverlay(
                    MachineSide.FRONT,
                    idleOverlay,
                    activeOverlays
            );
        }

        /**
         * Defines the idle back overlay followed by optional active
         * animation frames.
         */
        static Option backOverlay(
                String idleOverlay,
                String... activeOverlays
        ) {
            return sideOverlay(
                    MachineSide.BACK,
                    idleOverlay,
                    activeOverlays
            );
        }

        /**
         * Defines the idle left overlay followed by optional active
         * animation frames.
         */
        static Option leftOverlay(
                String idleOverlay,
                String... activeOverlays
        ) {
            return sideOverlay(
                    MachineSide.LEFT,
                    idleOverlay,
                    activeOverlays
            );
        }

        /**
         * Defines the idle right overlay followed by optional active
         * animation frames.
         */
        static Option rightOverlay(
                String idleOverlay,
                String... activeOverlays
        ) {
            return sideOverlay(
                    MachineSide.RIGHT,
                    idleOverlay,
                    activeOverlays
            );
        }

        /**
         * Defines the idle top overlay followed by optional active
         * animation frames.
         */
        static Option topOverlay(
                String idleOverlay,
                String... activeOverlays
        ) {
            return sideOverlay(
                    MachineSide.TOP,
                    idleOverlay,
                    activeOverlays
            );
        }

        /**
         * Defines the idle bottom overlay followed by optional active
         * animation frames.
         */
        static Option bottomOverlay(
                String idleOverlay,
                String... activeOverlays
        ) {
            return sideOverlay(
                    MachineSide.BOTTOM,
                    idleOverlay,
                    activeOverlays
            );
        }

        static TextureOption frontTexture(String texture) {
            return sideTexture(MachineSide.FRONT, texture);
        }

        static TextureOption backTexture(String texture) {
            return sideTexture(MachineSide.BACK, texture);
        }

        static TextureOption leftTexture(String texture) {
            return sideTexture(MachineSide.LEFT, texture);
        }

        static TextureOption rightTexture(String texture) {
            return sideTexture(MachineSide.RIGHT, texture);
        }

        static TextureOption topTexture(String texture) {
            return sideTexture(MachineSide.TOP, texture);
        }

        static TextureOption bottomTexture(String texture) {
            return sideTexture(MachineSide.BOTTOM, texture);
        }

        static Option model(String model) {
            return builder -> builder.model = Objects.requireNonNull(model);
        }

        static Option stoneTextureSource(
                StoneMaterial material,
                MaterialPart preferredPart,
                MaterialPart fallbackPart
        ) {
            return builder -> builder.stoneTextureSource = new StoneTextureSource(
                    material,
                    preferredPart,
                    fallbackPart
            );
        }

        private static Option sideOverlay(
                MachineSide side,
                String idleOverlay,
                String... activeOverlays
        ) {
            return builder -> {
                List<String> frames = new ArrayList<>();
                frames.add(Objects.requireNonNull(idleOverlay));

                if (activeOverlays != null) {
                    Collections.addAll(frames, activeOverlays);
                }

                builder.sideOverlays.put(side, frames);
            };
        }

        private static TextureOption sideTexture(
                MachineSide side,
                String texture
        ) {
            return new TextureOption(side, texture);
        }

        final class TextureOption implements Option {
            private final MachineSide side;
            private final String texture;
            private Integer color;

            private TextureOption(MachineSide side, String texture) {
                this.side = Objects.requireNonNull(side);
                this.texture = Objects.requireNonNull(texture);
            }

            public TextureOption color(int color) {
                this.color = color;
                return this;
            }

            @Override
            public void apply(Builder builder) {
                builder.sideTextures.put(side, texture);
                if (color == null) {
                    builder.sideTextureColors.remove(side);
                } else {
                    builder.sideTextureColors.put(side, color);
                }
            }
        }

        static Option mineableWith(
                ToolDefinition tool,
                ToolDefinition... moreTools
        ) {
            return builder -> {
                builder.miningTools = new LinkedHashSet<>();
                builder.miningTools.add(Objects.requireNonNull(tool));

                if (moreTools != null) {
                    Collections.addAll(
                            builder.miningTools,
                            moreTools
                    );
                }
            };
        }

        static Option breakingTier(MachineTier tier) {
            return builder -> {
                Objects.requireNonNull(tier, "breaking tier");
                if (tier == MachineTier.NONE) {
                    throw new IllegalArgumentException("Singleblock breaking tier must be a real machine tier");
                }
                builder.breakingTier = tier.recipeTier();
            };
        }

        static Option strength(float hardness) {
            return strength(hardness, hardness);
        }

        static Option strength(float hardness, float resistance) {
            BlockStrength strength = BlockStrength.of(hardness, resistance);
            return builder -> builder.strength = strength;
        }

        static Option progressBar(ProgressBar progressBar) {
            return builder ->
                    builder.progressBar =
                            Objects.requireNonNull(progressBar);
        }
    }

    public static final class Builder {
        private String id;
        private String displayName;
        private final List<String> tooltips = new ArrayList<>();

        private SingleBlockMachinePower power =
                SingleBlockMachinePower.NONE;

        private SingleBlockMachineResource resource =
                SingleBlockMachineResource.NONE;

        private SingleBlockMachineResourceMode resourceMode =
                SingleBlockMachineResourceMode.NONE;

        private MachineTier startTier = MachineTier.NONE;
        private boolean tierDefined;
        private final List<MachineTier> onlyTiers = new ArrayList<>();
        private final List<ResourceLocation> recipeTypes =
                new ArrayList<>();

        private Slots slots = new Slots(0, 0, 0, 0);
        private ResourceLocation steamConversionRecipe;
        private int steamCapacity;
        private int steamUsage;
        private int energyUsage;
        private boolean noDurationReset;
        private ProcessingProfile processingProfile;
        private int maxParallel = 1;
        private boolean waterloggable;
        private MachineSide kineticInput;
        private MachineSide kineticOutput;
        private double startSu = 8.0D;
        private Optional<Integer> minRpm = Optional.empty();
        private Optional<Integer> maxRpm = Optional.empty();
        private Optional<Integer> outputRpm = Optional.empty();

        private final List<StackRequirement> inputItems =
                new ArrayList<>();

        private final List<StackRequirement> inputFluids =
                new ArrayList<>();

        private final List<StackRequirement> outputItems =
                new ArrayList<>();

        private final List<StackRequirement> outputFluids =
                new ArrayList<>();

        private final List<BlockInteraction> blockInteractions =
                new ArrayList<>();

        private final List<MachineCondition> conditions =
                new ArrayList<>();

        private final List<MachineModifier> modifiers =
                new ArrayList<>();

        private final List<MachineArea> areas =
                new ArrayList<>();

        private final EnumMap<MachineSide, String> sideTextures =
                new EnumMap<>(MachineSide.class);

        private final EnumMap<MachineSide, Integer> sideTextureColors =
                new EnumMap<>(MachineSide.class);

        private final EnumMap<MachineSide, List<String>> sideOverlays =
                new EnumMap<>(MachineSide.class);

        private MachineTier breakingTier = MachineTier.LV;
        private BlockStrength strength;

        private Set<ToolDefinition> miningTools =
                new LinkedHashSet<>(Set.of(Tool.PICKAXE));

        private final EnumSet<MachineSide> noItemInputSides = EnumSet.noneOf(MachineSide.class);
        private final EnumSet<MachineSide> noItemOutputSides = EnumSet.noneOf(MachineSide.class);
        private final EnumSet<MachineSide> noFluidInputSides = EnumSet.noneOf(MachineSide.class);
        private final EnumSet<MachineSide> noFluidOutputSides = EnumSet.noneOf(MachineSide.class);

        private ProgressBar progressBar;
        private TemperatureSettings temperature;
        private String model;
        private StoneTextureSource stoneTextureSource;

        private Builder() {
        }

        public Builder machineDefinition(Option option) {
            Objects.requireNonNull(
                    option,
                    "Singleblock machine option"
            ).apply(this);

            return this;
        }

        public SingleBlockDefinition build() {
            return new SingleBlockDefinition(this);
        }
    }
}
