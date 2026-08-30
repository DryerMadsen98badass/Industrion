package net.mads.industron.material.chemistry;

import java.util.Objects;

public record ChemicalAtom(String id, String elementId, int formalCharge) {
    public ChemicalAtom {
        id = requireId(id, "atom id");
        elementId = requireId(elementId, "element id");
    }

    public ChemicalAtom(String id, String elementId) {
        this(id, elementId, 0);
    }

    private static String requireId(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + " cannot be blank");
        return normalized;
    }
}
