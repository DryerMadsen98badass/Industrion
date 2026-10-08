package net.mads.industron.validation.rules;

import net.mads.industron.recipe.recipetypes.assembly.AssemblyComponent;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipetypes.assembly.ComponentDefinition;
import net.mads.industron.recipe.recipes.assembly.ComponentDefinitions;
import net.mads.industron.recipe.recipes.assembly.ToolAssemblyRecipes;
import net.mads.industron.recipe.recipes.assembly.ToolDefinitions;
import net.mads.industron.tool.ToolMaterialRules;
import net.mads.industron.validation.ValidationCode;
import net.mads.industron.validation.ValidationCollector;
import net.mads.industron.validation.ValidationContext;
import net.mads.industron.validation.ValidationRule;
import net.mads.industron.validation.ValidationSubsystem;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/** Validates Assembly component definitions plus the dynamic tool-definition/recipe layer. */
public final class AssemblyDefinitionValidator implements ValidationRule {
    @Override
    public void validate(ValidationContext context, ValidationCollector diagnostics) {
        Set<String> ids = new LinkedHashSet<>();
        Set<AssemblyComponent> known = new HashSet<>();

        for (ComponentDefinition definition : ComponentDefinitions.ALL) {
            AssemblyComponent component = definition.component();
            String subject = "component:" + component.id();

            if (ResourceLocation.tryParse("industron:" + component.id()) == null) {
                diagnostics.error(
                        ValidationSubsystem.COMPONENT,
                        ValidationCode.INVALID_ID,
                        subject,
                        "Component id is not a valid resource path"
                );
            }
            if (!ids.add(component.id())) {
                diagnostics.error(
                        ValidationSubsystem.COMPONENT,
                        ValidationCode.DUPLICATE_ID,
                        subject,
                        "Duplicate component id"
                );
            }
            known.add(component);
        }

        for (ComponentDefinition definition : ComponentDefinitions.ALL) {
            validateSteps(definition, known, diagnostics);
            detectCycles(definition.component(), new LinkedHashSet<>(), diagnostics);
        }

        validateToolDefinitions(diagnostics);
    }


    private static void validateToolDefinitions(ValidationCollector diagnostics) {
        Set<String> ids = new HashSet<>();

        for (var definition : ToolDefinitions.ALL) {
            String subject = "tool:" + definition.id();
            if (ResourceLocation.tryParse("industron:" + definition.id()) == null) {
                diagnostics.error(
                        ValidationSubsystem.ASSEMBLY,
                        ValidationCode.INVALID_ID,
                        subject,
                        "Tool id is not a valid resource path"
                );
            }
            if (!ids.add(definition.id())) {
                diagnostics.error(
                        ValidationSubsystem.ASSEMBLY,
                        ValidationCode.DUPLICATE_ID,
                        subject,
                        "Duplicate tool definition id"
                );
            }

            for (var slot : definition.parts()) {
                if (ToolMaterialRules.candidates(slot.part()).isEmpty()) {
                    diagnostics.error(
                            ValidationSubsystem.ASSEMBLY,
                            ValidationCode.INVALID_REFERENCE,
                            subject,
                            "No valid tool material exposes Material." + slot.part().name()
                                    + " required by role '" + slot.role() + "'"
                    );
                }
            }

            if (definition.isAssembledTool() && definition.isFinishedToolEnabled()) {
                boolean hasRecipe = ToolAssemblyRecipes.ALL.stream()
                        .anyMatch(recipe -> recipe.toolOutput() == definition);
                if (!hasRecipe) {
                    diagnostics.error(
                            ValidationSubsystem.ASSEMBLY,
                            ValidationCode.INVALID_DEFINITION,
                            subject,
                            "Assembled tool has no ToolAssemblyRecipes route"
                    );
                }
            }
        }

        for (var recipe : ToolAssemblyRecipes.ALL) {
            if (!recipe.hasDynamicToolOutput()) {
                diagnostics.error(
                        ValidationSubsystem.ASSEMBLY,
                        ValidationCode.INVALID_DEFINITION,
                        "assembly:" + recipe.id(),
                        "Tool assembly recipe must have a dynamic tool output"
                );
            }
        }
    }

