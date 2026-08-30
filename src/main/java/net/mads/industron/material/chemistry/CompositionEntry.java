package net.mads.industron.material.chemistry;

import java.util.Objects;

public record CompositionEntry(String substanceId, int amount, ChemistryPhase phase) {
    public CompositionEntry {
        substanceId = Objects.requireNonNull(substanceId, "substanceId").trim().toLowerCase(java.util.Locale.ROOT);
        if (substanceId.isEmpty()) throw new IllegalArgumentException("substanceId cannot be blank");
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        phase = phase == null ? ChemistryPhase.UNKNOWN : phase;
    }
}
