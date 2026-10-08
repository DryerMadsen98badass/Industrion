package net.mads.industron.recipe;

import net.mads.industron.Industron;
import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Declarative definition of one CE process type.
 *
 * <p>The definition describes recipe IO, supported custom logic and display
 * information. Power is a property of the machine, not the recipe type.</p>
 */
public record RecipeTypeDefinition(
        ResourceLocation id,
        String displayName,
        int maxItemInputs,
        int maxItemOutputs,
        int maxFluidInputs,
        int maxFluidOutputs,
        List<ResourceLocation> supportedLogic,
        ProgressBar progressBar,
        int baseBlockItemInputIndex,
        boolean requiresCoilTemperature,
        boolean ignoresTier,
        Optional<ResourceLocation> generatedRecipeType,
        Optional<String> jeiToolIcon,
        boolean dedicatedToolSlot
) {
    public RecipeTypeDefinition {
        supportedLogic = List.copyOf(supportedLogic);
        progressBar = Objects.requireNonNullElse(progressBar, ProgressBar.ARROW);
        generatedRecipeType = generatedRecipeType == null ? Optional.empty() : generatedRecipeType;
        jeiToolIcon = jeiToolIcon == null ? Optional.empty() : jeiToolIcon;
    }

    public boolean supportsLogic(ResourceLocation logicId) {
        return supportedLogic.contains(logicId);
    }

    public static Builder recipeType() {
        return new Builder();
    }

    public static ResourceLocation id(String id) {
        return id.contains(":")
                ? ResourceLocation.parse(id)
                : ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, id);
    }

    /**
     * Options shown by autocomplete inside {@code .recipeTypeDefinition(Option...)}.
     */
    @FunctionalInterface
    public interface Option {
        void apply(Builder builder);

        /** Defines the registry path of the recipe type. */
        static Option id(String id) {
            return builder -> builder.id = RecipeTypeDefinition.id(id);
        }

        /** Defines the user-facing name shown in JEI and machine interfaces. */
        static Option displayName(String displayName) {
            return builder -> builder.displayName = displayName;
        }

        /** Defines maximum item and fluid inputs and outputs, in that order. */
        static Option maxIO(int itemInputs, int itemOutputs, int fluidInputs, int fluidOutputs) {
            return builder -> {
                builder.maxItemInputs = itemInputs;
                builder.maxItemOutputs = itemOutputs;
                builder.maxFluidInputs = fluidInputs;
                builder.maxFluidOutputs = fluidOutputs;
            };
        }

        /** Adds a supported custom recipe-logic capability. */
        static Option logic(CERecipeLogicDefinition logic) {
            return builder -> builder.supportedLogic.add(Objects.requireNonNull(logic).id());
        }

        /**
         * Selects the default progress bar used by JEI and machines using this recipe type.
         * A machine definition may override it for its own GUI.
         */
        static Option progressBar(ProgressBar progressBar) {
            return builder -> builder.progressBar = Objects.requireNonNull(progressBar);
        }

        /** Marks the first item input slot as the recipe's base block in automatic recipe viewers. */
        static Option baseBlockInput() {
            return builder -> builder.baseBlockItemInputIndex = 0;
        }

        /**
         * Requires every recipe of this type to declare a positive coil temperature and
         * the coil-temperature runtime logic. Used only by processes that actually melt solids.
         */
        static Option requiresCoilTemperature() {
            return builder -> builder.requiresCoilTemperature = true;
        }

        /**
         * Marks a manual/passive recipe type as tierless. Any tier declared by a recipe
         * is accepted for compatibility but discarded before runtime and serialization.
         */
        static Option ignoreTier() {
            return builder -> builder.ignoresTier = true;
        }

        /** Uses a registered dynamic tool family as the JEI category icon and catalyst. */
        static Option jeiToolIcon(ToolDefinition tool) {
            return builder -> builder.jeiToolIcon = Optional.of(Objects.requireNonNull(tool).id());
        }

        /** Shows the recipe's declared tool in a dedicated JEI catalyst slot. */
        static Option dedicatedToolSlot() {
            return builder -> builder.dedicatedToolSlot = true;
        }

        /** Also emits every recipe into an automated parent process, without manual tool actions. */
        static Option generateFor(RecipeTypeDefinition automatedType) {
            return builder -> builder.generatedRecipeType = Optional.of(
                    Objects.requireNonNull(automatedType).id()
            );
        }
    }

    public static final class Builder {
        private ResourceLocation id;
        private String displayName;
        private int maxItemInputs;
        private int maxItemOutputs;
        private int maxFluidInputs;
        private int maxFluidOutputs;
        private final List<ResourceLocation> supportedLogic = new ArrayList<>();
        private ProgressBar progressBar = ProgressBar.ARROW;
        private int baseBlockItemInputIndex = -1;
        private boolean requiresCoilTemperature;
        private boolean ignoresTier;
        private Optional<ResourceLocation> generatedRecipeType = Optional.empty();
        private Optional<String> jeiToolIcon = Optional.empty();
        private boolean dedicatedToolSlot;

        private Builder() {
        }

        public Builder recipeTypeDefinition(Option option) {
            Objects.requireNonNull(option, "Recipe type option").apply(this);
            return this;
        }

        public RecipeTypeDefinition build() {
            if (id == null) {
                throw new IllegalStateException("Recipe type is missing Option.id(...)");
            }
            if (displayName == null || displayName.isBlank()) {
                displayName = id.getPath();
            }
            if (maxItemInputs < 0 || maxItemOutputs < 0 || maxFluidInputs < 0 || maxFluidOutputs < 0) {
                throw new IllegalStateException("Recipe type " + id + " cannot have negative IO limits");
            }
            if (requiresCoilTemperature && !supportedLogic.contains(CERecipeLogics.COIL_TEMP.id())) {
                throw new IllegalStateException(
                        "Recipe type " + id + " requires coil temperature but does not support "
                                + CERecipeLogics.COIL_TEMP.id()
                );
            }
            return new RecipeTypeDefinition(
                    id,
                    displayName,
                    maxItemInputs,
                    maxItemOutputs,
                    maxFluidInputs,
                    maxFluidOutputs,
                    supportedLogic,
                    progressBar,
                    baseBlockItemInputIndex,
                    requiresCoilTemperature,
                    ignoresTier,
                    generatedRecipeType,
                    jeiToolIcon,
                    dedicatedToolSlot
            );
        }
    }
}
