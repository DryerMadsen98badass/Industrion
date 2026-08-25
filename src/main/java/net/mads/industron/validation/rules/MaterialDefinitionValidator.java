package net.mads.industron.validation.rules;

import net.mads.industron.material.ElementDefinition;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialProperties;
import net.mads.industron.material.MaterialPropertyCalculator;
import net.mads.industron.validation.ValidationCode;
import net.mads.industron.validation.ValidationCollector;
import net.mads.industron.validation.ValidationContext;
import net.mads.industron.validation.ValidationGraph;
import net.mads.industron.validation.ValidationRule;
import net.mads.industron.validation.ValidationSubsystem;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Definition, composition and material-template validation for the current material registry. */
public final class MaterialDefinitionValidator implements ValidationRule {
    @Override
    public void validate(ValidationContext context, ValidationCollector diagnostics) {
        validateElements(context, diagnostics);
        validateMaterials(context, diagnostics);
        validateCompositionCycles(context, diagnostics);
    }

    private static void validateElements(ValidationContext context, ValidationCollector diagnostics) {
        Set<String> ids = new HashSet<>();
        Set<String> symbols = new HashSet<>();
        Set<Integer> atomicNumbers = new HashSet<>();

        for (ElementDefinition element : context.elements()) {
            String subject = "element:" + element.id();
            if (!validPath(element.id())) {
                diagnostics.error(
                        ValidationSubsystem.MATERIAL,
                        ValidationCode.INVALID_ID,
                        subject,
                        "Element id is not a valid resource path"
                );
            }
            if (!ids.add(element.id())) {
                diagnostics.error(
                        ValidationSubsystem.MATERIAL,
                        ValidationCode.DUPLICATE_ID,
                        subject,
                        "Duplicate element id"
                );
            }
            if (!symbols.add(element.symbol())) {
                diagnostics.error(
                        ValidationSubsystem.MATERIAL,
                        ValidationCode.DUPLICATE_SYMBOL,
                        subject,
                        "Duplicate element symbol " + element.symbol()
                );
            }
            if (!atomicNumbers.add(element.atomicNumber())) {
                diagnostics.error(
                        ValidationSubsystem.MATERIAL,
                        ValidationCode.DUPLICATE_ATOMIC_NUMBER,
                        subject,
                        "Duplicate atomic number " + element.atomicNumber()
                );
            }
        }

        for (int atomicNumber = 1; atomicNumber <= 100; atomicNumber++) {
            if (!atomicNumbers.contains(atomicNumber)) {
                diagnostics.error(
                        ValidationSubsystem.MATERIAL,
                        ValidationCode.INVALID_DEFINITION,
                        "atomic_number:" + atomicNumber,
                        "Baseline element set must contain every atomic number from 1 through 100"
                );
            }
        }
    }

    private static void validateMaterials(ValidationContext context, ValidationCollector diagnostics) {
        Set<String> ids = new HashSet<>();
        Map<String, ElementDefinition> elementsById = new HashMap<>();
        for (ElementDefinition element : context.elements()) {
            elementsById.putIfAbsent(element.id(), element);
        }

        for (IndustrialMaterial material : context.materials()) {
            String subject = "material:" + material.id();
            if (!validPath(material.id())) {
                diagnostics.error(
                        ValidationSubsystem.MATERIAL,
                        ValidationCode.INVALID_ID,
                        subject,
                        "Material id is not a valid resource path"
                );
            }
            if (!ids.add(material.id())) {
                diagnostics.error(
                        ValidationSubsystem.MATERIAL,
                        ValidationCode.DUPLICATE_ID,
                        subject,
                        "Duplicate material id"
                );
            }
            if (material.properties().metal() && material.properties().gemCandidate()) {
                diagnostics.error(
                        ValidationSubsystem.MATERIAL,
                        ValidationCode.INVALID_CLASSIFICATION,
                        subject,
                        "A material cannot be both metal and gem"
                );
            }
            validateElectricalBehavior(material, subject, diagnostics);
            if (!material.parts().containsAll(material.existingParts().keySet())) {
                diagnostics.error(
                        ValidationSubsystem.MATERIAL,
                        ValidationCode.INVALID_REFERENCE,
                        subject,
                        "existingParts contains a part that is missing from the material's part set"
                );
            }

            ElementDefinition element = elementsById.get(material.id());
            if (element != null) {
                if (material.atomicNumber() != element.atomicNumber()) {
                    diagnostics.error(
                            ValidationSubsystem.MATERIAL,
                            ValidationCode.INCONSISTENT_DERIVED_DATA,
                            subject,
                            "Material atomic number does not match its element definition"
                    );
                }
                if (!material.properties().equals(MaterialPropertyCalculator.calculate(element))) {
                    diagnostics.error(
                            ValidationSubsystem.MATERIAL,
                            ValidationCode.INCONSISTENT_DERIVED_DATA,
                            subject,
                            "Stored material properties do not match MaterialPropertyCalculator"
                    );
                }
            }
        }
    }

