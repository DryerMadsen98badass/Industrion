package net.mads.industron.recipe;

import net.mads.industron.Industron;
import net.mads.industron.data.CreateRecipeBridge;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.MaterialUnits;
import net.mads.industron.machine.interaction.BlockInteraction;
import net.mads.industron.machine.interaction.MachineCondition;
import net.mads.industron.machine.interaction.MachineModifier;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyToolType;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Typesafe public builder for recipes processed by the CE machine runtime.
 *
 * <p>Create recipes intentionally use their existing Create builders instead.</p>
 */
public final class RecipeDefinition {
    private RecipeTypeDefinition type;
    private String id;
    private final List<SizedIngredient> itemInputs = new ArrayList<>();
    private final List<CEChancedItemInput> chancedItemInputs = new ArrayList<>();
    private final List<SizedFluidIngredient> fluidInputs = new ArrayList<>();
    private final List<CEChancedFluidInput> chancedFluidInputs = new ArrayList<>();
    private final List<SizedIngredient> notConsumableItems = new ArrayList<>();
    private final List<SizedFluidIngredient> notConsumableFluids = new ArrayList<>();
    private final List<CEToolRequirement> tools = new ArrayList<>();
    private final List<CEChancedItemOutput> itemOutputs = new ArrayList<>();
    private final List<FluidStack> fluidOutputs = new ArrayList<>();
    private final List<CEChancedFluidOutput> chancedFluidOutputs = new ArrayList<>();
    private Optional<ResourceLocation> treeSource = Optional.empty();
    private Optional<Integer> duration = Optional.empty();
    private Optional<Integer> manualUses = Optional.empty();
    private Optional<Double> fuelUnits = Optional.empty();
    private Optional<Integer> circuit = Optional.empty();
    private Optional<String> tier = Optional.empty();
    private Optional<Integer> minRpm = Optional.empty();
    private Optional<Integer> maxRpm = Optional.empty();
    private Optional<Integer> outputRpm = Optional.empty();
    private Optional<Integer> requiredTemp = Optional.empty();
    private Optional<ChemicalBalanceRange> chemicalBalanceRange = Optional.empty();
    private final List<ResourceLocation> requiredLogic = new ArrayList<>();
    private final List<ResourceLocation> optionalLogic = new ArrayList<>();
    private final List<BlockInteraction> blockInteractions = new ArrayList<>();
    private final List<MachineCondition> conditions = new ArrayList<>();
    private final List<MachineModifier> modifiers = new ArrayList<>();
    private boolean furnaceFuel;

    private RecipeDefinition() {
    }

    public static RecipeDefinition recipe() {
        return new RecipeDefinition();
    }

    public RecipeDefinition recipeDefinition(Option option) {
        Objects.requireNonNull(option, "Recipe option").apply(this);
        return this;
    }

    /** Declares repeated manual use of a registered tool without treating it as an item input. */
    public RecipeDefinition tool(ToolDefinition tool, int amount) {
        Option.tool(tool, amount).apply(this);
        return this;
    }

    /** Internal compatibility overload for existing generated assembly definitions. */
    public RecipeDefinition tool(AssemblyToolType tool, int amount) {
        Option.tool(tool, amount).apply(this);
        return this;
    }

    /**
     * Options shown by autocomplete inside {@code .recipeDefinition(Option...)}.
     */
    @FunctionalInterface
    public interface Option {
        void apply(RecipeDefinition definition);

        /** Defines the path used below the recipe type's generated data folder. */
        static Option id(String id) {
            return definition -> definition.id = id;
        }

        /** Selects the CE recipe type that owns and validates this recipe. */
        static Option recipeType(RecipeTypeDefinition type) {
            return definition -> definition.type = Objects.requireNonNull(type);
        }

        /** Adds a consumed item input. */
        static Option inputItem(String itemId, int count) {
            return definition -> definition.itemInputs.add(SizedIngredient.of(item(itemId), count));
        }

        /** Adds a consumed item input. */
        static Option inputItem(ItemLike item, int count) {
            return definition -> definition.itemInputs.add(SizedIngredient.of(item, count));
        }

