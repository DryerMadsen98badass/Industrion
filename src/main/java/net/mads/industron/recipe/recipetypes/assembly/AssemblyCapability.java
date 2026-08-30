package net.mads.industron.recipe.recipetypes.assembly;

import net.mads.industron.material.MaterialProperties;

import java.util.function.ToDoubleFunction;

/**
 * Typed numeric material stats that can be used by assembly requirements.
 *
 * <p>Normal stats are scalar values. {@link #CHEMICAL} is a coverage range:
 * {@code CHEMICAL.range(-32, 120)} means that the material's chemical range
 * must cover the whole requested interval.</p>
 */
public enum AssemblyCapability {
    TIER_MULTIPLIER("Tier Multiplier", MaterialProperties::tierMultiplier),
    PROTONS("Protons", MaterialProperties::protons),
    NEUTRONS("Neutrons", MaterialProperties::neutrons),
    ELECTRONS("Electrons", MaterialProperties::electrons),
    OUTER_SHELL("Outer Shell", MaterialProperties::outerShell),
    OUTER_SHELL_CAPACITY("Outer Shell Capacity", MaterialProperties::outerShellCapacity),
    OUTER_SHELL_ELECTRONS("Outer Shell Electrons", MaterialProperties::outerShellElectrons),
    STABLE_VALENCE_TARGET("Stable Valence Target", MaterialProperties::stableValenceTarget),
    ELECTRONS_TO_STABLE_SHELL("Electrons To Stable Shell", MaterialProperties::electronsToStableShell),
    ELECTRONS_FROM_STABLE_SHELL("Electrons From Stable Shell", MaterialProperties::electronsFromStableShell),
    PREFERRED_ION_CHARGE("Preferred Ion Charge", MaterialProperties::preferredIonCharge),
    UNPAIRED_ELECTRONS("Unpaired Electrons", MaterialProperties::unpairedElectrons),
    IONIZATION_ENERGY("Ionization Energy", MaterialProperties::ionizationEnergy),
    ELECTRON_AFFINITY("Electron Affinity", MaterialProperties::electronAffinity),
    ELECTRON_DONATION_TENDENCY("Electron Donation Tendency", MaterialProperties::electronDonationTendency),
    ELECTRON_ACCEPTANCE_TENDENCY("Electron Acceptance Tendency", MaterialProperties::electronAcceptanceTendency),
    BOND_STRENGTH("Bond Strength", MaterialProperties::bondStrength),
    BOND_ENERGY("Bond Energy", MaterialProperties::bondEnergy),
    ATOMIC_STABILITY("Atomic Stability", MaterialProperties::atomicStability),
    EFFECTIVE_NUCLEAR_CHARGE("Effective Nuclear Charge", MaterialProperties::effectiveNuclearCharge),
    ATOMIC_RADIUS("Atomic Radius", MaterialProperties::atomicRadius),
    VALENCE_S_ELECTRONS("Valence S Electrons", MaterialProperties::valenceSElectrons),
    VALENCE_P_ELECTRONS("Valence P Electrons", MaterialProperties::valencePElectrons),
    ACTIVE_D_ELECTRONS("Active D Electrons", MaterialProperties::activeDElectrons),
    ACTIVE_F_ELECTRONS("Active F Electrons", MaterialProperties::activeFElectrons),