    private static void validateCompositionCycles(ValidationContext context, ValidationCollector diagnostics) {
        List<IndustrialSubstance> knownSubstances = new java.util.ArrayList<>();
        knownSubstances.addAll(context.elements());
        knownSubstances.addAll(context.materials());

        ValidationGraph.detectUnknownReferences(
                knownSubstances,
                substance -> substance.getClass().getSimpleName() + ":" + substance.id(),
                MaterialDefinitionValidator::dependencies,
                ValidationSubsystem.COMPOSITION,
                ValidationCode.INVALID_REFERENCE,
                diagnostics
        );
        ValidationGraph.detectCycles(
                knownSubstances,
                substance -> substance.getClass().getSimpleName() + ":" + substance.id(),
                MaterialDefinitionValidator::dependencies,
                ValidationSubsystem.COMPOSITION,
                ValidationCode.COMPOSITION_CYCLE,
                diagnostics
        );
    }

    private static List<IndustrialSubstance> dependencies(IndustrialSubstance substance) {
        if (!(substance instanceof IndustrialMaterial material)) return List.of();
        return material.components().stream()
                .map(MaterialComponent::substance)
                .toList();
    }

    private static void validateElectricalBehavior(
            IndustrialMaterial material,
            String subject,
            ValidationCollector diagnostics
    ) {
        MaterialProperties properties = material.properties();
        boolean expectedConductor = properties.metal();
        boolean actualConductor = properties.electricalBehavior() == MaterialProperties.ElectricalBehavior.CONDUCTOR;

        if (expectedConductor != actualConductor) {
            diagnostics.error(
                    ValidationSubsystem.MATERIAL,
                    ValidationCode.INVALID_ELECTRICAL_BEHAVIOR,
                    subject,
                    "Electrical behavior must be CONDUCTOR exactly when metal=true"
            );
        }
        if (actualConductor) {
            if (properties.electricalConductivity() <= 0 || properties.insulationStrength() != 0) {
                diagnostics.error(
                        ValidationSubsystem.MATERIAL,
                        ValidationCode.INVALID_ELECTRICAL_BEHAVIOR,
                        subject,
                        "Conductors require electricalConductivity > 0 and insulationStrength = 0"
                );
            }
        } else {
            if (properties.electricalConductivity() != 0 || properties.insulationStrength() <= 0) {
                diagnostics.error(
                        ValidationSubsystem.MATERIAL,
                        ValidationCode.INVALID_ELECTRICAL_BEHAVIOR,
                        subject,
                        "Insulators require electricalConductivity = 0 and insulationStrength > 0"
                );
            }
            if (MaterialPropertyCalculator.wireBaseAmps(properties) != 0) {
                diagnostics.error(
                        ValidationSubsystem.MATERIAL,
                        ValidationCode.INVALID_ELECTRICAL_BEHAVIOR,
                        subject,
                        "Insulating materials must have zero bare-wire amp capacity"
                );
            }
        }
    }

    private static boolean validPath(String path) {
        return path != null
                && !path.isBlank()
                && ResourceLocation.tryParse("industron:" + path) != null;
    }
}