        /** Adds a consumed item-tag input. */
        static Option inputTag(String tagId, int count) {
            return definition -> definition.itemInputs.add(SizedIngredient.of(itemTag(tagId), count));
        }

        /** Adds an item input that must be present, but is only consumed when the chance succeeds. */
        static Option chancedInput(String itemId, int count, int chance) {
            return chancedInput(itemId, count, chance, 0);
        }

        /**
         * Adds an item input that must be present, but is only consumed when the chance succeeds.
         * Tier bonus is added once per runtime tier above the recipe baseline.
         */
        static Option chancedInput(String itemId, int count, int chance, int tierBonus) {
            return definition -> definition.chancedItemInputs.add(new CEChancedItemInput(
                    SizedIngredient.of(item(itemId), count),
                    chance,
                    tierBonus
            ));
        }

        /** Adds an item input that must be present, but is only consumed when the chance succeeds. */
        static Option chancedInput(ItemLike item, int count, int chance) {
            return chancedInput(item, count, chance, 0);
        }

        /**
         * Adds an item input that must be present, but is only consumed when the chance succeeds.
         * Tier bonus is added once per runtime tier above the recipe baseline.
         */
        static Option chancedInput(ItemLike item, int count, int chance, int tierBonus) {
            return definition -> definition.chancedItemInputs.add(new CEChancedItemInput(
                    SizedIngredient.of(item, count),
                    chance,
                    tierBonus
            ));
        }

        /** Adds a consumed fluid input measured in millibuckets. */
        static Option inputFluid(String fluidId, int amount) {
            return definition -> definition.fluidInputs.add(SizedFluidIngredient.of(fluid(fluidId), amount));
        }

        static Option inputFluid(FluidStack stack) {
            FluidStack copy = stack.copy();
            return definition -> definition.fluidInputs.add(new SizedFluidIngredient(
                    net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient.of(true, copy), copy.getAmount()));
        }

        static Option outputFluid(FluidStack stack) {
            return definition -> definition.fluidOutputs.add(stack.copy());
        }

        /** Adds a consumed fluid-tag input measured in millibuckets. */
        static Option inputFluidTag(String tagId, int amount) {
            return definition -> definition.fluidInputs.add(sizedFluidTag(tagId, amount));
        }

        static Option chancedInputTag(String tagId, int count, int chance) {
            return chancedInputTag(tagId, count, chance, 0);
        }

        static Option chancedInputTag(String tagId, int count, int chance, int tierBonus) {
            return definition -> definition.chancedItemInputs.add(new CEChancedItemInput(
                    SizedIngredient.of(itemTag(tagId), count), chance, tierBonus
            ));
        }

        static Option chancedFluidInput(String fluidId, int amount, int chance) {
            return chancedFluidInput(fluidId, amount, chance, 0);
        }

        static Option chancedFluidInput(String fluidId, int amount, int chance, int tierBonus) {
            return definition -> definition.chancedFluidInputs.add(new CEChancedFluidInput(
                    SizedFluidIngredient.of(fluid(fluidId), amount), chance, tierBonus
            ));
        }

        static Option chancedFluidInputTag(String tagId, int amount, int chance) {
            return chancedFluidInputTag(tagId, amount, chance, 0);
        }

        static Option chancedFluidInputTag(String tagId, int amount, int chance, int tierBonus) {
            return definition -> definition.chancedFluidInputs.add(new CEChancedFluidInput(
                    sizedFluidTag(tagId, amount), chance, tierBonus
            ));
        }

        /** Adds an item requirement that must remain present and is not consumed. */
        static Option notConsumableItem(String itemId, int count) {
            return definition -> definition.notConsumableItems.add(SizedIngredient.of(item(itemId), count));
        }

        /** Adds an item requirement that must remain present and is not consumed. */
        static Option notConsumableItem(ItemLike item, int count) {
            return definition -> definition.notConsumableItems.add(SizedIngredient.of(item, count));
        }

        static Option notConsumableInputItemTag(String tagId, int count) {
            return definition -> definition.notConsumableItems.add(SizedIngredient.of(itemTag(tagId), count));
        }

        /** Adds a fluid requirement that must remain present and is not consumed. */
        static Option notConsumableFluid(String fluidId, int amount) {
            return definition -> definition.notConsumableFluids.add(SizedFluidIngredient.of(fluid(fluidId), amount));
        }

