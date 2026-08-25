package net.mads.industron.material;

import java.util.List;

public record MaterialProperties(
        int tierMultiplier,
        int protons,
        int neutrons,
        int electrons,
        List<Integer> electronShells,
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
        int bondEnergy,
        int atomicStability,
        int effectiveNuclearCharge,
        int atomicRadius,
        int valenceSElectrons,
        int valencePElectrons,
        int activeDElectrons,
        int activeFElectrons,
        ElectronicFamily electronicFamily,
        int density,
        int hardness,
        int elasticity,
        int tensileStrength,
        int yieldStrength,
        int fractureToughness,
        int compressiveStrength,
        int ductility,
        int brittleness,
        int wearResistance,
        int fatigueResistance,
        int meltingPoint,
        int boilingPoint,
        int thermalConductivity,
        int specificHeatCapacity,
        int thermalExpansion,
        int maxOperatingTemperature,
        int thermalShockResistance,
        int electricalConductivity,
        int insulationStrength,
        ElectricalBehavior electricalBehavior,
        int electrochemicalPotential,
        int chargeStoragePotential,
        int batteryPotential,
        int corrosionResistance,
        int chemicalStability,
        int reactivity,
        int oxidationResistance,
        int acidity,
        int pressureResistance,
        int structuralStrength,
        int maxPressure,
        int magneticTendency,
        int magneticStrength,
        int machinability,
        int formability,
        int weldability,
        int castability,
        CrystalStructure crystalStructure,
        int crystalStability,
        int transparency,
        int refractiveIndex,
        int luster,
        int cleavage,
        int crystalHardness,
        FractureBehavior fractureBehavior,
        int impurityTolerance,
        int opticalPurity,
        int crystalGrowthDifficulty,
        int crystalFormationTemperature,
        int crystalFormationPressure,
        int gemQuality,
        int baseColor,
        int highlightColor,
        int shadowColor,
        int brightness,
        int emissiveStrength,
        PhysicalState state,
        int metallicity,
        MetallicityClass metallicityClass,
        boolean metal,
        boolean magnetic,
        boolean crystalline,
        boolean gemCandidate,
        boolean heatResistant,
        boolean pressureResistant,
        int ambientTemperature,
        int castTemperature,
        int radioactivity,
        int furnaceFuelPotential,
        boolean furnaceFuel,
        int furnaceBurnTimeTicks,
        double frictionCoefficient,
        int pipeCapabilityScore,
        int pipeThroughput,
        int pumpFlowScore,
        double pumpFlowRate,
        int pumpStressImpact,
        int maxFluidTemperature,
        int minChemicalRange,
        int maxChemicalRange,
        int tankCapabilityScore,
        int tankCapacity
) {
    public MaterialProperties {
        electronShells = List.copyOf(electronShells);
    }


    /**
     * Electrical classification is intentionally binary for elemental materials.
     * Metals are conductors; every non-metal is an insulator.
     */
    public enum ElectricalBehavior {
        CONDUCTOR,
        INSULATOR
    }

    public boolean electricallyConductive() {
        return electricalBehavior == ElectricalBehavior.CONDUCTOR;
    }

    public boolean electricallyInsulating() {
        return electricalBehavior == ElectricalBehavior.INSULATOR;
    }

    /**
     * Compatibility name for older callers. Insulation strength is the single source of truth.
     */
    @Deprecated
    public int dielectricStrength() {
        return insulationStrength;
    }

    public enum PhysicalState {
        SOLID,
        LIQUID,
        GAS
    }

    /**
     * Broad electronic family derived from the occupied valence subshells.
     * It is deliberately structural rather than tied to a real-world element name.
     */
    public enum ElectronicFamily {
        ALKALI_LIKE,
        ALKALINE_EARTH_LIKE,
        TRANSITION_EARLY,
        TRANSITION_MIDDLE,
        TRANSITION_LATE,
        COINAGE_LIKE,
        CLOSED_D_SHELL,
        F_BLOCK,
        NETWORK_CRYSTAL_P1,
        NETWORK_CRYSTAL_P2,
        PNICTOGEN_LIKE,
        CHALCOGEN_LIKE,
        HALOGEN_LIKE,
        NOBLE_GAS_LIKE,
        EXTENDED_BLOCK
    }

    public enum MetallicityClass {
        STRONGLY_METALLIC,
        METALLIC,
        SEMI_METALLIC,
        NON_METALLIC
    }

    public enum CrystalStructure {
        AMORPHOUS,
        CUBIC,
        HEXAGONAL,
        LAYERED,
        PRISMATIC,
        IRREGULAR
    }

    public enum FractureBehavior {
        DUCTILE,
        GRANULAR,
        CONCHOIDAL,
        CLEAVED,
        SHATTERING
    }
}
