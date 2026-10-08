package net.mads.industron.material.chemistry.process;

import net.mads.industron.material.AutomaticProcessIntermediate;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.chemistry.ChemistryPhase;
import net.mads.industron.material.chemistry.MaterialAnalysis;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Resolves one deterministic fictional material for a process requirement. */
public final class CompatibleMaterialResolver {
    public List<MaterialAnalysis> resolve(ProcessRequirement requirement, Map<String, MaterialAnalysis> materials) {
        if (requirement.fixedMaterial()) {
            MaterialAnalysis fixed = materials.get(requirement.fixedMaterialId());
            if (fixed == null || !matches(requirement, fixed)) return List.of();
            return List.of(fixed);
        }

        return materials.values().stream()
                // Target-specific slurry/solution/pyrolysate/etc. are graph states, never generic process aids.
                .filter(material -> !AutomaticProcessIntermediate.isAutomatic(material.source()))
                .filter(material -> matches(requirement, material))
                .sorted(Comparator
                        .comparingDouble((MaterialAnalysis candidate) -> score(requirement, candidate)).reversed()
                        .thenComparing(candidate -> candidate.source().id()))
                .toList();
    }

    public java.util.Optional<MaterialAnalysis> resolveBest(
            ProcessRequirement requirement,
            Map<String, MaterialAnalysis> materials
    ) {
        return resolve(requirement, materials).stream().findFirst();
    }

    private static boolean matches(ProcessRequirement requirement, MaterialAnalysis candidate) {
        if (!requirement.constraints().stream().allMatch(constraint ->
                constraint.matches(candidate.properties().get(constraint.property())))) return false;
        return hasUsableForm(candidate, requirement.requiredPhase());
    }

    private static boolean hasUsableForm(MaterialAnalysis candidate, ChemistryPhase requested) {
        if (!(candidate.source().backingMaterial() instanceof IndustrialMaterial material)) return false;
        ChemistryPhase phase = requested == ChemistryPhase.UNKNOWN ? candidate.phase() : requested;
        return switch (phase) {
            case GAS, PLASMA -> material.has(MaterialPart.GAS);
            case LIQUID -> material.has(MaterialPart.LIQUID);
            case MOLTEN -> material.has(MaterialPart.MOLTEN_FLUID);
            default -> material.has(MaterialPart.DUST)
                    || material.has(MaterialPart.GEM)
                    || material.has(MaterialPart.INGOT);
        };
    }

    private static double score(ProcessRequirement requirement, MaterialAnalysis candidate) {
        double score = 0.0D;
        for (PropertyConstraint constraint : requirement.constraints()) {
            double value = candidate.properties().get(constraint.property());
            double midpoint = (constraint.minimum() + constraint.maximum()) * 0.5D;
            double halfRange = Math.max(1.0D, Math.abs(constraint.maximum() - constraint.minimum()) * 0.5D);
            score += Math.max(0.0D, 100.0D - Math.abs(value - midpoint) * 100.0D / halfRange);
        }
        score += switch (requirement.role()) {
            case CATALYST, ACTIVATOR -> candidate.properties().get("catalyticactivity") * 1.5D
                    + candidate.properties().get("reactivity") * 0.35D;
            case SOLVENT -> candidate.properties().get("polarity")
                    + candidate.properties().get("chemicalstability") * 0.5D;
            case OXIDIZER -> candidate.properties().get("electronacceptancetendency");
            case REDUCER -> candidate.properties().get("electrondonationtendency");
            default -> 0.0D;
        };
        // Stable tiebreak still comes from id in the comparator, never random order.
        return score;
    }
}
