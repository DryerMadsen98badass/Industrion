package net.mads.industron.validation.rules;

import net.mads.industron.Industron;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialOreHost;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.recipes.CasingDefinition;
import net.mads.industron.material.recipes.MaterialCasingGenerator;
import net.mads.industron.material.recipes.MaterialCasingRecipes;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.validation.ValidationCode;
import net.mads.industron.validation.ValidationCollector;
import net.mads.industron.validation.ValidationContext;
import net.mads.industron.validation.ValidationRule;
import net.mads.industron.validation.ValidationSubsystem;
import net.minecraft.resources.ResourceLocation;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Complete Phase-04 validation for material-derived casing definitions and generated identity. */
public final class MaterialCasingDefinitionValidator implements ValidationRule {
    @Override
    public void validate(ValidationContext context, ValidationCollector diagnostics) {
        List<CasingDefinition> definitions = MaterialCasingRecipes.ALL;
        validateDefinitions(definitions, diagnostics);

        List<MaterialCasingGenerator.GeneratedCasing> first;
        List<MaterialCasingGenerator.GeneratedCasing> second;
        try {
            first = MaterialCasingGenerator.generate(definitions, context.materials());
            second = MaterialCasingGenerator.generate(definitions, context.materials());
        } catch (RuntimeException exception) {
            diagnostics.error(
                    ValidationSubsystem.CASING,
                    ValidationCode.INVALID_DEFINITION,
                    "material_casings",
                    exception.getClass().getSimpleName() + ": " + safeMessage(exception)
            );
            return;
        }

        validateDeterminism(first, second, diagnostics);
        validateGeneratedCasings(first, definitions, context, diagnostics);
    }

    private static void validateDefinitions(
            List<CasingDefinition> definitions,
            ValidationCollector diagnostics
    ) {
        Set<String> ids = new HashSet<>();
        Set<String> displayNames = new HashSet<>();

        for (CasingDefinition definition : definitions) {
            String subject = "casing_definition:" + definition.id();
            if (ResourceLocation.tryParse(Industron.MOD_ID + ":" + definition.id()) == null) {
                error(diagnostics, ValidationCode.INVALID_ID, subject, "Casing id is not a valid resource path");
            }
            if (!ids.add(definition.id())) {
                error(diagnostics, ValidationCode.DUPLICATE_ID, subject, "Duplicate casing definition id");
            }
            if (definition.displayName().isBlank()) {
                error(diagnostics, ValidationCode.INVALID_DEFINITION, subject, "Casing display name cannot be blank");
            } else if (!displayNames.add(definition.displayName())) {
                error(diagnostics, ValidationCode.DUPLICATE_ID, subject, "Duplicate casing display name");
            }
            if (!MachineTier.ALL.contains(definition.startTier())) {
                error(diagnostics, ValidationCode.INVALID_DEFINITION, subject, "Casing start tier must be electric");
            }
            if (definition.materialRequirements().isEmpty()) {
                error(diagnostics, ValidationCode.INVALID_DEFINITION, subject, "Casing needs material requirements");
            }
            if (definition.materialRequirements().stream().anyMatch(requirement ->
                    requirement.capability() != null && requirement.capability().isPracticalStat())) {
                error(diagnostics, ValidationCode.INVALID_DEFINITION, subject,
                        "Casing-level material requirements cannot use part-specific practical stats");
            }
            if (!definition.baseBlockInput().isBlock()) {
                error(diagnostics, ValidationCode.INVALID_DEFINITION, subject, "Casing base input must be a block form");
            }
            if (definition.inputs().isEmpty()) {
                error(diagnostics, ValidationCode.INVALID_DEFINITION, subject, "Casing needs at least one assembly input");
            }
            validateTexture(definition.texture(), subject, diagnostics);
        }
    }

    private static void validateTexture(
            ResourceLocation texture,
            String subject,
            ValidationCollector diagnostics
    ) {
        String resourcePath = "assets/" + texture.getNamespace() + "/textures/" + texture.getPath() + ".png";
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        URL resource = contextLoader == null ? null : contextLoader.getResource(resourcePath);
        if (resource == null) {
            resource = MaterialCasingDefinitionValidator.class.getClassLoader().getResource(resourcePath);
        }
        if (resource == null) {
            error(diagnostics, ValidationCode.MISSING_RESOURCE, subject,
                    "Missing casing texture " + texture + " (expected " + resourcePath + ")");
        }
    }

    private static void validateDeterminism(
            List<MaterialCasingGenerator.GeneratedCasing> first,
            List<MaterialCasingGenerator.GeneratedCasing> second,
            ValidationCollector diagnostics
    ) {
        List<String> firstSignature = signature(first);
        List<String> secondSignature = signature(second);
        if (!firstSignature.equals(secondSignature)) {
            error(diagnostics, ValidationCode.NON_DETERMINISTIC_OUTPUT, "material_casings",
                    "Identical casing resolution passes produced different ordered output");
        }
    }