    DENSITY("Density", MaterialProperties::density),
    HARDNESS("Hardness", MaterialProperties::hardness),
    ELASTICITY("Elasticity", MaterialProperties::elasticity),
    TENSILE_STRENGTH("Tensile Strength", MaterialProperties::tensileStrength),
    YIELD_STRENGTH("Yield Strength", MaterialProperties::yieldStrength),
    FRACTURE_TOUGHNESS("Fracture Toughness", MaterialProperties::fractureToughness),
    COMPRESSIVE_STRENGTH("Compressive Strength", MaterialProperties::compressiveStrength),
    DUCTILITY("Ductility", MaterialProperties::ductility),
    BRITTLENESS("Brittleness", MaterialProperties::brittleness),
    WEAR_RESISTANCE("Wear Resistance", MaterialProperties::wearResistance),
    FATIGUE_RESISTANCE("Fatigue Resistance", MaterialProperties::fatigueResistance),
    STRUCTURAL_STRENGTH("Structural Strength", MaterialProperties::structuralStrength),
    STRUCTURAL_LOAD(
            "Structural Load",
            MaterialProperties::structuralStrength,
            PracticalKind.STRUCTURAL_LOAD
    ),
    SHAFT_LOAD(
            "Shaft Load",
            MaterialProperties::structuralStrength,
            PracticalKind.SHAFT_LOAD
    ),
    FASTENER_LOAD(
            "Fastener Load",
            MaterialProperties::structuralStrength,
            PracticalKind.FASTENER_LOAD
    ),

    MELTING_POINT("Melting Point", MaterialProperties::meltingPoint),
    BOILING_POINT("Boiling Point", MaterialProperties::boilingPoint),
    THERMAL_CONDUCTIVITY("Thermal Conductivity", MaterialProperties::thermalConductivity),
    SPECIFIC_HEAT_CAPACITY("Specific Heat Capacity", MaterialProperties::specificHeatCapacity),
    THERMAL_EXPANSION("Thermal Expansion", MaterialProperties::thermalExpansion),
    MAX_OPERATING_TEMPERATURE("Max Operating Temperature", MaterialProperties::maxOperatingTemperature),
    THERMAL_SHOCK_RESISTANCE("Thermal Shock Resistance", MaterialProperties::thermalShockResistance),

    ELECTRICAL_CONDUCTIVITY("Electrical Conductivity", MaterialProperties::electricalConductivity),
    ELECTRICAL_CAPACITY(
            "Electrical Capacity",
            MaterialProperties::electricalConductivity,
            PracticalKind.ELECTRICAL_CAPACITY
    ),
    INSULATION_STRENGTH("Insulation Strength", MaterialProperties::insulationStrength),
    INSULATION_CAPACITY(
            "Insulation Capacity",
            MaterialProperties::insulationStrength,
            PracticalKind.INSULATION_CAPACITY
    ),
    ELECTROCHEMICAL_POTENTIAL("Electrochemical Potential", MaterialProperties::electrochemicalPotential),
    CHARGE_STORAGE_POTENTIAL("Charge Storage Potential", MaterialProperties::chargeStoragePotential),
    BATTERY_POTENTIAL("Battery Potential", MaterialProperties::batteryPotential),

    CORROSION_RESISTANCE("Corrosion Resistance", MaterialProperties::corrosionResistance),
    CHEMICAL_STABILITY("Chemical Stability", MaterialProperties::chemicalStability),
    REACTIVITY("Reactivity", MaterialProperties::reactivity),
    OXIDATION_RESISTANCE("Oxidation Resistance", MaterialProperties::oxidationResistance),
    ACIDITY("Acidity", MaterialProperties::acidity),
    MIN_CHEMICAL_RANGE("Min Chemical Range", MaterialProperties::minChemicalRange),
    MAX_CHEMICAL_RANGE("Max Chemical Range", MaterialProperties::maxChemicalRange),
    CHEMICAL("Chemical Range", MaterialProperties::minChemicalRange, MaterialProperties::maxChemicalRange),

    PRESSURE_RESISTANCE("Pressure Resistance", MaterialProperties::pressureResistance),
    MAX_PRESSURE("Max Pressure", MaterialProperties::maxPressure),
    PRESSURE_CAPACITY(
            "Pressure Capacity",
            MaterialProperties::maxPressure,
            PracticalKind.PRESSURE_CAPACITY
    ),

    MAGNETIC_TENDENCY("Magnetic Tendency", MaterialProperties::magneticTendency),
    MAGNETIC_STRENGTH("Magnetic Strength", MaterialProperties::magneticStrength),

