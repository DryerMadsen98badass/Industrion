package net.mads.industron.material.fuel;

/** Exact rational form multiplier applied after composition-derived fuel density. */
public record FuelFormFactor(int numerator, int denominator) {
    public static final FuelFormFactor ONE = new FuelFormFactor(1, 1);
    public FuelFormFactor {
        if (numerator <= 0 || denominator <= 0) {
            throw new IllegalArgumentException("Fuel form factor must be positive");
        }
    }

    public static FuelFormFactor of(int numerator, int denominator) {
        return new FuelFormFactor(numerator, denominator);
    }

    public double apply(double baseFuelUnits) {
        if (!Double.isFinite(baseFuelUnits) || baseFuelUnits <= 0.0D) return 0.0D;
        return baseFuelUnits * numerator / denominator;
    }
}
