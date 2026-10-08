package net.mads.industron.material.chemistry;

import java.math.BigInteger;
import java.util.Objects;

/** Exact rational amount used by the canonical composition model. */
public record CompositionAmount(BigInteger numerator, BigInteger denominator) implements Comparable<CompositionAmount> {
    public static final CompositionAmount ZERO = new CompositionAmount(BigInteger.ZERO, BigInteger.ONE);
    public static final CompositionAmount ONE = new CompositionAmount(BigInteger.ONE, BigInteger.ONE);

    public CompositionAmount {
        numerator = Objects.requireNonNull(numerator, "numerator");
        denominator = Objects.requireNonNull(denominator, "denominator");
        if (denominator.signum() == 0) throw new ArithmeticException("zero denominator");
        if (denominator.signum() < 0) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        BigInteger divisor = numerator.gcd(denominator);
        numerator = numerator.divide(divisor);
        denominator = denominator.divide(divisor);
    }

    public static CompositionAmount of(long value) {
        return new CompositionAmount(BigInteger.valueOf(value), BigInteger.ONE);
    }

    public static CompositionAmount of(BigInteger value) {
        return new CompositionAmount(value, BigInteger.ONE);
    }

    public CompositionAmount add(CompositionAmount other) {
        return new CompositionAmount(
                numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)),
                denominator.multiply(other.denominator)
        );
    }

    public CompositionAmount multiply(CompositionAmount other) {
        return new CompositionAmount(
                numerator.multiply(other.numerator),
                denominator.multiply(other.denominator)
        );
    }

    public CompositionAmount divide(BigInteger divisor) {
        return new CompositionAmount(numerator, denominator.multiply(divisor));
    }

    public boolean isZero() {
        return numerator.signum() == 0;
    }

    public String signature() {
        return denominator.equals(BigInteger.ONE) ? numerator.toString() : numerator + "/" + denominator;
    }

    @Override
    public int compareTo(CompositionAmount other) {
        return numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator));
    }
}
