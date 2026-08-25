package net.mads.industron.validation.rules;

import net.mads.industron.material.ElementDefinition;
import net.mads.industron.material.MaterialFormGenerator;
import net.mads.industron.material.MaterialProperties;
import net.mads.industron.material.MaterialPropertyCalculator;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureSetResolver;
import net.mads.industron.validation.ValidationCode;
import net.mads.industron.validation.ValidationCollector;
import net.mads.industron.validation.ValidationContext;
import net.mads.industron.validation.ValidationRule;
import net.mads.industron.validation.ValidationSubsystem;

import java.util.List;

/** Checks deterministic pure calculations and stable generator ordering. */
public final class DeterminismValidator implements ValidationRule {
    @Override
    public void validate(ValidationContext context, ValidationCollector diagnostics) {
        for (ElementDefinition element : context.elements()) {
            MaterialProperties first = MaterialPropertyCalculator.calculate(element);
            MaterialProperties second = MaterialPropertyCalculator.calculate(element);
            if (!first.equals(second)) {
                nondeterministic("element:" + element.id(), "Material properties changed between identical calculations", diagnostics);
            }
            if (!MaterialFormGenerator.partsFor(first).equals(MaterialFormGenerator.partsFor(second))) {
                nondeterministic("element:" + element.id(), "Generated material forms changed between identical calculations", diagnostics);
            }
        }

        for (StructureMaterial material : context.structureMaterials()) {
            List<String> firstTextures = StructureSetResolver.textureFiles(material.model());
            List<String> secondTextures = StructureSetResolver.textureFiles(material.model());
            if (!firstTextures.equals(secondTextures)) {
                nondeterministic(
                        "structure_material:" + material.id(),
                        "Structure texture discovery order changed between identical scans",
                        diagnostics
                );
            }

            try {
                List<String> firstIds = StructureMaterialGenerator.blockDefinitions(material).stream()
                        .map(StructureBlockDefinition::registryName)
                        .toList();
                List<String> secondIds = StructureMaterialGenerator.blockDefinitions(material).stream()
                        .map(StructureBlockDefinition::registryName)
                        .toList();
                if (!firstIds.equals(secondIds)) {
                    nondeterministic(
                            "structure_material:" + material.id(),
                            "Structure block ids changed between identical generation passes",
                            diagnostics
                    );
                }
            } catch (RuntimeException ignored) {
                // StructureDefinitionValidator reports the concrete generator/resource failure.
            }
        }
    }

    private static void nondeterministic(
            String subject,
            String message,
            ValidationCollector diagnostics
    ) {
        diagnostics.error(
                ValidationSubsystem.FOUNDATION,
                ValidationCode.NON_DETERMINISTIC_OUTPUT,
                subject,
                message
        );
    }
}