        static Option notConsumableFluidTag(String tagId, int amount) {
            return definition -> definition.notConsumableFluids.add(sizedFluidTag(tagId, amount));
        }

        /** Adds a manual tool action. Each completed use costs one durability. */
        static Option tool(ToolDefinition tool, int amount) {
            return definition -> definition.tools.add(CEToolRequirement.of(tool, amount));
        }

        /** Internal compatibility overload for existing generated assembly definitions. */
        static Option tool(AssemblyToolType tool, int amount) {
            return definition -> definition.tools.add(CEToolRequirement.of(tool, amount));
        }

        /** Adds an item output with a guaranteed result. */
        static Option outputItem(String itemId, int count) {
            return definition -> definition.itemOutputs.add(new CEChancedItemOutput(
                    new ItemStack(item(itemId), count),
                    CEChancedItemOutput.MAX_CHANCE
            ));
        }

        /** Adds an item output with a guaranteed result. */
        static Option outputItem(ItemLike item, int count) {
            return definition -> definition.itemOutputs.add(new CEChancedItemOutput(
                    new ItemStack(item, count),
                    CEChancedItemOutput.MAX_CHANCE
            ));
        }

        /**
         * Adds a chanced item output. Chance uses the CE scale where
         * {@link CEChancedItemOutput#MAX_CHANCE} is guaranteed.
         */
        static Option chancedOutputItem(String itemId, int count, int chance) {
            return chancedOutputItem(itemId, count, chance, 0);
        }

        /** Adds a chanced item output from a registered item form. */
        static Option chancedOutputItem(ItemLike item, int count, int chance) {
            return chancedOutputItem(item, count, chance, 0);
        }

        /**
         * Adds a chanced item output. Tier bonus is added once per runtime tier
         * above the recipe baseline and may be negative.
         */
        static Option chancedOutputItem(String itemId, int count, int chance, int tierBonus) {
            return definition -> definition.itemOutputs.add(
                    new CEChancedItemOutput(new ItemStack(item(itemId), count), chance, tierBonus)
            );
        }

        /** Adds a chanced item output from a registered item form, including a per-tier bonus. */
        static Option chancedOutputItem(ItemLike item, int count, int chance, int tierBonus) {
            return definition -> definition.itemOutputs.add(
                    new CEChancedItemOutput(new ItemStack(item, count), chance, tierBonus)
            );
        }

        /** Adds a fluid output measured in millibuckets. */
        static Option outputFluid(String fluidId, int amount) {
            return definition -> definition.fluidOutputs.add(new FluidStack(fluid(fluidId), amount));
        }

        static Option chancedFluidOutput(String fluidId, int amount, int chance) {
            return chancedFluidOutput(fluidId, amount, chance, 0);
        }

        static Option chancedFluidOutput(String fluidId, int amount, int chance, int tierBonus) {
            return definition -> definition.chancedFluidOutputs.add(new CEChancedFluidOutput(
                    new FluidStack(fluid(fluidId), amount), chance, tierBonus
            ));
        }

        /** Defines the naturally grown tree log represented by this recipe. */
        static Option treeSource(String blockId) {
            return definition -> definition.treeSource = Optional.of(resourceId(blockId));
        }

        /**
         * Generates one normal recipe for every registered furnace fuel.
         *
         * <p>The generated recipe consumes one fuel item and uses one second of
         * boiler duration for every item the fuel can smelt in a normal furnace.
         * Since one furnace smelt takes 200 ticks and one boiler second is
         * 20 ticks, the generated duration is {@code burnTime / 10}.</p>
         */
        static Option furnaceFuel() {
            return definition -> definition.furnaceFuel = true;
        }

        /** Defines the base processing duration in ticks. */
        static Option duration(int duration) {
            return definition -> definition.duration = Optional.of(duration);
        }

        /** Defines the number of deliberate hand uses/right-clicks required by a manual recipe. */
        static Option uses(int uses) {
            return definition -> definition.manualUses = Optional.of(uses);
        }

