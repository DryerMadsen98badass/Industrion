package net.mads.industron.machine.foundry;

import net.mads.industron.material.chemistry.CompositionVector;

import java.math.BigInteger;
import java.util.Map;
import java.util.TreeMap;

/** Exact intensive composition: splitting a fluid stack never rounds its ingredients. */
public record MoltenRatio(Map<String, BigInteger> weights) {
    public MoltenRatio {
        if (weights.isEmpty() || weights.size() > 64) throw new IllegalArgumentException("Invalid composition size");
        for (var entry : weights.entrySet()) {
            if (entry.getKey().isBlank() || entry.getValue().signum() <= 0
                    || entry.getValue().bitLength() > 4096) throw new IllegalArgumentException("Invalid composition");
        }
        Map<String, BigInteger> normalized = new TreeMap<>();
        CompositionVector.ratio(weights).integerWeights().forEach(normalized::put);
        weights = java.util.Collections.unmodifiableMap(normalized);
    }

    public static MoltenRatio pure(String id) { return new MoltenRatio(Map.of(id, BigInteger.ONE)); }
    public CompositionVector composition() { return CompositionVector.ratio(weights); }
    public BigInteger total() { return weights.values().stream().reduce(BigInteger.ZERO, BigInteger::add); }
    public double share(String id) {
        return new java.math.BigDecimal(weights.getOrDefault(id, BigInteger.ZERO))
                .divide(new java.math.BigDecimal(total()), java.math.MathContext.DECIMAL64).doubleValue();
    }

    public MoltenRatio mix(long amount, MoltenRatio other, long otherAmount) {
        if (amount <= 0 || otherAmount <= 0) throw new IllegalArgumentException("Positive volumes required");
        Map<String, BigInteger> result = new TreeMap<>();
        BigInteger left = other.total().multiply(BigInteger.valueOf(amount));
        BigInteger right = total().multiply(BigInteger.valueOf(otherAmount));
        weights.forEach((id, value) -> result.put(id, value.multiply(left)));
        other.weights.forEach((id, value) -> result.merge(id, value.multiply(right), BigInteger::add));
        return new MoltenRatio(result);
    }

    /** Refuse fractional mB outputs; a centrifuge waits for a complete exact batch. */
    public Map<String, Integer> splitExact(int amount) {
        Map<String, Integer> result = new TreeMap<>();
        for (var entry : weights.entrySet()) {
            BigInteger[] division = entry.getValue().multiply(BigInteger.valueOf(amount)).divideAndRemainder(total());
            if (division[1].signum() != 0 || division[0].signum() <= 0) return Map.of();
            result.put(entry.getKey(), division[0].intValueExact());
        }
        return result;
    }

    public Map<String, String> encode() {
        Map<String, String> result = new TreeMap<>();
        weights.forEach((id, value) -> result.put(id, value.toString()));
        return Map.copyOf(result);
    }

    public static MoltenRatio decode(Map<String, String> data) {
        Map<String, BigInteger> result = new TreeMap<>();
        data.forEach((id, value) -> {
            if (value.length() > 1234) throw new IllegalArgumentException("Composition weight too large");
            result.put(id, new BigInteger(value));
        });
        return new MoltenRatio(result);
    }
}
