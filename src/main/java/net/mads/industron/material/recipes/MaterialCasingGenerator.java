package net.mads.industron.material.recipes;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialMaterials;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.recipe.recipetypes.AssemblyMetal;
import net.mads.industron.recipe.recipetypes.AssemblyRequirement;
import net.mads.industron.recipe.recipetypes.AssemblyTools;
import net.mads.industron.recipe.recipes.assembly.ComponentDefinitions;

import java.util.ArrayList;
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

    public static final List<GeneratedCasing> ALL = generate(MaterialCasingRecipes.ALL);

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

    private static List<GeneratedCasing> generate(List<CasingDefinition> definitions) {
        List<GeneratedCasing> result = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();

        for (CasingDefinition definition : definitions) {
            int startIndex = MachineTier.ALL.indexOf(definition.startTier());
            if (startIndex < 0) {
                throw new IllegalStateException("Unknown casing start tier: " + definition.startTier().id());
            }

            for (IndustrialMaterial material : IndustrialMaterials.ALL) {
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
                    if (!materialInputAvailable(input.material(), input.metalOverride(), casingMaterial, requirements)) {
                        return false;
                    }
                }
                case COMPONENT -> {
                    if (!componentInputAvailable(
                            input.component(),
                            input.metalOverride(),
                            casingMaterial,
                            requirements
                    )) {
                        return false;
                    }
                }
                case TOOL -> {
                    if (AssemblyTools.all().stream().noneMatch(tool -> tool.type().equals(input.tool()))) {
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
            AssemblyMetal override,
            IndustrialMaterial inherited,
            List<AssemblyRequirement> requirements
    ) {
        if (override == null) return matchesForm(inherited, part, requirements);
        if (!override.isAny()) return matchesForm(override.resolve(), part, requirements);

        return IndustrialMaterials.ALL.stream()
                .anyMatch(material -> matchesForm(material, part, requirements));
    }

    private static boolean componentInputAvailable(
            net.mads.industron.recipe.recipetypes.AssemblyComponent component,
            AssemblyMetal override,
            IndustrialMaterial inherited,
            List<AssemblyRequirement> requirements
    ) {
        if (override == null) {
            return ComponentDefinitions.canResolve(component, inherited, requirements);
        }
        if (!override.isAny()) {
            return ComponentDefinitions.canResolve(component, override.resolve(), requirements);
        }
        return ComponentDefinitions.canResolveFree(component, requirements);
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

