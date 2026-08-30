package net.mads.industron.material.chemistry;

import java.util.Map;

/**
 * Infers a compound's bulk bond topology from .contains(...) and the calculated properties of its
 * components. The result is a material-level topology, not a claim about an exact crystallographic
 * atom graph. Explicit ChemicalStructure remains an optional override for special cases.
 */
public final class ChemicalTopologyResolver {
    private ChemicalTopologyResolver() {
    }

    public static ChemicalStructure.Topology resolve(
            MaterialSnapshot material,
            Map<String, MaterialSnapshot> registry
    ) {
        if (material.structure().isPresent()) return material.structure().orElseThrow().topology();
        if (material.composition().isEmpty()) return ChemicalStructure.Topology.ATOMIC;

        IonBalance ionBalance = ionBalance(material, registry);
        if (ionBalance.hasPositive() && ionBalance.hasNegative() && ionBalance.netCharge() == 0) {
            return ChemicalStructure.Topology.IONIC_LATTICE;
        }

        boolean allMetalLike = true;
        int resolved = 0;
        double transferContrast = 0.0;
        double bondStrength = 0.0;
        double crystalStability = 0.0;
        double volatility = 0.0;
        int totalWeight = 0;
        int fluidWeight = 0;

        for (CompositionEntry component : material.composition()) {
            MaterialSnapshot child = registry.get(component.substanceId());
            if (child == null) {
                allMetalLike = false;
                continue;
            }
            resolved++;
            int weight = Math.max(1, component.amount());
            totalWeight += weight;
            if (component.phase().isFluidLike()) fluidWeight += weight;

            double metallicity = Math.max(child.property("metalliccharacter"), child.property("metallicity"));
            if (metallicity < 45.0) allMetalLike = false;

            double donation = child.property("electrondonationtendency");
            double acceptance = child.property("electronacceptancetendency");
            transferContrast = Math.max(transferContrast, Math.max(donation, acceptance));
            bondStrength += child.property("bondstrength") * weight;
            crystalStability += child.property("crystalstability") * weight;
            volatility += child.property("volatility") * weight;
        }

        if (totalWeight > 0 && fluidWeight * 2 > totalWeight && material.composition().size() > 1
                && !hasStrongDonorAcceptorPair(material, registry)) {
            return ChemicalStructure.Topology.PHYSICAL_MIXTURE;
        }

        if (resolved == material.composition().size() && allMetalLike) {
            return ChemicalStructure.Topology.METALLIC_LATTICE;
        }

        if (ionBalance.hasPositive() && ionBalance.hasNegative() && Math.abs(ionBalance.netCharge()) <= 1) {
            return ChemicalStructure.Topology.IONIC_LATTICE;
        }

        if (hasStrongDonorAcceptorPair(material, registry) && transferContrast >= 60.0) {
            return ChemicalStructure.Topology.IONIC_LATTICE;
        }

        if (totalWeight > 0) {
            double averageBond = bondStrength / totalWeight;
            double averageCrystal = crystalStability / totalWeight;
            double averageVolatility = volatility / totalWeight;
            if (averageBond >= 55.0 && averageCrystal >= 55.0 && averageVolatility < 45.0) {
                return ChemicalStructure.Topology.NETWORK;
            }
        }

        return ChemicalStructure.Topology.DISCRETE_MOLECULE;
    }

    private static IonBalance ionBalance(MaterialSnapshot material, Map<String, MaterialSnapshot> registry) {
        int net = 0;
        boolean positive = false;
        boolean negative = false;
        for (CompositionEntry component : material.composition()) {
            MaterialSnapshot child = registry.get(component.substanceId());
            if (child == null) continue;
            int charge = (int) Math.round(child.property("preferredioncharge"));
            if (charge > 0) positive = true;
            if (charge < 0) negative = true;
            long contribution = (long) charge * component.amount();
            if (contribution > Integer.MAX_VALUE) contribution = Integer.MAX_VALUE;
            if (contribution < Integer.MIN_VALUE) contribution = Integer.MIN_VALUE;
            long sum = (long) net + contribution;
            net = sum > Integer.MAX_VALUE ? Integer.MAX_VALUE
                    : sum < Integer.MIN_VALUE ? Integer.MIN_VALUE
                    : (int) sum;
        }
        return new IonBalance(net, positive, negative);
    }

    private static boolean hasStrongDonorAcceptorPair(
            MaterialSnapshot material,
            Map<String, MaterialSnapshot> registry
    ) {
        double strongestDonor = 0.0;
        double strongestAcceptor = 0.0;
        for (CompositionEntry component : material.composition()) {
            MaterialSnapshot child = registry.get(component.substanceId());
            if (child == null) continue;
            strongestDonor = Math.max(strongestDonor, child.property("electrondonationtendency"));
            strongestAcceptor = Math.max(strongestAcceptor, child.property("electronacceptancetendency"));
        }
        return strongestDonor >= 55.0 && strongestAcceptor >= 55.0;
    }

    private record IonBalance(int netCharge, boolean hasPositive, boolean hasNegative) {
    }
}
