package net.mads.industron.material.chemistry;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Canonical chemistry formula data, separate from display-only material formula text. */
public record ChemicalFormula(Map<String, Integer> atoms, int netCharge) {
    public ChemicalFormula {
        Map<String, Integer> normalized = new TreeMap<>();
        if (atoms != null) {
            atoms.forEach((id, amount) -> {
                if (id == null || id.isBlank() || amount == null || amount <= 0) return;
                normalized.merge(id.trim().toLowerCase(java.util.Locale.ROOT), amount, Math::addExact);
            });
        }
        atoms = Collections.unmodifiableMap(new LinkedHashMap<>(normalized));
    }

    public static ChemicalFormula neutral(Map<String, Integer> atoms) {
        return new ChemicalFormula(atoms, 0);
    }

    public static ChemicalFormula fromStructure(ChemicalStructure structure) {
        return new ChemicalFormula(structure == null ? Map.of() : structure.formula(),
                structure == null ? 0 : structure.netCharge());
    }

    public static Optional<ChemicalFormula> fromAtomicVector(CompositionVector vector, int netCharge) {
        if (vector == null || vector.entries().isEmpty()) return java.util.Optional.empty();
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, CompositionAmount> entry : vector.entries().entrySet()) {
            if (!entry.getValue().denominator().equals(BigInteger.ONE)) return java.util.Optional.empty();
            result.put(entry.getKey(), entry.getValue().numerator().intValueExact());
        }
        return Optional.of(new ChemicalFormula(result, netCharge));
    }

    public static Optional<ChemicalFormula> stoichiometricFromAtomicVector(CompositionVector vector, int netCharge) {
        if (vector == null || vector.entries().isEmpty()) return Optional.empty();
        BigInteger lcm = BigInteger.ONE;
        for (CompositionAmount amount : vector.entries().values()) {
            lcm = lcm(lcm, amount.denominator());
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, CompositionAmount> entry : vector.entries().entrySet()) {
            BigInteger scaled = entry.getValue().numerator().multiply(lcm).divide(entry.getValue().denominator());
            if (scaled.signum() <= 0) return Optional.empty();
            result.put(entry.getKey(), scaled.intValueExact());
        }
        return Optional.of(new ChemicalFormula(result, Math.multiplyExact(netCharge, lcm.intValueExact())));
    }

    public ChemicalFormula add(ChemicalFormula other) {
        Map<String, Integer> result = new LinkedHashMap<>(atoms);
        other.atoms.forEach((id, amount) -> result.merge(id, amount, Math::addExact));
        return new ChemicalFormula(result, netCharge + other.netCharge);
    }

    public ChemicalFormula multiply(int factor) {
        if (factor < 1) throw new IllegalArgumentException("formula factor must be positive");
        Map<String, Integer> result = new LinkedHashMap<>();
        atoms.forEach((id, amount) -> result.put(id, Math.multiplyExact(amount, factor)));
        return new ChemicalFormula(result, Math.multiplyExact(netCharge, factor));
    }

    public String signature() {
        StringBuilder builder = new StringBuilder();
        atoms.forEach((id, amount) -> {
            if (!builder.isEmpty()) builder.append(';');
            builder.append(id).append('=').append(amount);
        });
        return builder.append("|charge=").append(netCharge).toString();
    }

    private static BigInteger lcm(BigInteger a, BigInteger b) {
        if (a.signum() == 0 || b.signum() == 0) return BigInteger.ZERO;
        return a.divide(a.gcd(b)).multiply(b).abs();
    }
}
