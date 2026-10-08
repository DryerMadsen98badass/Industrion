package net.mads.industron.material.recipes;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyMaterialSelector;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRequirement;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipes.assembly.ComponentDefinitions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Resolves casing definitions into concrete generated casings. */
public final class MaterialCasingGenerator {
    public record GeneratedCasing(
            CasingDefinition definition,
            IndustrialMaterial material,
            MachineTier tier,
            List<AssemblyRequirement> materialRequirements
    ) {
        public GeneratedCasing {
            Objects.requireNonNull(definition, "definition");
            Objects.requireNonNull(material, "material");
            Objects.requireNonNull(tier, "tier");
            materialRequirements = List.copyOf(materialRequirements);
        }

        public String registryName() { return material.id() + "_" + definition.id(); }
        public String displayName() { return material.displayName() + " " + definition.displayName(); }
    }

    public static final List<GeneratedCasing> ALL = generate(MaterialCasingRecipes.ALL, IndustrialMaterials.ALL);

    private MaterialCasingGenerator() {
    }

    public static GeneratedCasing find(String registryName) {
        return ALL.stream().filter(generated -> generated.registryName().equals(registryName)).findFirst().orElse(null);
    }

    /** Recipe-level casing requirements AND input-local stats. */
    public static List<AssemblyRequirement> effectiveInputRequirements(
            GeneratedCasing generated,
            CasingDefinition.Input input
    ) {
        if (input.kind() != CasingDefinition.InputKind.MATERIAL
                && input.kind() != CasingDefinition.InputKind.COMPONENT) {
            return List.of();
        }

        List<AssemblyRequirement> local = scaleRequirements(
                input.requirements(),
                generated.definition().startTier(),
                generated.tier()
        );
        if (generated.materialRequirements().isEmpty()) return local;
        if (local.isEmpty()) return generated.materialRequirements();

        List<AssemblyRequirement> combined = new ArrayList<>(
                generated.materialRequirements().size() + local.size()
        );
        combined.addAll(generated.materialRequirements());
        combined.addAll(local);
        return List.copyOf(combined);
    }

    /**
     * Deterministically ordered casing resolution used by registration and validation.
     * Definition/material declaration order is deliberately not part of generated identity.
     */
    public static List<GeneratedCasing> generate(
            List<CasingDefinition> definitions,
            List<IndustrialMaterial> materials
    ) {
        List<GeneratedCasing> result = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();

        List<CasingDefinition> orderedDefinitions = definitions.stream()
                .sorted(Comparator.comparing(CasingDefinition::id))
                .toList();
        List<IndustrialMaterial> orderedMaterials = materials.stream()
                .sorted(Comparator.comparing(IndustrialMaterial::id))
                .toList();

        for (CasingDefinition definition : orderedDefinitions) {
            int startIndex = MachineTier.ALL.indexOf(definition.startTier());
            if (startIndex < 0) {
                throw new IllegalStateException("Unknown casing start tier: " + definition.startTier().id());
            }

            for (IndustrialMaterial material : orderedMaterials) {
                if (material.isClayMaterial()) continue;
                int tierIndex = MachineTier.ALL.indexOf(material.tier());
                if (tierIndex < startIndex) continue;

                List<AssemblyRequirement> scaled = scaleRequirements(
                        definition.materialRequirements(),
                        definition.startTier(),
                        material.tier()
                );
                GeneratedCasing candidate = new GeneratedCasing(definition, material, material.tier(), scaled);
                if (!qualifies(candidate)) continue;
                if (!ids.add(candidate.registryName())) {
                    throw new IllegalStateException("Duplicate generated casing registry id: " + candidate.registryName());
                }
                result.add(candidate);
            }
        }
        return List.copyOf(result);
    }

    private static boolean qualifies(GeneratedCasing generated) {
        IndustrialMaterial casingMaterial = generated.material();
        if (!matchesRaw(casingMaterial, generated.materialRequirements())) return false;
        if (!casingMaterial.has(generated.definition().baseBlockInput())) return false;

        for (CasingDefinition.Input input : generated.definition().inputs()) {
            List<AssemblyRequirement> requirements = effectiveInputRequirements(generated, input);
            switch (input.kind()) {
                case MATERIAL -> {
                    if (!materialInputAvailable(input.material(), input.materialOverride(), casingMaterial, requirements)) {
                        return false;
                    }
                }
                case COMPONENT -> {
                    if (!componentInputAvailable(
                            input.component(),
                            input.materialOverride(),
                            casingMaterial,
                            requirements
                    )) {
                        return false;
                    }
                }
                case TOOL -> {
                    if (!AssemblyTools.hasType(input.tool())) {
                        return false;
                    }
                }
                case ITEM, WAIT -> {
                    // Exact registry ids are validated after registries exist; waits are always valid.
                }
            }
        }
        return true;
    }

    private static boolean materialInputAvailable(
            MaterialPart part,
            AssemblyMaterialSelector override,
            IndustrialMaterial inherited,
            List<AssemblyRequirement> requirements
    ) {
        if (override == null) return matchesForm(inherited, part, requirements);
        if (override.isFixed()) return matchesForm(override.resolve(), part, requirements);

        return override.candidates().stream()
                .anyMatch(material -> matchesForm(material, part, requirements));
    }

    private static boolean componentInputAvailable(
            net.mads.industron.recipe.recipetypes.assembly.AssemblyComponent component,
            AssemblyMaterialSelector override,
            IndustrialMaterial inherited,
            List<AssemblyRequirement> requirements
    ) {
        if (override == null) {
            return ComponentDefinitions.canResolve(component, inherited, requirements);
        }
        if (override.isFixed()) {
            return ComponentDefinitions.canResolve(component, override.resolve(), requirements);
        }
        return ComponentDefinitions.canResolve(component, override, requirements);
    }

    private static boolean matchesForm(
            IndustrialMaterial material,
            MaterialPart part,
            List<AssemblyRequirement> requirements
    ) {
        if (!material.has(part)) return false;
        return requirements.stream().allMatch(requirement -> requirement.matches(material, part));
    }

    private static boolean matchesRaw(
            IndustrialMaterial material,
            List<AssemblyRequirement> requirements
    ) {
        for (AssemblyRequirement requirement : requirements) {
            if (requirement.capability() != null && requirement.capability().isPracticalStat()) return false;
            if (!requirement.matches(material.properties())) return false;
        }
        return true;
    }

    private static List<AssemblyRequirement> scaleRequirements(
            List<AssemblyRequirement> requirements,
            MachineTier startTier,
            MachineTier targetTier
    ) {
        List<AssemblyRequirement> result = new ArrayList<>(requirements.size());
        for (AssemblyRequirement requirement : requirements) {
            result.add(MaterialStatTierScaling.scale(requirement, startTier, targetTier));
        }
        return List.copyOf(result);
    }
}