        /** Defines the machine-independent chemical energy stored by a generic fuel recipe. */
        static Option fuelUnits(double units) {
            if (!Double.isFinite(units) || units <= 0.0D) {
                throw new IllegalArgumentException("Fuel Units must be finite and positive");
            }
            return definition -> definition.fuelUnits = Optional.of(units);
        }

        /** Requires an integrated circuit configuration from 1 through 32. */
        static Option circuit(int circuit) {
            if (circuit < 1 || circuit > 32) {
                throw new IllegalArgumentException("Circuit must be between 1 and 32");
            }
            return definition -> definition.circuit = Optional.of(circuit);
        }

        /** Defines the minimum machine tier allowed to run the recipe. */
        static Option tier(MachineTier tier) {
            return definition -> definition.tier = Optional.of(Objects.requireNonNull(tier).id());
        }

        /** Defines the minimum accepted rotational speed. */
        static Option minRpm(int rpm) {
            return definition -> definition.minRpm = Optional.of(rpm);
        }

        /** Defines the maximum accepted rotational speed. */
        static Option maxRpm(int rpm) {
            return definition -> definition.maxRpm = Optional.of(rpm);
        }

        /** Defines the rotational speed produced while this recipe is active. */
        static Option outputRpm(int rpm) {
            return definition -> definition.outputRpm = Optional.of(rpm);
        }

        /** Defines the minimum machine or coil temperature required by the recipe. */
        static Option temperature(int requiredTemperature) {
            if (requiredTemperature <= 0) {
                throw new IllegalArgumentException("Temperature requirement must be positive");
            }
            return definition -> definition.requiredTemp = Optional.of(requiredTemperature);
        }

        /**
         * Declares a melting heat requirement. The recipe then needs a formed coil multiblock
         * whose available heat is at least this temperature.
         */
        static Option coilTemperature(int requiredTemperature) {
            return definition -> {
                temperature(requiredTemperature).apply(definition);
                requiredLogic(CERecipeLogics.COIL_TEMP).apply(definition);
            };
        }

        /** Defines the inclusive Chemical Balance range required while the recipe is selected and processing. */
        static Option chemicalBalanceRange(double minimum, double maximum) {
            ChemicalBalanceRange range = ChemicalBalanceRange.of(minimum, maximum);
            return definition -> definition.chemicalBalanceRange = Optional.of(range);
        }

        /** Adds a custom logic requirement that must be available. */
        static Option requiredLogic(CERecipeLogicDefinition logic) {
            return definition -> definition.requiredLogic.add(Objects.requireNonNull(logic).id());
        }

        /** Adds custom logic that can be used when available but is not mandatory. */
        static Option optionalLogic(CERecipeLogicDefinition logic) {
            return definition -> definition.optionalLogic.add(Objects.requireNonNull(logic).id());
        }

        /** Adds a world block/fluid interaction required only by this recipe. */
        static Option blockInteraction(BlockInteraction interaction) {
            return definition -> definition.blockInteractions.add(Objects.requireNonNull(interaction));
        }

        /** Adds a world block/fluid interaction required only by this recipe. */
        static Option blockInteraction(BlockInteraction.Builder interaction) {
            return blockInteraction(Objects.requireNonNull(interaction).build());
        }

        /** Adds a world condition required only by this recipe. */
        static Option condition(MachineCondition condition) {
            return definition -> definition.conditions.add(Objects.requireNonNull(condition));
        }

        /** Adds an ordered modifier available only to this recipe. */
        static Option modifier(MachineModifier modifier) {
            return definition -> definition.modifiers.add(Objects.requireNonNull(modifier));
        }

        /** Adds an ordered modifier available only to this recipe. */
        static Option modifier(MachineModifier.Builder modifier) {
            return modifier(Objects.requireNonNull(modifier).build());
        }
    }

    public CERecipe build() {
        validate();
        return new CERecipe(
                type.id(),
                itemInputs,
                chancedItemInputs,
                fluidInputs,
                chancedFluidInputs,
                notConsumableItems,
                notConsumableFluids,
                tools,
                itemOutputs,
                fluidOutputs,
                chancedFluidOutputs,
                treeSource,
                duration,
                manualUses,
                fuelUnits,
                circuit,
                effectiveTier(),
                minRpm,
                maxRpm,
                outputRpm,
                requiredTemp,
                chemicalBalanceRange,
                requiredLogic,
                optionalLogic,
                blockInteractions,
                conditions,
                modifiers,
                furnaceFuel
        );
    }

