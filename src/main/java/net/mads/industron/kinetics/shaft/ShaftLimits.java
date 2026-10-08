package net.mads.industron.kinetics.shaft;

/**
 * Mechanical limits imposed by a shaft material.  Create still owns its normal
 * global kinetic limits; {@link #UNLIMITED} means that Industron does not add a
 * stricter limit for that property.
 */
public record ShaftLimits(float maxRpm, float maxNetworkStress) {
    public static final float UNLIMITED = Float.POSITIVE_INFINITY;

    public static final ShaftLimits WOOD = new ShaftLimits(32.0F, 8192.0F);
    public static final ShaftLimits METAL = new ShaftLimits(UNLIMITED, UNLIMITED);

    public ShaftLimits {
        if (!(maxRpm > 0.0F) || !(maxNetworkStress > 0.0F)) {
            throw new IllegalArgumentException("Shaft limits must be positive");
        }
    }

    public boolean limitsRpm() {
        return Float.isFinite(maxRpm);
    }

    public boolean limitsNetworkStress() {
        return Float.isFinite(maxNetworkStress);
    }

    public boolean isRpmExceeded(float rpm) {
        return limitsRpm() && Math.abs(rpm) > maxRpm;
    }

    public boolean isNetworkStressExceeded(float stress) {
        return limitsNetworkStress() && stress > maxNetworkStress;
    }

    public boolean hasAnyLimit() {
        return limitsRpm() || limitsNetworkStress();
    }
}
