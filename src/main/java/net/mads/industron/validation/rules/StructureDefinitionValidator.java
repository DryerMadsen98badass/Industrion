package net.mads.industron.validation.rules;

import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureModel;
import net.mads.industron.material.structure.StructureSetResolver;
import net.mads.industron.validation.ValidationCode;
import net.mads.industron.validation.ValidationCollector;
import net.mads.industron.validation.ValidationContext;
import net.mads.industron.validation.ValidationRule;
import net.mads.industron.validation.ValidationSubsystem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Validates structure-set source resources and the block ids generated from them. */
public final class StructureDefinitionValidator implements ValidationRule {
    @Override
    public void validate(ValidationContext context, ValidationCollector diagnostics) {
        Set<String> materialIds = new HashSet<>();
        Set<String> generatedBlockIds = new HashSet<>();

        for (StructureMaterial material : context.structureMaterials()) {
            String subject = "structure_material:" + material.id();
            if (!materialIds.add(material.id())) {
                diagnostics.error(
                        ValidationSubsystem.STRUCTURE,
                        ValidationCode.DUPLICATE_ID,
                        subject,
                        "Duplicate structure material id"
                );
            }

            List<String> sourceTextures = StructureSetResolver.textureFiles(material.model());
            if (sourceTextures.isEmpty()) {
                diagnostics.error(
                        ValidationSubsystem.STRUCTURE,
                        ValidationCode.MISSING_RESOURCE,
                        subject,
                        "No source textures found for structure model " + material.model().category()
                                + "/" + material.model().id()
                );
                continue;
            }

            List<StructureBlockDefinition> definitions;
            try {
                definitions = StructureMaterialGenerator.blockDefinitions(material);
            } catch (RuntimeException exception) {
                diagnostics.error(
                        ValidationSubsystem.STRUCTURE,
                        classify(exception),
                        subject,
                        exception.getClass().getSimpleName() + ": " + safeMessage(exception)
                );
                continue;
            }

            for (StructureBlockDefinition definition : definitions) {
                if (definition.part().isPresent() && material.hasExistingPart(definition.part().get())) {
                    continue;
                }
                String blockSubject = "structure_block:" + definition.registryName();
                if (!generatedBlockIds.add(definition.registryName())) {
                    diagnostics.error(
                            ValidationSubsystem.STRUCTURE,
                            ValidationCode.DUPLICATE_ID,
                            blockSubject,
                            "Duplicate generated structure block id"
                    );
                }
                validateTexture(material.model(), definition.textureFile(), blockSubject, diagnostics);
                definition.topTextureFile().ifPresent(texture ->
                        validateTexture(material.model(), texture, blockSubject, diagnostics));
                definition.bottomTextureFile().ifPresent(texture ->
                        validateTexture(material.model(), texture, blockSubject, diagnostics));
                definition.itemTextureFile().ifPresent(texture ->
                        validateTexture(material.model(), texture, blockSubject, diagnostics));
                definition.textureFiles().values().forEach(texture ->
                        validateTexture(material.model(), texture, blockSubject, diagnostics));
            }
        }
    }

    private static void validateTexture(
            StructureModel model,
            String texture,
            String subject,
            ValidationCollector diagnostics
    ) {
        if (StructureSetResolver.sourceTemplate(model, texture).isEmpty()) {
            diagnostics.error(
                    ValidationSubsystem.STRUCTURE,
                    ValidationCode.MISSING_RESOURCE,
                    subject,
                    "Missing structure source texture " + texture
            );
        }
    }

    private static ValidationCode classify(RuntimeException exception) {
        String message = safeMessage(exception).toLowerCase();
        if (message.contains("texture") || message.contains("resource")) {
            return ValidationCode.MISSING_RESOURCE;
        }
        if (message.contains("duplicate")) {
            return ValidationCode.DUPLICATE_ID;
        }
        return ValidationCode.INVALID_DEFINITION;
    }

    private static String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? "no message" : exception.getMessage();
    }
}
