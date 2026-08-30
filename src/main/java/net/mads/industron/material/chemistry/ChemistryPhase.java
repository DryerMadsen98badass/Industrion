package net.mads.industron.material.chemistry;

/** Physical state used by chemistry/process planning. MOLTEN is intentionally distinct from ambient LIQUID. */
public enum ChemistryPhase {
    SOLID,
    LIQUID,
    GAS,
    MOLTEN,
    PLASMA,
    MIXED,
    UNKNOWN;

    public boolean isFluidLike() {
        return this == LIQUID || this == GAS || this == MOLTEN || this == PLASMA;
    }
}
