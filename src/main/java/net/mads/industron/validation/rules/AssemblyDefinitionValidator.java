package net.mads.industron.validation.rules;

import net.mads.industron.recipe.recipetypes.assembly.AssemblyComponent;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipetypes.assembly.ComponentDefinition;
import net.mads.industron.recipe.recipes.assembly.ComponentDefinitions;
import net.mads.industron.validation.ValidationCode;
import net.mads.industron.validation.ValidationCollector;
import net.mads.industron.validation.ValidationContext;
import net.mads.industron.validation.ValidationRule;
import net.mads.industron.validation.ValidationSubsystem;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/** Validates the structure-only ComponentDefinitions graph. */
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
    }

    private static void validateSteps(
            ComponentDefinition definition,
            Set<AssemblyComponent> known,
            ValidationCollector diagnostics
    ) {
        String subject = "component:" + definition.component().id();

        boolean hasRepresentative = definition.steps().stream().anyMatch(step ->
                step.kind() == ComponentDefinition.StepKind.MATERIAL
                        || step.kind() == ComponentDefinition.StepKind.ITEM
        );
        if (!hasRepresentative) {
            diagnostics.error(
                    ValidationSubsystem.COMPONENT,
                    ValidationCode.INVALID_DEFINITION,
                    subject,
                    "Component needs a direct Material or exact item input to represent it in JEI"
            );
        }

        for (ComponentDefinition.Step step : definition.steps()) {
            if (!step.relativeRequirements().isEmpty()
                    && (step.metalOverride() == null || !step.metalOverride().isAny())) {
                diagnostics.error(
                        ValidationSubsystem.ASSEMBLY,
                        ValidationCode.INVALID_DEFINITION,
                        subject,
                        "Relative component stat requirements are only valid on inputAny(...) / Metal.ANY steps"
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
                    validateMetalOverride(step, subject, diagnostics);
                }
                case TOOL -> {
                    if (AssemblyTools.all().stream().noneMatch(tool -> tool.type().equals(step.tool()))) {
                        diagnostics.error(
                                ValidationSubsystem.ASSEMBLY,
                                ValidationCode.INVALID_REFERENCE,
                                subject,
                                "No registered tool item for Tool." + step.tool().id().toUpperCase(java.util.Locale.ROOT)
                        );
                    }
                }
                case MATERIAL -> validateMetalOverride(step, subject, diagnostics);
                case ITEM, WAIT -> {
                    // Registry ids are validated after registry construction; waits validate in the builder.
                }
            }
        }
    }

    private static void validateMetalOverride(
            ComponentDefinition.Step step,
            String subject,
            ValidationCollector diagnostics
    ) {
        if (step.metalOverride() == null || step.metalOverride().isAny()) return;
        try {
            step.metalOverride().resolve();
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
