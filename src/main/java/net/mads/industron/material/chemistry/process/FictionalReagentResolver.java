package net.mads.industron.material.chemistry.process;

import net.mads.industron.material.AutomaticProcessIntermediate;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.chemistry.MaterialAnalysis;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Chooses concrete fictional catalyst/activator materials exclusively from registered Industron substances.
 * Acid/base conditions are not materials here: they are expressed through the machine Chemical Balance (CB) system.
 */
public final class FictionalReagentResolver {
    public Optional<String> resolveCatalyst(MaterialAnalysis target, Map<String, MaterialAnalysis> materials) {
        return resolveCatalyst(target, materials, target.calculatedTierIndex());
    }

    public Optional<String> resolveCatalyst(
            MaterialAnalysis target,
            Map<String, MaterialAnalysis> materials,
            int maximumTierIndex
    ) {
        int allowedTier = Math.max(target.calculatedTierIndex(), maximumTierIndex);
        return materials.values().stream()
                .filter(candidate -> !candidate.source().id().equals(target.source().id()))
                .filter(candidate -> !AutomaticProcessIntermediate.isAutomatic(candidate.source()))
                .filter(candidate -> candidate.calculatedTierIndex() <= allowedTier)
                .filter(FictionalReagentResolver::hasUsableCondensedForm)
                .filter(candidate -> normalized(candidate.properties().get("catalyticactivity")) >= 50.0D)
                .sorted(Comparator
                        .comparingDouble((MaterialAnalysis candidate) -> catalystScore(target, candidate)).reversed()
                        .thenComparing(candidate -> candidate.source().id()))
                .map(candidate -> candidate.source().id())
                .findFirst();
    }

    public Optional<String> resolveActivator(MaterialAnalysis target, Map<String, MaterialAnalysis> materials) {
        return resolveActivator(target, materials, Set.of());
    }

    public Optional<String> resolveActivator(
            MaterialAnalysis target,
            Map<String, MaterialAnalysis> materials,
            Set<String> excludedMaterialIds
    ) {
        return resolveActivator(target, materials, excludedMaterialIds, target.calculatedTierIndex());
    }

    public Optional<String> resolveActivator(
            MaterialAnalysis target,
            Map<String, MaterialAnalysis> materials,
            Set<String> excludedMaterialIds,
            int maximumTierIndex
    ) {
        Set<String> excluded = excludedMaterialIds == null ? Set.of() : Set.copyOf(excludedMaterialIds);
        int allowedTier = Math.max(target.calculatedTierIndex(), maximumTierIndex);
        return materials.values().stream()
                .filter(candidate -> !candidate.source().id().equals(target.source().id()))
                .filter(candidate -> !excluded.contains(candidate.source().id()))
                .filter(candidate -> !AutomaticProcessIntermediate.isAutomatic(candidate.source()))
                .filter(candidate -> candidate.calculatedTierIndex() <= allowedTier)
                .filter(FictionalReagentResolver::hasUsableCondensedForm)
                .filter(candidate -> normalized(candidate.properties().get("catalyticactivity")) >= 35.0D)
                .filter(candidate -> {
                    double reactivity = normalized(candidate.properties().get("reactivity"));
                    return reactivity >= 12.0D && reactivity <= 88.0D;
                })
                .sorted(Comparator
                        .comparingDouble((MaterialAnalysis candidate) -> activatorScore(target, candidate)).reversed()
                        .thenComparing(candidate -> candidate.source().id()))
                .map(candidate -> candidate.source().id())
                .findFirst();
    }

    private static double catalystScore(MaterialAnalysis target, MaterialAnalysis candidate) {
        double catalytic = normalized(candidate.properties().get("catalyticactivity"));
        double stability = normalized(candidate.properties().get("chemicalstability"));
        double reactivity = normalized(candidate.properties().get("reactivity"));
        double targetReactivity = normalized(target.properties().get("reactivity"));
        double desired = Math.max(20.0D, 65.0D - targetReactivity * 0.35D);
        double reactivityMatch = 100.0D - Math.min(100.0D, Math.abs(reactivity - desired));
        return catalytic * 0.60D + stability * 0.20D + reactivityMatch * 0.20D;
    }

    private static double activatorScore(MaterialAnalysis target, MaterialAnalysis candidate) {
        double catalytic = normalized(candidate.properties().get("catalyticactivity"));
        double reactivity = normalized(candidate.properties().get("reactivity"));
        double targetStability = normalized(target.properties().get("chemicalstability"));
        double targetBond = normalized(target.properties().get("bondstrength"));
        double desiredReactivity = Math.min(80.0D, 35.0D + (targetStability + targetBond) * 0.20D);
        double reactivityMatch = 100.0D - Math.min(100.0D, Math.abs(reactivity - desiredReactivity));
        return catalytic * 0.58D + reactivityMatch * 0.42D;
    }

    private static boolean hasUsableCondensedForm(MaterialAnalysis analysis) {
        if (!(analysis.source().backingMaterial() instanceof IndustrialMaterial material)) return false;
        // Process aids follow the same concrete-form precedence as .contains consumers:
        // DUST first, otherwise normal LIQUID, otherwise GAS. A molten form is created only
        // by an explicit melting route and is never an implicit catalyst/activator fallback.
        return material.has(MaterialPart.DUST)
                || material.has(MaterialPart.LIQUID)
                || material.has(MaterialPart.GAS);
    }

    private static double normalized(double value) {
        if (!Double.isFinite(value)) return 0.0D;
        return clamp(value);
    }



    private static double clamp(double value) {
        return Math.max(0.0D, Math.min(100.0D, value));
    }
}