    private static void validateSteps(
            ComponentDefinition definition,
            Set<AssemblyComponent> known,
            ValidationCollector diagnostics
    ) {
        String subject = "component:" + definition.component().id();

        boolean hasRepresentative = definition.steps().stream().anyMatch(step ->
                step.kind() == ComponentDefinition.StepKind.MATERIAL
                        || step.kind() == ComponentDefinition.StepKind.PLANT_PART
                        || step.kind() == ComponentDefinition.StepKind.ITEM
        );
        if (!hasRepresentative) {
            diagnostics.error(
                    ValidationSubsystem.COMPONENT,
                    ValidationCode.INVALID_DEFINITION,
                    subject,
                    "Component needs a direct Material, PlantPart, or exact item input to represent it in JEI"
            );
        }

        for (ComponentDefinition.Step step : definition.steps()) {
            if (!step.relativeRequirements().isEmpty()
                    && (step.materialOverride() == null || !step.materialOverride().isAny())) {
                diagnostics.error(
                        ValidationSubsystem.ASSEMBLY,
                        ValidationCode.INVALID_DEFINITION,
                        subject,
                        "Relative component stat requirements are only valid on inputAny(...) / MaterialType.ANY steps"
                );
            }

            switch (step.kind()) {
                case COMPONENT -> {
                    if (!known.contains(step.component())) {
                        diagnostics.error(
                                ValidationSubsystem.COMPONENT,
                                ValidationCode.INVALID_REFERENCE,
                                subject,
                                "Missing ComponentDefinition for Component." + step.component().id().toUpperCase(java.util.Locale.ROOT)
                        );
                    }
                    validateMaterialOverride(step, subject, diagnostics);
                }
                case TOOL -> {
                    if (!AssemblyTools.hasType(step.tool())) {
                        diagnostics.error(
                                ValidationSubsystem.ASSEMBLY,
                                ValidationCode.INVALID_REFERENCE,
                                subject,
                                "No registered tool item for Tool." + step.tool().id().toUpperCase(java.util.Locale.ROOT)
                        );
                    }
                }
                case MATERIAL -> validateMaterialOverride(step, subject, diagnostics);
                case PLANT_PART, ITEM, WAIT -> {
                    // Plant-part/item availability is validated after registry construction; waits validate in the builder.
                }
            }
        }
    }

    private static void validateMaterialOverride(
            ComponentDefinition.Step step,
            String subject,
            ValidationCollector diagnostics
    ) {
        if (step.materialOverride() == null || !step.materialOverride().isFixed()) return;
        try {
            step.materialOverride().resolve();
        } catch (IllegalStateException exception) {
            diagnostics.error(
                    ValidationSubsystem.ASSEMBLY,
                    ValidationCode.INVALID_REFERENCE,
                    subject,
                    exception.getMessage()
            );
        }
    }

    private static void detectCycles(
            AssemblyComponent component,
            LinkedHashSet<AssemblyComponent> path,
            ValidationCollector diagnostics
    ) {
        if (!path.add(component)) {
            diagnostics.error(
                    ValidationSubsystem.COMPONENT,
                    ValidationCode.COMPONENT_CYCLE,
                    "component:" + component.id(),
                    "Component cycle: " + path.stream().map(AssemblyComponent::id).toList() + " -> " + component.id()
            );
            return;
        }

        ComponentDefinition definition = ComponentDefinitions.find(component);
        if (definition == null) return;

        for (ComponentDefinition.Step step : definition.steps()) {
            if (step.kind() == ComponentDefinition.StepKind.COMPONENT) {
                detectCycles(step.component(), new LinkedHashSet<>(path), diagnostics);
            }
        }
    }
}
