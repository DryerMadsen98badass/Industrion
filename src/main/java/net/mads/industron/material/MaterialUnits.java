package net.mads.industron.material;

import net.mads.industron.material.chemistry.ChemistryPhase;

/** Shared exact conversion between one normal material unit and serialized volume. */
public final class MaterialUnits {
    /** One liquid/molten material unit is 144 mB. */
    public static final int LIQUID_MILLIBUCKETS_PER_UNIT = 144;

    /** One gas material unit occupies four times the liquid volume: 576 mB. */
    public static final int GAS_MILLIBUCKETS_PER_UNIT = LIQUID_MILLIBUCKETS_PER_UNIT * 4;

    /** Backwards-compatible liquid-volume constant for non-phase-aware callers. */
    public static final int MILLIBUCKETS_PER_UNIT = LIQUID_MILLIBUCKETS_PER_UNIT;

    private MaterialUnits() {
    }

    /** Converts normal liquid material units to mB. */
    public static int toMillibuckets(int materialUnits) {
        return toMillibuckets(materialUnits, ChemistryPhase.LIQUID);
    }

    /** Converts material units to serialized mB using the physical phase volume. */
    public static int toMillibuckets(int materialUnits, ChemistryPhase phase) {
        if (materialUnits < 0) throw new IllegalArgumentException("Material units cannot be negative");
        return Math.multiplyExact(materialUnits, millibucketsPerUnit(phase));
    }

    /** Converts chemistry milli-units (1000 = one material unit) to exact phase-aware mB. */
    public static int milliUnitsToMillibuckets(long milliUnits, ChemistryPhase phase) {
        if (milliUnits < 0) throw new IllegalArgumentException("Material milli-units cannot be negative");
        int millibucketsPerUnit = millibucketsPerUnit(phase);
        long scaled = Math.multiplyExact(milliUnits, (long) millibucketsPerUnit);
        if (scaled % 1000L != 0L) {
            throw new IllegalStateException(
                    "Material amount " + milliUnits + " milli-units cannot be represented as an exact "
                            + phaseName(phase) + " mB amount"
            );
        }
        return Math.toIntExact(scaled / 1000L);
    }

    /** Converts exact phase-aware mB back to chemistry milli-units (1000 = one material unit). */
    public static long millibucketsToMilliUnits(long millibuckets, ChemistryPhase phase) {
        if (millibuckets < 0) throw new IllegalArgumentException("Millibuckets cannot be negative");
        int millibucketsPerUnit = millibucketsPerUnit(phase);
        long scaled = Math.multiplyExact(millibuckets, 1000L);
        if (scaled % millibucketsPerUnit != 0L) {
            throw new IllegalStateException(
                    phaseName(phase) + " amount " + millibuckets
                            + " mB is not an exact material-unit fraction"
            );
        }
        return scaled / millibucketsPerUnit;
    }

    /** Legacy liquid/molten conversion retained for non-phase-aware callers. */
    public static int milliUnitsToMillibuckets(long milliUnits) {
        return milliUnitsToMillibuckets(milliUnits, ChemistryPhase.LIQUID);
    }

    /** Legacy liquid/molten conversion retained for non-phase-aware callers. */
    public static long millibucketsToMilliUnits(long millibuckets) {
        return millibucketsToMilliUnits(millibuckets, ChemistryPhase.LIQUID);
    }

    public static int millibucketsPerUnit(ChemistryPhase phase) {
        ChemistryPhase normalized = phase == null ? ChemistryPhase.UNKNOWN : phase;
        if (!normalized.isFluidLike()) {
            throw new IllegalArgumentException("Phase " + normalized + " is not fluid-like");
        }
        return normalized == ChemistryPhase.GAS
                ? GAS_MILLIBUCKETS_PER_UNIT
                : LIQUID_MILLIBUCKETS_PER_UNIT;
    }

    private static String phaseName(ChemistryPhase phase) {
        return phase == null ? "fluid" : phase.name().toLowerCase(java.util.Locale.ROOT);
    }
}
