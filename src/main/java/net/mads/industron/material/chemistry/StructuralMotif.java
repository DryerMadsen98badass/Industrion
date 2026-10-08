package net.mads.industron.material.chemistry;

import java.util.ArrayList;
import java.util.List;

/** Phase 12 structural motif detection over the generic fictional atom graph. */
public record StructuralMotif(String id, String description) {
    public static final StructuralMotif CHARGED_TERMINAL = new StructuralMotif(
            "charged_terminal", "terminal atom with a formal charge"
    );
    public static final StructuralMotif MULTIPLE_BOND = new StructuralMotif(
            "multiple_bond", "double/triple/aromatic bond"
    );
    public static final StructuralMotif BRANCHED_NODE = new StructuralMotif(
            "branched_node", "atom connected to three or more neighbours"
    );
    public static final StructuralMotif CROSS_LINK = new StructuralMotif(
            "cross_link", "polymer/network-like crosslink"
    );

    public StructuralMotif {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("motif id cannot be blank");
        id = id.trim().toLowerCase(java.util.Locale.ROOT);
        description = description == null ? "" : description;
    }

    public static List<StructuralMotif> detect(ChemicalStructure structure) {
        if (structure == null) return List.of();
        List<StructuralMotif> result = new ArrayList<>();

        boolean chargedTerminal = false;
        boolean multipleBond = false;
        boolean branched = false;
        for (ChemicalAtom atom : structure.atoms()) {
            int degree = 0;
            for (ChemicalBond bond : structure.bonds()) {
                if (bond.firstAtom().equals(atom.id()) || bond.secondAtom().equals(atom.id())) degree++;
                if (bond.order().value() > 1.0D) multipleBond = true;
            }
            if (degree <= 1 && atom.formalCharge() != 0) chargedTerminal = true;
            if (degree >= 3) branched = true;
        }

        if (chargedTerminal) result.add(CHARGED_TERMINAL);
        if (multipleBond) result.add(MULTIPLE_BOND);
        if (branched) result.add(BRANCHED_NODE);
        if (structure.crosslinkDensity() > 0.0D
                || structure.topology() == ChemicalStructure.Topology.POLYMER_NETWORK
                || structure.topology() == ChemicalStructure.Topology.NETWORK) {
            result.add(CROSS_LINK);
        }
        return List.copyOf(result);
    }
}
