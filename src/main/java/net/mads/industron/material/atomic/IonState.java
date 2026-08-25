package net.mads.industron.material.atomic;

import net.mads.industron.material.MaterialProperties;

import java.util.List;

/**
 * Chemical state of one element after electrons have been removed or added.
 * IonState is data only: it never creates charged ingots, nuggets, ores or blocks.
 */
public record IonState(
        int atomicNumber,
        int neutrons,
        int charge,
        long electronCount,
        List<Integer> electronShells,
        int outerShell,
        int outerShellCapacity,
        int outerShellElectrons,
        int valenceSElectrons,
        int valencePElectrons,
        int activeDElectrons,
        int activeFElectrons,
        int unpairedElectrons,
        int effectiveNuclearCharge,
        int atomicRadius,
        int ionizationEnergy,
        int electronAffinity,
        int electronDonationTendency,
        int electronAcceptanceTendency,
        int bondStrength,
        int electronicStability,
        int formationCost,
        int viabilityScore,
        boolean chemicallyAllowed,
        MaterialProperties.ElectronicFamily electronicFamily
) {
    public IonState {
        if (atomicNumber <= 0) {
            throw new IllegalArgumentException("Ion atomic number must be positive");
        }
        if (charge == 0) {
            throw new IllegalArgumentException("IonState charge cannot be zero; use AtomicModel.neutral(...) instead");
        }
        if (electronCount < 0) {
            throw new IllegalArgumentException("Ion electron count cannot be negative");
        }
        electronShells = List.copyOf(electronShells);
    }

    public boolean cation() {
        return charge > 0;
    }

    public boolean anion() {
        return charge < 0;
    }
}
