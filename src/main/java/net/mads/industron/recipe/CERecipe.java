package net.mads.industron.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mads.industron.Industron;
import net.mads.industron.machine.MachineDrive;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.MachineTierStats;
import net.mads.industron.machine.interaction.BlockInteraction;
import net.mads.industron.machine.interaction.MachineCondition;
import net.mads.industron.machine.interaction.MachineModifier;
import net.mads.industron.registry.RecipeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record CERecipe(
        ResourceLocation recipeType,
        List<SizedIngredient> itemInputs,
        List<CEChancedItemInput> chancedItemInputs,
        List<SizedFluidIngredient> fluidInputs,
        List<CEChancedFluidInput> chancedFluidInputs,
        List<SizedIngredient> notConsumableItems,
        List<SizedFluidIngredient> notConsumableFluids,
        List<CEChancedItemOutput> itemOutputs,
        List<FluidStack> fluidOutputs,
        List<CEChancedFluidOutput> chancedFluidOutputs,
        Optional<ResourceLocation> treeSource,
        int duration,
        Optional<Integer> circuit,
        Optional<String> tier,
        Optional<Integer> minRpm,
        Optional<Integer> maxRpm,
        Optional<Integer> outputRpm,
        Optional<Integer> requiredTemp,
        Optional<ChemicalBalanceRange> chemicalBalanceRange,
        List<ResourceLocation> requiredLogic,
        List<ResourceLocation> optionalLogic,
        List<BlockInteraction> blockInteractions,
        List<MachineCondition> conditions,
        List<MachineModifier> modifiers,
        boolean furnaceFuel
) implements Recipe<CERecipeInput> {
    public static final int DEFAULT_MAX_RPM = 256;

    public static final MapCodec<CERecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("recipe_type").forGetter(CERecipe::recipeType),
            InputFields.CODEC.forGetter(recipe -> new InputFields(
                    recipe.itemInputs(),
                    recipe.chancedItemInputs(),
                    recipe.fluidInputs(),
                    recipe.chancedFluidInputs(),
                    recipe.notConsumableItems(),
                    recipe.notConsumableFluids()
            )),
            CEChancedItemOutput.CODEC.listOf().optionalFieldOf("item_outputs", List.of()).forGetter(CERecipe::itemOutputs),
            FluidStack.CODEC.listOf().optionalFieldOf("fluid_outputs", List.of()).forGetter(CERecipe::fluidOutputs),
            CEChancedFluidOutput.CODEC.listOf().optionalFieldOf("chanced_fluid_outputs", List.of()).forGetter(CERecipe::chancedFluidOutputs),
            ExtraCodecs.POSITIVE_INT.fieldOf("duration").forGetter(CERecipe::duration),
            ExtraCodecs.intRange(1, 32).optionalFieldOf("circuit").forGetter(CERecipe::circuit),
            ExtraCodecs.NON_EMPTY_STRING.optionalFieldOf("tier").forGetter(CERecipe::tier),
            LegacyPowerFields.CODEC.forGetter(recipe -> LegacyPowerFields.EMPTY),
            RuntimeFields.CODEC.forGetter(recipe -> new RuntimeFields(
                    recipe.treeSource(),
                    recipe.minRpm(),
                    recipe.maxRpm(),
                    recipe.outputRpm(),
                    recipe.requiredTemp(),
                    recipe.chemicalBalanceRange(),
                    Optional.empty(),
                    recipe.furnaceFuel()
            )),
            LogicFields.CODEC.forGetter(recipe -> new LogicFields(recipe.requiredLogic(), recipe.optionalLogic())),
            InteractionFields.CODEC.forGetter(recipe -> new InteractionFields(
                    recipe.blockInteractions(),
                    recipe.conditions(),
                    recipe.modifiers()
            ))
    ).apply(instance, CERecipe::fromCodec));

    public CERecipe {
        itemInputs = List.copyOf(itemInputs);
        chancedItemInputs = List.copyOf(chancedItemInputs);
        fluidInputs = List.copyOf(fluidInputs);
        chancedFluidInputs = List.copyOf(chancedFluidInputs);
        notConsumableItems = List.copyOf(notConsumableItems);
        notConsumableFluids = List.copyOf(notConsumableFluids);
        itemOutputs = List.copyOf(itemOutputs);
        fluidOutputs = List.copyOf(fluidOutputs);
        chancedFluidOutputs = List.copyOf(chancedFluidOutputs);
        treeSource = treeSource == null ? Optional.empty() : treeSource;
        circuit = circuit == null ? Optional.empty() : circuit;
        tier = tier == null ? Optional.empty() : tier;
        minRpm = minRpm == null ? Optional.empty() : minRpm;
        maxRpm = maxRpm == null ? Optional.empty() : maxRpm;
        outputRpm = outputRpm == null ? Optional.empty() : outputRpm;
        requiredTemp = requiredTemp == null ? Optional.empty() : requiredTemp;
        chemicalBalanceRange = chemicalBalanceRange == null ? Optional.empty() : chemicalBalanceRange;
        requiredLogic = List.copyOf(requiredLogic);
        optionalLogic = List.copyOf(optionalLogic);
        blockInteractions = List.copyOf(blockInteractions);
        conditions = List.copyOf(conditions);
        modifiers = List.copyOf(modifiers);
        if (minRpm.isPresent() && maxRpm.isPresent() && maxRpm.get() < minRpm.get()) {
            throw new IllegalArgumentException("Maximum RPM cannot be lower than minimum RPM");
        }
        if (outputRpm.isPresent()
                && (outputRpm.get() < 1 || outputRpm.get() > DEFAULT_MAX_RPM)) {
            throw new IllegalArgumentException(
                    "Output RPM must be between 1 and " + DEFAULT_MAX_RPM
            );
        }
    }

    private static CERecipe fromCodec(
            ResourceLocation recipeType,
            InputFields inputFields,
            List<CEChancedItemOutput> itemOutputs,
            List<FluidStack> fluidOutputs,
            List<CEChancedFluidOutput> chancedFluidOutputs,
            int duration,
            Optional<Integer> circuit,
            Optional<String> tier,
            LegacyPowerFields legacyPowerFields,
            RuntimeFields runtimeFields,
            LogicFields logicFields,
            InteractionFields interactionFields
    ) {
        return new CERecipe(
                normalizeLegacyRecipeType(recipeType),
                inputFields.itemInputs(),
                inputFields.chancedItemInputs(),
                inputFields.fluidInputs(),
                inputFields.chancedFluidInputs(),
                inputFields.notConsumableItems(),
                inputFields.notConsumableFluids(),
                itemOutputs,
                fluidOutputs,
                chancedFluidOutputs,
                runtimeFields.treeSource(),
                duration,
                circuit,
                resolvedTier(tier, legacyPowerFields),
                runtimeFields.minRpm(),
                runtimeFields.maxRpm(),
                runtimeFields.outputRpm(),
                runtimeFields.requiredTemp(),
                runtimeFields.resolvedChemicalBalanceRange(),
                logicFields.requiredLogic(),
                logicFields.optionalLogic(),
                interactionFields.blockInteractions(),
                interactionFields.conditions(),
                interactionFields.modifiers(),
                runtimeFields.furnaceFuel()
        );
    }

    private static ResourceLocation normalizeLegacyRecipeType(ResourceLocation recipeType) {
        if (!Industron.MOD_ID.equals(recipeType.getNamespace())) {
            return recipeType;
        }
        return switch (recipeType.getPath()) {
            default -> recipeType;
        };
    }

    private record InputFields(
            List<SizedIngredient> itemInputs,
            List<CEChancedItemInput> chancedItemInputs,
            List<SizedFluidIngredient> fluidInputs,
            List<CEChancedFluidInput> chancedFluidInputs,
            List<SizedIngredient> notConsumableItems,
            List<SizedFluidIngredient> notConsumableFluids
    ) {
        private static final MapCodec<InputFields> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                SizedIngredient.FLAT_CODEC.listOf().optionalFieldOf("item_inputs", List.of()).forGetter(InputFields::itemInputs),
                CEChancedItemInput.CODEC.listOf().optionalFieldOf("chanced_item_inputs", List.of()).forGetter(InputFields::chancedItemInputs),
                SizedFluidIngredient.FLAT_CODEC.listOf().optionalFieldOf("fluid_inputs", List.of()).forGetter(InputFields::fluidInputs),
                CEChancedFluidInput.CODEC.listOf().optionalFieldOf("chanced_fluid_inputs", List.of()).forGetter(InputFields::chancedFluidInputs),
                SizedIngredient.FLAT_CODEC.listOf().optionalFieldOf("not_consumable_items", List.of()).forGetter(InputFields::notConsumableItems),
                SizedFluidIngredient.FLAT_CODEC.listOf().optionalFieldOf("not_consumable_fluids", List.of()).forGetter(InputFields::notConsumableFluids)
        ).apply(instance, InputFields::new));
    }

    /**
     * Reads the old power-specific fields so existing generated data keeps its
     * processing tier. New recipes never encode these fields.
     */
    private record LegacyPowerFields(int cet, Optional<String> kineticTier) {
        private static final LegacyPowerFields EMPTY = new LegacyPowerFields(0, Optional.empty());
        private static final MapCodec<LegacyPowerFields> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("cet", 0).forGetter(LegacyPowerFields::cet),
                ExtraCodecs.NON_EMPTY_STRING.optionalFieldOf("kinetic").forGetter(LegacyPowerFields::kineticTier)
        ).apply(instance, LegacyPowerFields::new));

        private Optional<MachineTier> minimumTier() {
            Optional<MachineTier> result = kineticTier.flatMap(CERecipe::tierById);
            if (cet > 0) {
                MachineTier energyTier = MachineTierStats.tierForCEt(cet);
                result = result.map(tier -> MachineTierStats.max(tier, energyTier)).or(() -> Optional.of(energyTier));
            }
            return result;
        }
    }

    private record RuntimeFields(
            Optional<ResourceLocation> treeSource,
            Optional<Integer> minRpm,
            Optional<Integer> maxRpm,
            Optional<Integer> outputRpm,
            Optional<Integer> requiredTemp,
            Optional<ChemicalBalanceRange> chemicalBalanceRange,
            Optional<LegacyPhRange> legacyPhRange,
            boolean furnaceFuel
    ) {
        private static final MapCodec<RuntimeFields> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("tree_source").forGetter(RuntimeFields::treeSource),
                ExtraCodecs.intRange(1, DEFAULT_MAX_RPM).optionalFieldOf("min_rpm").forGetter(RuntimeFields::minRpm),
                ExtraCodecs.intRange(1, DEFAULT_MAX_RPM).optionalFieldOf("max_rpm").forGetter(RuntimeFields::maxRpm),
                ExtraCodecs.intRange(1, DEFAULT_MAX_RPM).optionalFieldOf("output_rpm").forGetter(RuntimeFields::outputRpm),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("temp").forGetter(RuntimeFields::requiredTemp),
                ChemicalBalanceRange.CODEC.optionalFieldOf("chemical_balance_range").forGetter(RuntimeFields::chemicalBalanceRange),
                LegacyPhRange.CODEC.optionalFieldOf("ph_range").forGetter(RuntimeFields::legacyPhRange),
                com.mojang.serialization.Codec.BOOL.optionalFieldOf("furnace_fuel", false).forGetter(RuntimeFields::furnaceFuel)
        ).apply(instance, RuntimeFields::new));

        private Optional<ChemicalBalanceRange> resolvedChemicalBalanceRange() {
            if (chemicalBalanceRange.isPresent()) {
                return chemicalBalanceRange;
            }
            return legacyPhRange.map(LegacyPhRange::toChemicalBalanceRange);
        }
    }

    private record LegacyPhRange(double min, double max) {
        private static final Codec<LegacyPhRange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                com.mojang.serialization.Codec.DOUBLE.fieldOf("min").forGetter(LegacyPhRange::min),
                com.mojang.serialization.Codec.DOUBLE.fieldOf("max").forGetter(LegacyPhRange::max)
        ).apply(instance, LegacyPhRange::new));

        private ChemicalBalanceRange toChemicalBalanceRange() {
            if (!Double.isFinite(min) || !Double.isFinite(max) || min < 0.0D || max > 14.0D || max < min) {
                throw new IllegalArgumentException("Legacy pH range must be between 0 and 14");
            }
            double cbMin = (7.0D - max) / 7.0D * 100.0D;
            double cbMax = (7.0D - min) / 7.0D * 100.0D;
            return ChemicalBalanceRange.of(cbMin, cbMax);
        }
    }

    private record LogicFields(List<ResourceLocation> requiredLogic, List<ResourceLocation> optionalLogic) {
        private static final MapCodec<LogicFields> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.listOf().optionalFieldOf("required_logic", List.of()).forGetter(LogicFields::requiredLogic),
                ResourceLocation.CODEC.listOf().optionalFieldOf("optional_logic", List.of()).forGetter(LogicFields::optionalLogic)
        ).apply(instance, LogicFields::new));
    }

    private record InteractionFields(
            List<BlockInteraction> blockInteractions,
            List<MachineCondition> conditions,
            List<MachineModifier> modifiers
    ) {
        private static final MapCodec<InteractionFields> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BlockInteraction.CODEC.listOf().optionalFieldOf("block_interactions", List.of()).forGetter(InteractionFields::blockInteractions),
                MachineCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(InteractionFields::conditions),
                MachineModifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(InteractionFields::modifiers)
        ).apply(instance, InteractionFields::new));
    }

    private static Optional<MachineTier> tierById(String id) {
        return MachineTier.ALL.stream()
                .filter(machineTier -> machineTier.id().equals(id))
                .findFirst();
    }

    private static Optional<String> resolvedTier(
            Optional<String> explicitTier,
            LegacyPowerFields legacyPowerFields
    ) {
        Optional<MachineTier> explicit = explicitTier.flatMap(CERecipe::tierById);
        Optional<MachineTier> legacy = legacyPowerFields.minimumTier();
        if (explicit.isPresent() && legacy.isPresent()) {
            return Optional.of(MachineTierStats.max(explicit.get(), legacy.get()).id());
        }
        if (explicit.isPresent()) {
            return Optional.of(explicit.get().id());
        }
        if (legacy.isPresent()) {
            return Optional.of(legacy.get().id());
        }
        return explicitTier;
    }

    public Optional<MachineTier> requiredTier() {
        return tier.flatMap(CERecipe::tierById);
    }

    public Optional<MachineTier> minimumRuntimeTier() {
        return requiredTier();
    }

    public Optional<Integer> effectiveMaxRpm() {
        return maxRpm;
    }

    public boolean usesRpm() {
        return minRpm.isPresent() || maxRpm.isPresent();
    }

    public int baseRpm() {
        if (minRpm.isPresent()) {
            return Math.max(1, minRpm.get());
        }
        if (maxRpm.isPresent() && maxRpm.get() < 64) {
            return Math.max(1, maxRpm.get());
        }
        return 64;
    }

    public Optional<ItemStack> furnaceFuelStack(CERecipeInput input) {
        if (!furnaceFuel) {
            return Optional.empty();
        }
        return input.items().stream()
                .filter(stack -> !stack.isEmpty() && stack.getBurnTime(RecipeType.SMELTING) > 0)
                .findFirst()
                .map(ItemStack::copy);
    }

    public int runtimeDuration(
            MachineTier runtimeTier,
            MachineDrive drive,
            int rpm,
            CERecipeInput input
    ) {
        int baseDuration = furnaceFuelStack(input)
                .map(stack -> Math.max(1, stack.getBurnTime(RecipeType.SMELTING) / 10))
                .orElse(duration);
        return runtimeDuration(runtimeTier, drive, rpm, baseDuration);
    }

    public int runtimeDuration(MachineTier runtimeTier, MachineDrive drive, int rpm) {
        return runtimeDuration(runtimeTier, drive, rpm, duration);
    }

    /** Compatibility overload that uses the drive supplied by the input. */
    public int runtimeDuration(MachineTier runtimeTier, int rpm, CERecipeInput input) {
        return runtimeDuration(runtimeTier, input.drive(), rpm, input);
    }

    /** Compatibility overload for non-kinetic displays and callers. */
    public int runtimeDuration(MachineTier runtimeTier, int rpm) {
        return runtimeDuration(runtimeTier, MachineDrive.NONE, rpm, duration);
    }

    private int runtimeDuration(
            MachineTier runtimeTier,
            MachineDrive drive,
            int rpm,
            int baseDuration
    ) {
        int adjustedDuration = baseDuration;
        Optional<MachineTier> required = requiredTier();
        if (required.isPresent() && MachineTierStats.isAtLeast(runtimeTier, required.get())) {
            int factor = MachineTierStats.tierOverclockFactor(required.get(), runtimeTier);
            adjustedDuration = Math.max(1, (adjustedDuration + factor - 1) / factor);
        }
        return rpmAdjustedDuration(adjustedDuration, drive, rpm);
    }

    private int rpmAdjustedDuration(int baseDuration, MachineDrive drive, int rpm) {
        if (!drive.usesKinetic()) {
            return baseDuration;
        }

        int effectiveRpm = Math.max(1, rpm);
        double rpmFactor = Math.sqrt(effectiveRpm / (double) baseRpm());
        return Math.max(1, (int) Math.ceil(baseDuration / rpmFactor));
    }

    @Override
    public boolean matches(CERecipeInput input, Level level) {
        return matches(input, level, true);
    }

    /**
     * Matches every recipe requirement except the live kinetic RPM window.
     * Kinetic machines use this while selecting and consuming a recipe so the
     * execution can be locked first and then wait for the requested RPM.
     */
    public boolean matchesIgnoringRpm(CERecipeInput input, Level level) {
        return matches(input, level, false);
    }

    private boolean matches(CERecipeInput input, Level level, boolean checkRpm) {
        if (circuit.isPresent() && !circuit.equals(input.circuit())) {
            return false;
        }

        if (checkRpm && input.usesKinetic()) {
            if (input.rpm() < 1 || input.rpm() > DEFAULT_MAX_RPM) {
                return false;
            }
            if (minRpm.isPresent() && input.rpm() < minRpm.get()) {
                return false;
            }
            if (maxRpm.isPresent() && input.rpm() > maxRpm.get()) {
                return false;
            }
        }

        if (requiredTemp.isPresent() && input.coilHeat() < requiredTemp.get()) {
            return false;
        }
        if (!input.availableLogic().containsAll(requiredLogic)) {
            return false;
        }

        Optional<MachineTier> required = requiredTier();
        Optional<MachineTier> actual = input.processingTier();
        if (required.isPresent()) {
            if (actual.isPresent()) {
                if (!MachineTierStats.isAtLeast(actual.get(), required.get())) {
                    return false;
                }
            } else if (input.drive() != MachineDrive.NONE) {
                return false;
            }
        }

        if (furnaceFuel && furnaceFuelStack(input).isEmpty()) {
            return false;
        }

        return matchesItems(input.items(), requirements(itemInputs, chancedItemInputs, notConsumableItems))
                && matchesFluids(input.fluids(), fluidRequirements(fluidInputs, chancedFluidInputs, notConsumableFluids));
    }

    @Override
    public ItemStack assemble(CERecipeInput input, HolderLookup.Provider registries) {
        return getResultItem(registries).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return itemOutputs.stream()
                .filter(CEChancedItemOutput::guaranteed)
                .findFirst()
                .or(() -> itemOutputs.stream().findFirst())
                .map(CEChancedItemOutput::stack)
                .map(ItemStack::copy)
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        itemInputs.forEach(input -> ingredients.add(input.ingredient()));
        chancedItemInputs.forEach(input -> ingredients.add(input.ingredient().ingredient()));
        notConsumableItems.forEach(input -> ingredients.add(input.ingredient()));
        return ingredients;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(Blocks.FURNACE);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.MACHINE_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeRegistry.MACHINE_RECIPE_TYPE.get();
    }

    private static List<SizedIngredient> requirements(
            List<SizedIngredient> inputs,
            List<CEChancedItemInput> chancedInputs,
            List<SizedIngredient> notConsumables
    ) {
        List<SizedIngredient> requirements = new ArrayList<>(inputs.size() + chancedInputs.size() + notConsumables.size());
        requirements.addAll(inputs);
        chancedInputs.forEach(input -> requirements.add(input.ingredient()));
        requirements.addAll(notConsumables);
        return requirements;
    }

    private static List<SizedFluidIngredient> fluidRequirements(
            List<SizedFluidIngredient> inputs,
            List<CEChancedFluidInput> chancedInputs,
            List<SizedFluidIngredient> notConsumables
    ) {
        List<SizedFluidIngredient> requirements = new ArrayList<>(inputs.size() + chancedInputs.size() + notConsumables.size());
        requirements.addAll(inputs);
        chancedInputs.forEach(input -> requirements.add(input.ingredient()));
        requirements.addAll(notConsumables);
        return requirements;
    }

    private static boolean matchesItems(List<ItemStack> availableStacks, List<SizedIngredient> requirements) {
        List<ItemStack> remaining = availableStacks.stream()
                .map(ItemStack::copy)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        for (SizedIngredient requirement : requirements) {
            int required = requirement.count();
            for (ItemStack stack : remaining) {
                if (!stack.isEmpty() && requirement.ingredient().test(stack)) {
                    int taken = Math.min(required, stack.getCount());
                    stack.shrink(taken);
                    required -= taken;
                    if (required <= 0) {
                        break;
                    }
                }
            }
            if (required > 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesFluids(List<FluidStack> availableStacks, List<SizedFluidIngredient> requirements) {
        List<FluidStack> remaining = availableStacks.stream()
                .map(FluidStack::copy)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        for (SizedFluidIngredient requirement : requirements) {
            int required = requirement.amount();
            for (FluidStack stack : remaining) {
                if (!stack.isEmpty() && requirement.ingredient().test(stack)) {
                    int taken = Math.min(required, stack.getAmount());
                    stack.shrink(taken);
                    required -= taken;
                    if (required <= 0) {
                        break;
                    }
                }
            }
            if (required > 0) {
                return false;
            }
        }
        return true;
    }
}