    private Optional<String> effectiveTier() {
        return type != null && type.ignoresTier() ? Optional.empty() : tier;
    }

    public void save(RecipeOutput output) {
        validateIdentity();
        CERecipe recipe = build();
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID, type.id().getPath() + "/" + id
        );
        output.accept(recipeId, recipe, null);
        CreateRecipeBridge.save(output, recipeId, recipe);
        type.generatedRecipeType().ifPresent(generatedTypeId -> {
            RecipeTypeDefinition generatedType = CERecipeTypes.byId(generatedTypeId);
            if (generatedType == null) {
                throw new IllegalStateException("Unknown generated recipe type: " + generatedTypeId);
            }
            validateGeneratedType(generatedType);
            ResourceLocation generatedRecipeId = ResourceLocation.fromNamespaceAndPath(
                    Industron.MOD_ID, generatedType.id().getPath() + "/" + id
            );
            CERecipe generatedRecipe = recipe.automatedCopy(generatedType.id());
            output.accept(generatedRecipeId, generatedRecipe, null);
            CreateRecipeBridge.save(output, generatedRecipeId, generatedRecipe);
        });
    }

    private void validateGeneratedType(RecipeTypeDefinition generatedType) {
        if (generatedType.id().equals(type.id())) {
            throw new IllegalStateException("Recipe type " + type.id() + " cannot generate itself");
        }
        if (duration.isEmpty() || duration.orElse(0) <= 0) {
            throw new IllegalStateException("Recipe " + id + " generates automated type "
                    + generatedType.id() + " but has no positive duration");
        }
        if (itemInputs.size() + chancedItemInputs.size() + notConsumableItems.size() > generatedType.maxItemInputs()
                || fluidInputs.size() + chancedFluidInputs.size() + notConsumableFluids.size() > generatedType.maxFluidInputs()
                || itemOutputs.size() > generatedType.maxItemOutputs()
                || fluidOutputs.size() + chancedFluidOutputs.size() > generatedType.maxFluidOutputs()) {
            throw new IllegalStateException("Recipe " + id + " does not fit generated type " + generatedType.id());
        }
        for (ResourceLocation logic : requiredLogic) {
            if (!generatedType.supportsLogic(logic)) {
                throw new IllegalStateException("Generated type " + generatedType.id() + " does not support " + logic);
            }
        }
        for (ResourceLocation logic : optionalLogic) {
            if (!generatedType.supportsLogic(logic)) {
                throw new IllegalStateException("Generated type " + generatedType.id() + " does not support " + logic);
            }
        }
    }

    private void validate() {
        validateIdentity();

        if (duration.isPresent() && duration.get() <= 0) {
            throw new IllegalStateException("Recipe " + id + " must have a positive duration when specified");
        }
        if (manualUses.isPresent() && manualUses.get() <= 0) {
            throw new IllegalStateException("Recipe " + id + " must have a positive use count when specified");
        }
        if (fuelUnits.isPresent() && fuelUnits.get() <= 0.0D) {
            throw new IllegalStateException("Recipe " + id + " must have positive fuel units when specified");
        }
        if (type.id().equals(CERecipeTypes.FUEL.id())) {
            validateFuelRecipe();
        } else if (fuelUnits.isPresent()) {
            throw new IllegalStateException("Only generic Fuel recipes may use Option.fuelUnits(...): " + id);
        }
        if (type.id().equals(CERecipeTypes.KILN_FIRING.id())) {
            if (itemInputs.size() != 1
                    || itemInputs.getFirst().count() != 1
                    || !chancedItemInputs.isEmpty()
                    || !notConsumableItems.isEmpty()
                    || !fluidInputs.isEmpty()
                    || !chancedFluidInputs.isEmpty()
                    || !notConsumableFluids.isEmpty()
                    || itemOutputs.size() != 1
                    || !itemOutputs.getFirst().guaranteed()
                    || itemOutputs.getFirst().stack().getCount() != 1
                    || !fluidOutputs.isEmpty()
                    || !chancedFluidOutputs.isEmpty()) {
                throw new IllegalStateException(
                        "Kiln Firing recipe " + id
                                + " must be exactly one consumed item -> one guaranteed item; Kiln chambers hold one item"
                );
            }
        }
        if (type.id().equals(CERecipeTypes.HAND_PROCESSING.id())) {
            if (manualUses.isEmpty()) {
                throw new IllegalStateException("Hand Processing recipe " + id + " requires Option.uses(...)");
            }
            if (itemInputs.size() != 1
                    || !chancedItemInputs.isEmpty()
                    || !notConsumableItems.isEmpty()
                    || !fluidInputs.isEmpty()
                    || !chancedFluidInputs.isEmpty()
                    || !notConsumableFluids.isEmpty()
                    || !tools.isEmpty()
                    || itemOutputs.isEmpty()
                    || !fluidOutputs.isEmpty()
                    || !chancedFluidOutputs.isEmpty()) {
                throw new IllegalStateException(
                        "Hand Processing recipe " + id
                                + " must use one consumed item input, item output(s), no fluids and no tools"
                );
            }
        } else if (manualUses.isPresent()) {
            throw new IllegalStateException(
                    "Only Hand Processing recipes may use Option.uses(...): " + id
            );
        }
        if (furnaceFuel && (!itemInputs.isEmpty() || !chancedItemInputs.isEmpty())) {
            throw new IllegalStateException("Recipe " + id + " cannot combine Option.furnaceFuel() with normal item inputs");
        }
        if (furnaceFuel && treeSource.isPresent()) {
            throw new IllegalStateException("Recipe " + id + " cannot combine Option.furnaceFuel() with treeSource");
        }
        if (furnaceFuel && type.maxItemInputs() < 1) {
            throw new IllegalStateException("Recipe type " + type.id() + " does not have an item input slot for furnace fuel");
        }
        if (treeSource.isPresent() && (!itemInputs.isEmpty() || !chancedItemInputs.isEmpty())) {
            throw new IllegalStateException("Recipe " + id + " cannot use both treeSource and item inputs");
        }
        if (itemInputs.size() + chancedItemInputs.size() + notConsumableItems.size() > type.maxItemInputs()) {
            throw new IllegalStateException("Recipe " + id + " has too many item inputs for " + type.id());
        }
        for (CEToolRequirement tool : tools) {
            if (tool.registeredType().isEmpty()) {
                throw new IllegalStateException("Recipe " + id + " uses unregistered tool " + tool.toolId());
            }
        }

        validateCommonRecipeRules();
    }

    private void validateFuelRecipe() {
        if (fuelUnits.isEmpty()) {
            throw new IllegalStateException("Fuel recipe " + id + " requires Option.fuelUnits(...)");
        }
        int consumedInputs = itemInputs.size() + fluidInputs.size();
        boolean validFluidFuelAmount = fluidInputs.size() == 1
                && (fluidInputs.getFirst().amount() == MaterialUnits.LIQUID_MILLIBUCKETS_PER_UNIT
                || fluidInputs.getFirst().amount() == MaterialUnits.GAS_MILLIBUCKETS_PER_UNIT);
        boolean validFuelAmount = (itemInputs.size() == 1
                && itemInputs.getFirst().count() == 1
                && fluidInputs.isEmpty())
                || (validFluidFuelAmount && itemInputs.isEmpty());
        if (consumedInputs != 1
                || !validFuelAmount
                || !chancedItemInputs.isEmpty()
                || !chancedFluidInputs.isEmpty()
                || !notConsumableItems.isEmpty()
                || !notConsumableFluids.isEmpty()
                || !tools.isEmpty()
                || !itemOutputs.isEmpty()
                || !fluidOutputs.isEmpty()
                || !chancedFluidOutputs.isEmpty()
                || treeSource.isPresent()
                || duration.isPresent()
                || manualUses.isPresent()
                || circuit.isPresent()
                || minRpm.isPresent()
                || maxRpm.isPresent()
                || outputRpm.isPresent()
                || requiredTemp.isPresent()
                || chemicalBalanceRange.isPresent()
                || !requiredLogic.isEmpty()
                || !optionalLogic.isEmpty()
                || !blockInteractions.isEmpty()
                || !conditions.isEmpty()
                || !modifiers.isEmpty()
                || furnaceFuel) {
            throw new IllegalStateException(
                    "Fuel recipe " + id + " must consume exactly 1 item, "
                            + MaterialUnits.LIQUID_MILLIBUCKETS_PER_UNIT + " mB liquid, or "
                            + MaterialUnits.GAS_MILLIBUCKETS_PER_UNIT + " mB gas and only fuel_units"
            );
        }
    }

    private void validateCommonRecipeRules() {
        if (itemOutputs.size() > type.maxItemOutputs()) {
            throw new IllegalStateException("Recipe " + id + " has too many item outputs for " + type.id());
        }
        if (fluidInputs.size() + chancedFluidInputs.size() + notConsumableFluids.size() > type.maxFluidInputs()) {
            throw new IllegalStateException("Recipe " + id + " has too many fluid inputs for " + type.id());
        }
        if (fluidOutputs.size() + chancedFluidOutputs.size() > type.maxFluidOutputs()) {
            throw new IllegalStateException("Recipe " + id + " has too many fluid outputs for " + type.id());
        }
        if (minRpm.isPresent() && (minRpm.get() < 1 || minRpm.get() > CERecipe.DEFAULT_MAX_RPM)) {
            throw new IllegalStateException("Recipe " + id + " has min RPM outside 1-" + CERecipe.DEFAULT_MAX_RPM);
        }
        if (maxRpm.isPresent() && (maxRpm.get() < 1 || maxRpm.get() > CERecipe.DEFAULT_MAX_RPM)) {
            throw new IllegalStateException("Recipe " + id + " has max RPM outside 1-" + CERecipe.DEFAULT_MAX_RPM);
        }
        if (maxRpm.isPresent() && minRpm.isPresent() && maxRpm.get() < minRpm.get()) {
            throw new IllegalStateException("Recipe " + id + " has max RPM lower than min RPM");
        }
        if (outputRpm.isPresent()
                && (outputRpm.get() < 1 || outputRpm.get() > CERecipe.DEFAULT_MAX_RPM)) {
            throw new IllegalStateException(
                    "Recipe " + id + " has output RPM outside 1-" + CERecipe.DEFAULT_MAX_RPM
            );
        }
        if (type.requiresCoilTemperature()) {
            if (requiredTemp.isEmpty()) {
                throw new IllegalStateException(
                        "Recipe " + id + " of " + type.id() + " requires a positive coil temperature"
                );
            }
            if (!requiredLogic.contains(CERecipeLogics.COIL_TEMP.id())) {
                throw new IllegalStateException(
                        "Recipe " + id + " of " + type.id() + " requires " + CERecipeLogics.COIL_TEMP.id()
                );
            }
        }
        validateLogic(requiredLogic);
        validateLogic(optionalLogic);
    }

    private void validateIdentity() {
        if (type == null) {
            throw new IllegalStateException("Recipe is missing Option.recipeType(...)");
        }
        if (id == null || id.isBlank() || !ResourceLocation.isValidPath(id)) {
            throw new IllegalStateException("Recipe has an invalid or missing Option.id(...): " + id);
        }
    }

    private void validateLogic(List<ResourceLocation> logicIds) {
        for (ResourceLocation logicId : logicIds) {
            if (!type.supportsLogic(logicId)) {
                throw new IllegalStateException(
                        "Recipe " + id + " uses logic " + logicId + " but " + type.id() + " does not support it"
                );
            }
        }
    }

    private static ResourceLocation resourceId(String id) {
        return id.contains(":")
                ? ResourceLocation.parse(id)
                : ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, id);
    }

    private static Item item(String itemId) {
        return BuiltInRegistries.ITEM.get(resourceId(itemId));
    }

    private static Fluid fluid(String fluidId) {
        return BuiltInRegistries.FLUID.get(resourceId(fluidId));
    }

    private static TagKey<Item> itemTag(String tagId) {
        return ItemTags.create(resourceId(tagId));
    }

    private static SizedFluidIngredient sizedFluidTag(String tagId, int amount) {
        return new SizedFluidIngredient(
                FluidIngredient.tag(FluidTags.create(resourceId(tagId))),
                amount
        );
    }
}
