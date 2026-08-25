package net.mads.industron.machine.interaction;

/** How a condition combines results from all positions in one named area. */
public enum AreaMatch {
    /** Every position in the area must match. */
    ALL,
    /** At least one position in the area must match. */
    ANY
}
