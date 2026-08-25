package net.mads.industron.machine;

/**
 * Runtime power source used by a CE machine.
 *
 * <p>Recipes are intentionally independent of this value. The machine decides
 * how it is powered and which runtime constraints apply.</p>
 */
public enum MachineDrive {
    NONE,
    ELECTRIC,
    STEAM,
    KINETIC,
    KINETIC_OUTPUT;

    public boolean usesKinetic() {
        return this == KINETIC;
    }

    public boolean usesKineticInput() {
        return this == KINETIC;
    }

    public boolean usesKineticOutput() {
        return this == KINETIC_OUTPUT;
    }

    public boolean isKinetic() {
        return usesKineticInput() || usesKineticOutput();
    }
}
