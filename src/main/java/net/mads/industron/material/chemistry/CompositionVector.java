package net.mads.industron.material.chemistry;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Canonical, sorted composition vector shared by chemistry, validation and molten mixtures. */
public record CompositionVector(Map<String, CompositionAmount> entries) {
    public static final CompositionVector EMPTY = new CompositionVector(Map.of());

    public CompositionVector {
        Map<String, CompositionAmount> normalized = new TreeMap<>();
        if (entries != null) {
            for (Map.Entry<String, CompositionAmount> entry : entries.entrySet()) {
                String id = normalize(entry.getKey());
                CompositionAmount amount = entry.getValue();
                if (amount == null || amount.isZero()) continue;
                normalized.merge(id, amount, CompositionAmount::add);
            }
        }
        normalized.entrySet().removeIf(entry -> entry.getValue().isZero());
        entries = Collections.unmodifiableMap(new LinkedHashMap<>(normalized));
    }

    public static CompositionVector direct(List<CompositionEntry> composition) {
        if (composition == null || composition.isEmpty()) return EMPTY;
        Map<String, BigInteger> amounts = new TreeMap<>();
        for (CompositionEntry entry : composition) {
            amounts.merge(normalize(entry.substanceId()), BigInteger.valueOf(entry.amount()), BigInteger::add);
        }
        return ratio(amounts);
    }

    public static CompositionVector ratio(Map<String, BigInteger> weights) {
        if (weights == null || weights.isEmpty()) return EMPTY;
        BigInteger gcd = BigInteger.ZERO;
        Map<String, BigInteger> merged = new TreeMap<>();
        for (Map.Entry<String, BigInteger> entry : weights.entrySet()) {
            String id = normalize(entry.getKey());
            BigInteger value = entry.getValue();
            if (value == null || value.signum() <= 0) {
                throw new IllegalArgumentException("Composition weights must be positive: " + id);
            }
            merged.merge(id, value, BigInteger::add);
        }
        for (BigInteger value : merged.values()) gcd = gcd.gcd(value);
        Map<String, CompositionAmount> result = new TreeMap<>();
        for (Map.Entry<String, BigInteger> entry : merged.entrySet()) {
            result.put(entry.getKey(), CompositionAmount.of(entry.getValue().divide(gcd)));
        }
        return new CompositionVector(result);
    }

    public CompositionVector multiply(CompositionAmount factor) {
        if (entries.isEmpty() || factor.isZero()) return EMPTY;
        Map<String, CompositionAmount> result = new TreeMap<>();
        for (Map.Entry<String, CompositionAmount> entry : entries.entrySet()) {
            result.put(entry.getKey(), entry.getValue().multiply(factor));
        }
        return new CompositionVector(result);
    }

    public CompositionVector add(CompositionVector other) {
        if (other == null || other.entries.isEmpty()) return this;
        if (entries.isEmpty()) return other;
        Map<String, CompositionAmount> result = new TreeMap<>(entries);
        other.entries.forEach((id, amount) -> result.merge(id, amount, CompositionAmount::add));
        return new CompositionVector(result);
    }

    public String signature() {
        if (entries.isEmpty()) return "empty";
        StringBuilder builder = new StringBuilder();
        entries.forEach((id, amount) -> {
            if (!builder.isEmpty()) builder.append(';');
            builder.append(id).append('=').append(amount.signature());
        });
        return builder.toString();
    }

    public Map<String, BigInteger> integerWeights() {
        Map<String, BigInteger> result = new TreeMap<>();
        for (Map.Entry<String, CompositionAmount> entry : entries.entrySet()) {
            if (!entry.getValue().denominator().equals(BigInteger.ONE)) {
                throw new ArithmeticException("Composition vector contains fractional amount for " + entry.getKey());
            }
            result.put(entry.getKey(), entry.getValue().numerator());
        }
        return Map.copyOf(result);
    }

    private static String normalize(String value) {
        if (value == null) throw new IllegalArgumentException("Composition id cannot be null");
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) throw new IllegalArgumentException("Composition id cannot be blank");
        return normalized;
    }
}
