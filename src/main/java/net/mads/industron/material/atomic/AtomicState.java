package net.mads.industron.material.atomic;

import net.mads.industron.material.MaterialProperties;

import java.util.List;

/**
 * Deterministic neutral atomic state. This is atomic identity/intermediate physics,
 * not a material form and not something that registers an item or block.
 */
public record AtomicState(
        int protons,
        int neutrons,
        int electrons,
        List<Integer> shells,
        int outerShell,
        int outerShellCapacity,
        int outerShellElectrons,
        int stableValenceTarget,
        int electronsToStableShell,
        int electronsFromStableShell,
        int preferredIonCharge,
        int unpairedElectrons,
        int ionizationEnergy,
        int electronAffinity,
        int electronDonationTendency,
        int electronAcceptanceTendency,
        int bondStrength,
        int atomicStability,
        int effectiveNuclearCharge,
        int atomicRadius,
        int valenceSElectrons,
        int valencePElectrons,
        int activeDElectrons,
        int activeFElectrons,
        int frontierSubshell,
        int frontierOccupancy,
        int frontierCapacity,
        int directionalBonding,
        MaterialProperties.ElectronicFamily family
) {
    public AtomicState {
        if (protons <= 0) {
            throw new IllegalArgumentException("Atomic state must have at least one proton");
        }
        if (electrons != protons) {
            throw new IllegalArgumentException("AtomicState represents a neutral atom; electrons must equal protons");
        }
        shells = List.copyOf(shells);
    }
}