    MACHINABILITY("Machinability", MaterialProperties::machinability),
    FORMABILITY("Formability", MaterialProperties::formability),
    WELDABILITY("Weldability", MaterialProperties::weldability),
    CASTABILITY("Castability", MaterialProperties::castability),

    CRYSTAL_STABILITY("Crystal Stability", MaterialProperties::crystalStability),
    TRANSPARENCY("Transparency", MaterialProperties::transparency),
    REFRACTIVE_INDEX("Refractive Index", MaterialProperties::refractiveIndex),
    LUSTER("Luster", MaterialProperties::luster),
    CLEAVAGE("Cleavage", MaterialProperties::cleavage),
    CRYSTAL_HARDNESS("Crystal Hardness", MaterialProperties::crystalHardness),
    IMPURITY_TOLERANCE("Impurity Tolerance", MaterialProperties::impurityTolerance),
    OPTICAL_PURITY("Optical Purity", MaterialProperties::opticalPurity),
    CRYSTAL_GROWTH_DIFFICULTY("Crystal Growth Difficulty", MaterialProperties::crystalGrowthDifficulty),
    CRYSTAL_FORMATION_TEMPERATURE("Crystal Formation Temperature", MaterialProperties::crystalFormationTemperature),
    CRYSTAL_FORMATION_PRESSURE("Crystal Formation Pressure", MaterialProperties::crystalFormationPressure),
    GEM_QUALITY("Gem Quality", MaterialProperties::gemQuality),

    BASE_COLOR("Base Color", MaterialProperties::baseColor),
    HIGHLIGHT_COLOR("Highlight Color", MaterialProperties::highlightColor),
    SHADOW_COLOR("Shadow Color", MaterialProperties::shadowColor),
    BRIGHTNESS("Brightness", MaterialProperties::brightness),
    EMISSIVE_STRENGTH("Emissive Strength", MaterialProperties::emissiveStrength),
    METALLICITY("Metallicity", MaterialProperties::metallicity),
    AMBIENT_TEMPERATURE("Ambient Temperature", MaterialProperties::ambientTemperature),
    CAST_TEMPERATURE("Cast Temperature", MaterialProperties::castTemperature),
    RADIOACTIVITY("Radioactivity", MaterialProperties::radioactivity),
    FURNACE_FUEL_POTENTIAL("Furnace Fuel Potential", MaterialProperties::furnaceFuelPotential),
    FURNACE_BURN_TIME_TICKS("Furnace Burn Time Ticks", MaterialProperties::furnaceBurnTimeTicks),

    FRICTION_COEFFICIENT("Friction Coefficient", MaterialProperties::frictionCoefficient),
    PIPE_CAPABILITY_SCORE("Pipe Capability Score", MaterialProperties::pipeCapabilityScore),
    PIPE_THROUGHPUT("Pipe Throughput", MaterialProperties::pipeThroughput),
    PUMP_FLOW_SCORE("Pump Flow Score", MaterialProperties::pumpFlowScore),
    PUMP_FLOW_RATE("Pump Flow Rate", MaterialProperties::pumpFlowRate),
    PUMP_STRESS_IMPACT("Pump Stress Impact", MaterialProperties::pumpStressImpact),
    MAX_FLUID_TEMPERATURE("Max Fluid Temperature", MaterialProperties::maxFluidTemperature),
    TANK_CAPABILITY_SCORE("Tank Capability Score", MaterialProperties::tankCapabilityScore),
    TANK_CAPACITY("Tank Capacity", MaterialProperties::tankCapacity);

    private final String displayName;
    private final ToDoubleFunction<MaterialProperties> scalar;
    private final ToDoubleFunction<MaterialProperties> rangeMin;
    private final ToDoubleFunction<MaterialProperties> rangeMax;
    private final PracticalKind practicalKind;

