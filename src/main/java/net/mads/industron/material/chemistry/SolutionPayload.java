package net.mads.industron.material.chemistry;

import java.util.List;

/** Runtime solution payload for dissolved species; it is not a registered item per ion combination. */
public record SolutionPayload(String solventId, List<ChemicalSpecies> dissolvedSpecies) {
    public SolutionPayload {
        if (solventId == null || solventId.isBlank()) throw new IllegalArgumentException("solvent id cannot be blank");
        solventId = solventId.trim().toLowerCase(java.util.Locale.ROOT);
        dissolvedSpecies = dissolvedSpecies == null ? List.of() : List.copyOf(dissolvedSpecies);
    }

    public int netCharge() {
        return dissolvedSpecies.stream().mapToInt(ChemicalSpecies::netCharge).sum();
    }

    public boolean chargeBalanced() {
        return netCharge() == 0;
    }
}
