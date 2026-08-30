package net.mads.industron.material.chemistry;

import java.util.Objects;

public record ChemicalBond(String firstAtom, String secondAtom, BondType type, BondOrder order) {
    public ChemicalBond {
        firstAtom = require(firstAtom, "firstAtom");
        secondAtom = require(secondAtom, "secondAtom");
        if (firstAtom.equals(secondAtom)) throw new IllegalArgumentException("A bond cannot connect an atom to itself");
        type = Objects.requireNonNull(type, "type");
        order = Objects.requireNonNull(order, "order");
    }

    private static String require(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + " cannot be blank");
        return normalized;
    }
}
