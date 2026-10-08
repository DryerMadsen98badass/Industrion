package net.mads.industron.material.chemistry;

/** One stoichiometric participant in a Phase 13 reaction plan. */
public record ReactionParticipant(String materialId, long milliUnits, ChemistryPhase phase) {
    public ReactionParticipant {
        if (materialId == null || materialId.isBlank()) throw new IllegalArgumentException("material id cannot be blank");
        materialId = materialId.trim().toLowerCase(java.util.Locale.ROOT);
        if (milliUnits <= 0) throw new IllegalArgumentException("reaction participant amount must be positive");
        phase = phase == null ? ChemistryPhase.UNKNOWN : phase;
    }

    public static ReactionParticipant units(String materialId, long materialUnits, ChemistryPhase phase) {
        return new ReactionParticipant(materialId, Math.multiplyExact(materialUnits, 1000L), phase);
    }
}
