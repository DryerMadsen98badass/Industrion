package net.mads.industron.recipe;

import net.mads.industron.Industron;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.interaction.BlockInteraction;
import net.mads.industron.machine.interaction.MachineCondition;
import net.mads.industron.machine.interaction.MachineModifier;
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
    private final List<CEChancedItemOutput> itemOutputs = new ArrayList<>();
    private final List<FluidStack> fluidOutputs = new ArrayList<>();
    private final List<CEChancedFluidOutput> chancedFluidOutputs = new ArrayList<>();
    private Optional<ResourceLocation> treeSource = Optional.empty();
    private int duration = 100;
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

        /**
         * Adds a chanced item output. Tier bonus is added once per runtime tier
         * above the recipe baseline and may be negative.
         */
        static Option chancedOutputItem(String itemId, int count, int chance, int tierBonus) {
            return definition -> definition.itemOutputs.add(
                    new CEChancedItemOutput(new ItemStack(item(itemId), count), chance, tierBonus)
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
            return definition -> definition.duration = duration;
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
                itemOutputs,
                fluidOutputs,
                chancedFluidOutputs,
                treeSource,
                duration,
                circuit,
                tier,
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

    public void save(RecipeOutput output) {
        validateIdentity();
        output.accept(
                ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, type.id().getPath() + "/" + id),
                build(),
                null
        );
    }

    private void validate() {
        validateIdentity();

        if (duration <= 0) {
            throw new IllegalStateException("Recipe " + id + " must have a positive duration");
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

        validateCommonRecipeRules();
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