    enum PracticalKind {
        NONE,
        ELECTRICAL_CAPACITY,
        INSULATION_CAPACITY,
        STRUCTURAL_LOAD,
        PRESSURE_CAPACITY,
        SHAFT_LOAD,
        FASTENER_LOAD
    }

    AssemblyCapability(String displayName, ToDoubleFunction<MaterialProperties> scalar) {
        this(displayName, scalar, null, null, PracticalKind.NONE);
    }

    AssemblyCapability(
            String displayName,
            ToDoubleFunction<MaterialProperties> scalar,
            PracticalKind practicalKind
    ) {
        this(displayName, scalar, null, null, practicalKind);
    }

    AssemblyCapability(
            String displayName,
            ToDoubleFunction<MaterialProperties> rangeMin,
            ToDoubleFunction<MaterialProperties> rangeMax
    ) {
        this(displayName, null, rangeMin, rangeMax, PracticalKind.NONE);
    }

    private AssemblyCapability(
            String displayName,
            ToDoubleFunction<MaterialProperties> scalar,
            ToDoubleFunction<MaterialProperties> rangeMin,
            ToDoubleFunction<MaterialProperties> rangeMax,
            PracticalKind practicalKind
    ) {
        this.displayName = displayName;
        this.scalar = scalar;
        this.rangeMin = rangeMin;
        this.rangeMax = rangeMax;
        this.practicalKind = practicalKind;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isRangeStat() {
        return rangeMin != null && rangeMax != null;
    }

    PracticalKind practicalKind() {
        return practicalKind;
    }

    public boolean isPracticalStat() {
        return practicalKind != PracticalKind.NONE;
    }

    double rawScalar(MaterialProperties properties) {
        if (scalar == null) {
            throw new IllegalStateException(displayName + " is not a scalar capability");
        }
        return scalar.applyAsDouble(properties);
    }

    public AssemblyRequirement atLeast(double value) {
        return AssemblyRequirement.atLeast(this, value);
    }

    public AssemblyRequirement atMost(double value) {
        return AssemblyRequirement.atMost(this, value);
    }

    /**
     * Requires this scalar stat to equal the requested value exactly.
     */
    public AssemblyRequirement exactly(double value) {
        if (isRangeStat()) {
            throw new IllegalStateException(displayName + " is a range capability; use covers(...) instead");
        }
        return AssemblyRequirement.exactly(this, value);
    }

    /**
     * For scalar stats, requires min <= value <= max.
     * For range stats such as CHEMICAL, requires the material range to cover [min, max].
     */
    public AssemblyRequirement range(double min, double max) {
        return AssemblyRequirement.range(this, min, max);
    }

    /**
     * Explicit coverage spelling for range capabilities such as CHEMICAL_RANGE.
     * The material range must cover the complete requested interval.
     */
    public AssemblyRequirement covers(double min, double max) {
        if (!isRangeStat()) {
            throw new IllegalStateException(displayName + " is not a range capability");
        }
        return AssemblyRequirement.range(this, min, max);
    }

    boolean matchesAtLeast(MaterialProperties properties, double required) {
        if (isRangeStat()) return rangeMax.applyAsDouble(properties) >= required;
        return scalar.applyAsDouble(properties) >= required;
    }

    boolean matchesAtMost(MaterialProperties properties, double required) {
        if (isRangeStat()) return rangeMin.applyAsDouble(properties) <= required;
        return scalar.applyAsDouble(properties) <= required;
    }

    boolean matchesExactly(MaterialProperties properties, double required) {
        if (isRangeStat()) return false;
        return Double.compare(scalar.applyAsDouble(properties), required) == 0;
    }

    boolean matchesRange(MaterialProperties properties, double min, double max) {
        if (isRangeStat()) {
            return rangeMin.applyAsDouble(properties) <= min
                    && rangeMax.applyAsDouble(properties) >= max;
        }
        double value = scalar.applyAsDouble(properties);
        return value >= min && value <= max;
    }
}