    private static List<String> signature(List<MaterialCasingGenerator.GeneratedCasing> generated) {
        return generated.stream().map(casing ->
                casing.registryName() + "|" + casing.displayName() + "|" + casing.tier().id()
                        + "|" + casing.materialRequirements()).toList();
    }

    private static void validateGeneratedCasings(
            List<MaterialCasingGenerator.GeneratedCasing> generated,
            List<CasingDefinition> definitions,
            ValidationContext context,
            ValidationCollector diagnostics
    ) {
        Set<String> ids = new HashSet<>();
        Map<String, Integer> variantsPerDefinition = new HashMap<>();
        Map<String, String> reservedBlockIds = reservedBlockIds(context);

        for (MaterialCasingGenerator.GeneratedCasing casing : generated) {
            String id = casing.registryName();
            String subject = "generated_casing:" + id;
            String expectedId = casing.material().id() + "_" + casing.definition().id();
            String expectedName = casing.material().displayName() + " " + casing.definition().displayName();

            if (!id.equals(expectedId)) {
                error(diagnostics, ValidationCode.INCONSISTENT_DERIVED_DATA, subject,
                        "Generated casing id is not derived from material id + casing id");
            }
            if (!casing.displayName().equals(expectedName)) {
                error(diagnostics, ValidationCode.INCONSISTENT_DERIVED_DATA, subject,
                        "Generated casing display name must contain material name + casing name");
            }
            if (ResourceLocation.tryParse(Industron.MOD_ID + ":" + id) == null) {
                error(diagnostics, ValidationCode.INVALID_ID, subject, "Generated casing id is invalid");
            }
            if (!ids.add(id)) {
                error(diagnostics, ValidationCode.DUPLICATE_ID, subject, "Duplicate generated casing id");
            }
            String owner = reservedBlockIds.get(id);
            if (owner != null) {
                error(diagnostics, ValidationCode.DUPLICATE_ID, subject,
                        "Generated casing block id collides with " + owner);
            }
            if (!casing.material().has(casing.definition().baseBlockInput())) {
                error(diagnostics, ValidationCode.INVALID_REFERENCE, subject,
                        "Qualified casing material is missing its required base block form");
            }
            variantsPerDefinition.merge(casing.definition().id(), 1, Integer::sum);
        }

        for (CasingDefinition definition : definitions) {
            if (variantsPerDefinition.getOrDefault(definition.id(), 0) == 0) {
                error(diagnostics, ValidationCode.INVALID_DEFINITION,
                        "casing_definition:" + definition.id(),
                        "Casing definition does not produce any material variants");
            }
        }
    }

    private static Map<String, String> reservedBlockIds(ValidationContext context) {
        Map<String, String> result = new LinkedHashMap<>();

        for (MachineTier tier : MachineTier.ALL) {
            putReserved(result, tier.casingRegistryName(), "tier machine casing " + tier.id());
        }
        for (IndustrialMaterial material : context.materials()) {
            for (MaterialPart part : material.parts()) {
                if (part.isBlock() && !material.hasExistingPart(part)) {
                    putReserved(result, part.registryName(material),
                            "material block " + material.id() + "/" + part.id());
                }
            }
            if (MaterialOreHost.hasNaturalOre(material)) {
                for (MaterialOreHost host : MaterialOreHost.compatibleHosts(material)) {
                    putReserved(result, host.registryName(material, false),
                            "ore host block " + material.id() + "/" + host.id());
                    putReserved(result, host.registryName(material, true),
                            "small ore host block " + material.id() + "/" + host.id());
                }
            }
        }
        for (var structureMaterial : context.structureMaterials()) {
            for (var block : StructureMaterialGenerator.generatedBlockDefinitions(structureMaterial)) {
                putReserved(result, block.registryName(),
                        "structure block " + structureMaterial.id() + "/" + block.part().map(MaterialPart::id).orElse("generated"));
            }
        }
        return Map.copyOf(result);
    }

    private static void putReserved(Map<String, String> result, String id, String owner) {
        String previous = result.putIfAbsent(id, owner);
        if (previous != null && !previous.equals(owner)) {
            // Existing generators keep their own duplicate validation. Preserve the first owner so
            // the casing collision diagnostic remains deterministic and concise.
            result.put(id, previous);
        }
    }

    private static void error(
            ValidationCollector diagnostics,
            ValidationCode code,
            String subject,
            String message
    ) {
        diagnostics.error(ValidationSubsystem.CASING, code, subject, message);
    }

    private static String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? "no message" : exception.getMessage();
    }
}
