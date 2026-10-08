package net.mads.industron.material.chemistry;

import java.util.List;

/** Phase 12 polymer/network descriptor derived from structure instead of a hardcoded material name. */
public record PolymerDescriptor(
        String repeatUnitId,
        int repeatCount,
        double chainFlexibility,
        double crosslinkDensity,
        List<StructuralMotif> motifs,
        PolymerFamily family
) {
    public enum PolymerFamily {
        NOT_POLYMER,
        THERMOPLASTIC,
        ELASTOMER,
        THERMOSET,
        NETWORK
    }

    public PolymerDescriptor {
        repeatUnitId = repeatUnitId == null ? "" : repeatUnitId.trim().toLowerCase(java.util.Locale.ROOT);
        repeatCount = Math.max(0, repeatCount);
        chainFlexibility = clamp(chainFlexibility);
        crosslinkDensity = clamp(crosslinkDensity);
        motifs = motifs == null ? List.of() : List.copyOf(motifs);
        family = family == null ? PolymerFamily.NOT_POLYMER : family;
    }

    public static PolymerDescriptor from(ChemicalStructure structure) {
        if (structure == null) return none();
        boolean polymer = structure.topology() == ChemicalStructure.Topology.POLYMER_CHAIN
                || structure.topology() == ChemicalStructure.Topology.POLYMER_NETWORK;
        if (!polymer) return none();

        double flexibility = structure.chainFlexibility();
        double crosslinks = structure.crosslinkDensity();
        PolymerFamily family;
        if (structure.topology() == ChemicalStructure.Topology.POLYMER_NETWORK || crosslinks >= 0.65D) {
            family = PolymerFamily.THERMOSET;
        } else if (flexibility >= 0.65D && crosslinks <= 0.30D) {
            family = PolymerFamily.ELASTOMER;
        } else {
            family = PolymerFamily.THERMOPLASTIC;
        }
        return new PolymerDescriptor(
                structure.repeatUnitId(),
                structure.repeatCount(),
                flexibility,
                crosslinks,
                StructuralMotif.detect(structure),
                family
        );
    }

    private static PolymerDescriptor none() {
        return new PolymerDescriptor("", 0, 0, 0, List.of(), PolymerFamily.NOT_POLYMER);
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) return 0;
        return Math.max(0, Math.min(1, value));
    }
}
