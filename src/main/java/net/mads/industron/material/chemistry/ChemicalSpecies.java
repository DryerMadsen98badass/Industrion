package net.mads.industron.material.chemistry;

/** A runtime chemical species: substance identity plus formula/charge, without registry explosion. */
public record ChemicalSpecies(
        String id,
        SubstanceIdentity identity,
        ChemicalFormula formula,
        ChemistryPhase phase,
        ChemicalStructure.Topology topology
) {
    public ChemicalSpecies {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("species id cannot be blank");
        id = id.trim().toLowerCase(java.util.Locale.ROOT);
        phase = phase == null ? ChemistryPhase.UNKNOWN : phase;
        topology = topology == null ? ChemicalStructure.Topology.UNKNOWN : topology;
    }

    public int netCharge() {
        return formula == null ? 0 : formula.netCharge();
    }
}
