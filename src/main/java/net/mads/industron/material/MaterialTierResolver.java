package net.mads.industron.material;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.structure.StructureMaterial;

import java.util.HashSet;
import java.util.Set;

/**
 * Progression resolver for composed substances.
 *
 * <p>Geology uses the strongest underlying component tier, recursively. A compound containing an
 * LV component and an EV component therefore belongs to the EV geology band even if calculated
 * bulk properties would otherwise move the manufactured material tier.</p>
 */
public final class MaterialTierResolver {
    private MaterialTierResolver() {
    }

    public static MachineTier strongestComponentTier(IndustrialSubstance substance) {
        return MachineTier.ELECTRIC_TIERS.get(strongestComponentTierIndex(substance));
    }

    public static int strongestComponentTierIndex(IndustrialSubstance substance) {
        if (substance == null) return 0;
        return strongestComponentTierIndex(substance, new HashSet<>());
    }

    public static MaterialOrePolicy.DimensionBand geologyDimension(IndustrialSubstance substance) {
        return MaterialOrePolicy.dimensionForTier(strongestComponentTier(substance));
    }

    public static MachineTier oneTierBelowStrongest(IndustrialSubstance substance) {
        int index = Math.max(0, strongestComponentTierIndex(substance) - 1);
        return MachineTier.ELECTRIC_TIERS.get(index);
    }

    private static int strongestComponentTierIndex(IndustrialSubstance substance, Set<String> stack) {
        if (substance instanceof ElementDefinition element) {
            return tierIndex(element.tier());
        }

        if (!stack.add(substance.id())) {
            // Cyclic composition is invalid elsewhere; returning ULV here prevents recursive failure
            // while validation can report the actual cycle.
            return 0;
        }

        try {
            if (substance instanceof IndustrialMaterial material) {
                if (material.atomicNumber() > 0 || material.components().isEmpty()) {
                    return tierIndex(material.tier());
                }
                return strongest(material.components(), stack);
            }
            if (substance instanceof StructureMaterial structure) {
                if (structure.components().isEmpty()) return 0;
                return strongest(structure.components(), stack);
            }
            return 0;
        } finally {
            stack.remove(substance.id());
        }
    }

    private static int strongest(Iterable<MaterialComponent> components, Set<String> stack) {
        int strongest = 0;
        for (MaterialComponent component : components) {
            strongest = Math.max(strongest, strongestComponentTierIndex(component.substance(), stack));
        }
        return strongest;
    }

    private static int tierIndex(MachineTier tier) {
        int index = MachineTier.ELECTRIC_TIERS.indexOf(tier);
        return Math.max(0, index);
    }
}
