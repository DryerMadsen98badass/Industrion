package net.mads.industron.validation.rules;

import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.defenitions.OrganicMaterials;
import net.mads.industron.material.defenitions.PlantMaterials;
import net.mads.industron.material.organic.OrganicMaterial;
import net.mads.industron.material.plant.PlantMaterial;
import net.mads.industron.material.plant.PlantMaterialGenerator;
import net.mads.industron.material.plant.PlantPart;
import net.mads.industron.material.plant.PlantProcessIntermediate;
import net.mads.industron.material.plant.PlantProcessingPlanner;
import net.mads.industron.validation.ValidationCode;
import net.mads.industron.validation.ValidationCollector;
import net.mads.industron.validation.ValidationContext;
import net.mads.industron.validation.ValidationRule;
import net.mads.industron.validation.ValidationSubsystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Definition-time invariants for the composition-driven plant/organic processing model. */
public final class PlantDefinitionValidator implements ValidationRule {
    @Override
    public void validate(ValidationContext context, ValidationCollector diagnostics) {
        Set<String> materialIds = new HashSet<>();
        Map<ResourceLocation, String> existingOwners = new HashMap<>();
        Set<ResourceLocation> generatedIds = new HashSet<>();

        for (PlantMaterial material : PlantMaterials.ALL) {
            String subject = "plant_material:" + material.id();
            if (ResourceLocation.tryParse("industron:" + material.id()) == null) {
                diagnostics.error(
                        ValidationSubsystem.MATERIAL,
                        ValidationCode.INVALID_ID,
                        subject,
                        "Plant material id is not a valid resource path"
                );
            }
            if (!materialIds.add(material.id())) {
                diagnostics.error(
                        ValidationSubsystem.MATERIAL,
                        ValidationCode.DUPLICATE_ID,
                        subject,
                        "Duplicate plant material id"
                );
            }
            validateComponents(material.components(), subject, diagnostics);
            if (material.components().isEmpty()) {
                diagnostics.error(
                        ValidationSubsystem.COMPOSITION,
                        ValidationCode.INVALID_DEFINITION,
                        subject,
                        "Every PlantMaterial must declare .contains(...)"
                );
            }

            material.existingParts().forEach((part, id) -> {
                if ("minecraft".equals(id.getNamespace()) && BuiltInRegistries.ITEM.getOptional(id).isEmpty()) {
                    diagnostics.error(
                            ValidationSubsystem.MATERIAL,
                            ValidationCode.INVALID_DEFINITION,
                            subject,
                            "Existing vanilla plant form is not an inventory item: " + id
                    );
                }
                String previous = existingOwners.putIfAbsent(id, material.id() + "/" + part.name());
                if (previous != null) {
                    diagnostics.error(
                            ValidationSubsystem.MATERIAL,
                            ValidationCode.DUPLICATE_ID,
                            subject,
                            "Existing plant item/block " + id + " is already owned by plant mapping " + previous
                    );
                }
            });

            for (PlantPart part : PlantMaterialGenerator.generatedItemForms(material)) {
                ResourceLocation generated = ResourceLocation.fromNamespaceAndPath(
                        "industron",
                        part.registryName(material)
                );
                if (!generatedIds.add(generated)) {
                    diagnostics.error(
                            ValidationSubsystem.MATERIAL,
                            ValidationCode.DUPLICATE_ID,
                            subject,
                            "Duplicate generated plant item id " + generated
                    );
                }
                try {
                    validateComponents(
                            PlantMaterialGenerator.substanceFor(material, part).components(),
                            subject + "/" + part.name().toLowerCase(),
                            diagnostics
                    );
                } catch (RuntimeException exception) {
                    diagnostics.error(
                            ValidationSubsystem.MATERIAL,
                            ValidationCode.INVALID_DEFINITION,
                            subject,
                            exception.getClass().getSimpleName() + ": " + safeMessage(exception)
                    );
                }
            }

            try {
                for (PlantProcessIntermediate intermediate : PlantProcessingPlanner.requiredIntermediates(material)) {
                    ResourceLocation generated = intermediate.registryId();
                    if (!generatedIds.add(generated)) {
                        diagnostics.error(
                                ValidationSubsystem.MATERIAL,
                                ValidationCode.DUPLICATE_ID,
                                subject,
                                "Duplicate generated plant intermediate id " + generated
                        );
                    }
                    validateComponents(
                            intermediate.components(),
                            "plant_intermediate:" + intermediate.id(),
                            diagnostics
                    );
                }
            } catch (RuntimeException exception) {
                diagnostics.error(
                        ValidationSubsystem.RECIPE,
                        ValidationCode.INVALID_DEFINITION,
                        subject,
                        "Plant route planning failed: " + exception.getClass().getSimpleName()
                                + ": " + safeMessage(exception)
                );
            }

            validateGeneratedRouteConservation(material, subject, diagnostics);
        }

        for (OrganicMaterial organic : OrganicMaterials.ALL) {
            String subject = "organic_material:" + organic.id();
            validateComponents(organic.components(), subject, diagnostics);
            if (organic.components().isEmpty()) {
                diagnostics.error(
                        ValidationSubsystem.COMPOSITION,
                        ValidationCode.INVALID_DEFINITION,
                        subject,
                        "Every OrganicMaterial must declare .contains(...)"
                );
            }
        }
    }

    private static void validateGeneratedRouteConservation(
            PlantMaterial material,
            String subject,
            ValidationCollector diagnostics
    ) {
        if (!PlantMaterialGenerator.hasFiberFraction(material)) return;
        try {
            PlantProcessingPlanner.FiberBatch batch = PlantProcessingPlanner.fiberBatch(material);
            int declared = PlantMaterialGenerator.totalCompositionAmount(material);
            if (batch.inputCount() != declared
                    || batch.inputCount() != batch.fiberCount() + batch.residueCount()) {
                diagnostics.error(
                        ValidationSubsystem.RECIPE,
                        ValidationCode.INVALID_DEFINITION,
                        subject,
                        "Generated fibre route is not unit-conserving: "
                                + batch.inputCount() + " input vs "
                                + (batch.fiberCount() + batch.residueCount()) + " output"
                );
            }

            int residueComposition = material.components().stream()
                    .filter(component -> !component.substance().id().equals(
                            net.mads.industron.material.defenitions.CompoundMaterials.SYLVARA.id()
                    ))
                    .mapToInt(MaterialComponent::amount)
                    .sum();
            if (residueComposition != batch.residueCount()) {
                diagnostics.error(
                        ValidationSubsystem.COMPOSITION,
                        ValidationCode.INVALID_DEFINITION,
                        subject,
                        "Generated plant residue composition does not match separation count: "
                                + residueComposition + " vs " + batch.residueCount()
                );
            }
        } catch (RuntimeException exception) {
            diagnostics.error(
                    ValidationSubsystem.RECIPE,
                    ValidationCode.INVALID_DEFINITION,
                    subject,
                    "Plant conservation validation failed: "
                            + exception.getClass().getSimpleName() + ": " + safeMessage(exception)
            );
        }
    }

    private static void validateComponents(
            java.util.List<MaterialComponent> components,
            String subject,
            ValidationCollector diagnostics
    ) {
        for (MaterialComponent component : components) {
            if (component == null || component.substance() == null || component.amount() <= 0) {
                diagnostics.error(
                        ValidationSubsystem.COMPOSITION,
                        ValidationCode.INVALID_DEFINITION,
                        subject,
                        "Composition entries must have a substance and a positive amount"
                );
            }
        }
    }

    private static String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? "no message" : exception.getMessage();
    }
}
