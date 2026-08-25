package net.mads.industron.recipe.recipetypes;

import net.mads.industron.material.MaterialProperties;

import java.util.List;

/**
 * Readable entry point for material stat requirements in recipes.
 *
 * <pre>
 * .input(Component.PLATE, 8)
 *     .stat(Stats.STRUCTURAL_STRENGTH).atLeast(50)
 *     .stat(Stats.PRESSURE_RESISTANCE).atLeast(100)
 * .input(Material.ROD, Metal.VERNIUM, 2)
 *     .stat(Stats.HARDNESS).range(100, 250)
 * </pre>
 */
public final class Stats {
    public static final AssemblyCapability TIER_MULTIPLIER = AssemblyCapability.TIER_MULTIPLIER;
    public static final AssemblyCapability PROTONS = AssemblyCapability.PROTONS;
    public static final AssemblyCapability NEUTRONS = AssemblyCapability.NEUTRONS;
    public static final AssemblyCapability ELECTRONS = AssemblyCapability.ELECTRONS;
    public static final AssemblyCapability OUTER_SHELL = AssemblyCapability.OUTER_SHELL;
    public static final AssemblyCapability OUTER_SHELL_CAPACITY = AssemblyCapability.OUTER_SHELL_CAPACITY;
    public static final AssemblyCapability OUTER_SHELL_ELECTRONS = AssemblyCapability.OUTER_SHELL_ELECTRONS;
    public static final AssemblyCapability STABLE_VALENCE_TARGET = AssemblyCapability.STABLE_VALENCE_TARGET;
    public static final AssemblyCapability ELECTRONS_TO_STABLE_SHELL = AssemblyCapability.ELECTRONS_TO_STABLE_SHELL;
    public static final AssemblyCapability ELECTRONS_FROM_STABLE_SHELL = AssemblyCapability.ELECTRONS_FROM_STABLE_SHELL;
    public static final AssemblyCapability PREFERRED_ION_CHARGE = AssemblyCapability.PREFERRED_ION_CHARGE;
    public static final AssemblyCapability UNPAIRED_ELECTRONS = AssemblyCapability.UNPAIRED_ELECTRONS;
    public static final AssemblyCapability IONIZATION_ENERGY = AssemblyCapability.IONIZATION_ENERGY;
    public static final AssemblyCapability ELECTRON_AFFINITY = AssemblyCapability.ELECTRON_AFFINITY;
    public static final AssemblyCapability ELECTRON_DONATION_TENDENCY = AssemblyCapability.ELECTRON_DONATION_TENDENCY;
    public static final AssemblyCapability ELECTRON_ACCEPTANCE_TENDENCY = AssemblyCapability.ELECTRON_ACCEPTANCE_TENDENCY;
    public static final AssemblyCapability BOND_STRENGTH = AssemblyCapability.BOND_STRENGTH;
    public static final AssemblyCapability BOND_ENERGY = AssemblyCapability.BOND_ENERGY;
    public static final AssemblyCapability ATOMIC_STABILITY = AssemblyCapability.ATOMIC_STABILITY;
    public static final AssemblyCapability EFFECTIVE_NUCLEAR_CHARGE = AssemblyCapability.EFFECTIVE_NUCLEAR_CHARGE;
    public static final AssemblyCapability ATOMIC_RADIUS = AssemblyCapability.ATOMIC_RADIUS;
    public static final AssemblyCapability VALENCE_S_ELECTRONS = AssemblyCapability.VALENCE_S_ELECTRONS;
    public static final AssemblyCapability VALENCE_P_ELECTRONS = AssemblyCapability.VALENCE_P_ELECTRONS;
    public static final AssemblyCapability ACTIVE_D_ELECTRONS = AssemblyCapability.ACTIVE_D_ELECTRONS;
    public static final AssemblyCapability ACTIVE_F_ELECTRONS = AssemblyCapability.ACTIVE_F_ELECTRONS;

    public static final AssemblyCapability DENSITY = AssemblyCapability.DENSITY;
    public static final AssemblyCapability HARDNESS = AssemblyCapability.HARDNESS;
    public static final AssemblyCapability ELASTICITY = AssemblyCapability.ELASTICITY;
    public static final AssemblyCapability TENSILE_STRENGTH = AssemblyCapability.TENSILE_STRENGTH;
    public static final AssemblyCapability YIELD_STRENGTH = AssemblyCapability.YIELD_STRENGTH;
    public static final AssemblyCapability FRACTURE_TOUGHNESS = AssemblyCapability.FRACTURE_TOUGHNESS;
    public static final AssemblyCapability COMPRESSIVE_STRENGTH = AssemblyCapability.COMPRESSIVE_STRENGTH;
    public static final AssemblyCapability DUCTILITY = AssemblyCapability.DUCTILITY;
    public static final AssemblyCapability BRITTLENESS = AssemblyCapability.BRITTLENESS;
    public static final AssemblyCapability WEAR_RESISTANCE = AssemblyCapability.WEAR_RESISTANCE;
    public static final AssemblyCapability FATIGUE_RESISTANCE = AssemblyCapability.FATIGUE_RESISTANCE;
    public static final AssemblyCapability STRUCTURAL_STRENGTH = AssemblyCapability.STRUCTURAL_STRENGTH;
    public static final AssemblyCapability STRUCTURAL_LOAD = AssemblyCapability.STRUCTURAL_LOAD;
    public static final AssemblyCapability SHAFT_LOAD = AssemblyCapability.SHAFT_LOAD;
    public static final AssemblyCapability FASTENER_LOAD = AssemblyCapability.FASTENER_LOAD;

    public static final AssemblyCapability MELTING_POINT = AssemblyCapability.MELTING_POINT;
    public static final AssemblyCapability BOILING_POINT = AssemblyCapability.BOILING_POINT;
    public static final AssemblyCapability THERMAL_CONDUCTIVITY = AssemblyCapability.THERMAL_CONDUCTIVITY;
    public static final AssemblyCapability SPECIFIC_HEAT_CAPACITY = AssemblyCapability.SPECIFIC_HEAT_CAPACITY;
    public static final AssemblyCapability THERMAL_EXPANSION = AssemblyCapability.THERMAL_EXPANSION;
    public static final AssemblyCapability MAX_OPERATING_TEMPERATURE = AssemblyCapability.MAX_OPERATING_TEMPERATURE;
    public static final AssemblyCapability THERMAL_SHOCK_RESISTANCE = AssemblyCapability.THERMAL_SHOCK_RESISTANCE;

    public static final AssemblyCapability ELECTRICAL_CONDUCTIVITY = AssemblyCapability.ELECTRICAL_CONDUCTIVITY;
    public static final AssemblyCapability ELECTRICAL_CAPACITY = AssemblyCapability.ELECTRICAL_CAPACITY;
    public static final AssemblyCapability INSULATION_STRENGTH = AssemblyCapability.INSULATION_STRENGTH;
    public static final AssemblyCapability INSULATION_CAPACITY = AssemblyCapability.INSULATION_CAPACITY;
    public static final AssemblyCapability ELECTROCHEMICAL_POTENTIAL = AssemblyCapability.ELECTROCHEMICAL_POTENTIAL;
    public static final AssemblyCapability CHARGE_STORAGE_POTENTIAL = AssemblyCapability.CHARGE_STORAGE_POTENTIAL;
    public static final AssemblyCapability BATTERY_POTENTIAL = AssemblyCapability.BATTERY_POTENTIAL;

    public static final AssemblyCapability CORROSION_RESISTANCE = AssemblyCapability.CORROSION_RESISTANCE;
    public static final AssemblyCapability CHEMICAL_STABILITY = AssemblyCapability.CHEMICAL_STABILITY;
    public static final AssemblyCapability REACTIVITY = AssemblyCapability.REACTIVITY;
    public static final AssemblyCapability OXIDATION_RESISTANCE = AssemblyCapability.OXIDATION_RESISTANCE;
    public static final AssemblyCapability ACIDITY = AssemblyCapability.ACIDITY;
    public static final AssemblyCapability MIN_CHEMICAL_RANGE = AssemblyCapability.MIN_CHEMICAL_RANGE;
    public static final AssemblyCapability MAX_CHEMICAL_RANGE = AssemblyCapability.MAX_CHEMICAL_RANGE;
    public static final AssemblyCapability CHEMICAL_RANGE = AssemblyCapability.CHEMICAL;

    public static final AssemblyCapability PRESSURE_RESISTANCE = AssemblyCapability.PRESSURE_RESISTANCE;
    public static final AssemblyCapability MAX_PRESSURE = AssemblyCapability.MAX_PRESSURE;
    public static final AssemblyCapability PRESSURE_CAPACITY = AssemblyCapability.PRESSURE_CAPACITY;

    public static final AssemblyCapability MAGNETIC_TENDENCY = AssemblyCapability.MAGNETIC_TENDENCY;
    public static final AssemblyCapability MAGNETIC_STRENGTH = AssemblyCapability.MAGNETIC_STRENGTH;

    public static final AssemblyCapability MACHINABILITY = AssemblyCapability.MACHINABILITY;
    public static final AssemblyCapability FORMABILITY = AssemblyCapability.FORMABILITY;
    public static final AssemblyCapability WELDABILITY = AssemblyCapability.WELDABILITY;
    public static final AssemblyCapability CASTABILITY = AssemblyCapability.CASTABILITY;

    public static final AssemblyCapability CRYSTAL_STABILITY = AssemblyCapability.CRYSTAL_STABILITY;
    public static final AssemblyCapability TRANSPARENCY = AssemblyCapability.TRANSPARENCY;
    public static final AssemblyCapability REFRACTIVE_INDEX = AssemblyCapability.REFRACTIVE_INDEX;
    public static final AssemblyCapability LUSTER = AssemblyCapability.LUSTER;
    public static final AssemblyCapability CLEAVAGE = AssemblyCapability.CLEAVAGE;
    public static final AssemblyCapability CRYSTAL_HARDNESS = AssemblyCapability.CRYSTAL_HARDNESS;
    public static final AssemblyCapability IMPURITY_TOLERANCE = AssemblyCapability.IMPURITY_TOLERANCE;
    public static final AssemblyCapability OPTICAL_PURITY = AssemblyCapability.OPTICAL_PURITY;
    public static final AssemblyCapability CRYSTAL_GROWTH_DIFFICULTY = AssemblyCapability.CRYSTAL_GROWTH_DIFFICULTY;
    public static final AssemblyCapability CRYSTAL_FORMATION_TEMPERATURE = AssemblyCapability.CRYSTAL_FORMATION_TEMPERATURE;
    public static final AssemblyCapability CRYSTAL_FORMATION_PRESSURE = AssemblyCapability.CRYSTAL_FORMATION_PRESSURE;
    public static final AssemblyCapability GEM_QUALITY = AssemblyCapability.GEM_QUALITY;

    public static final AssemblyCapability BASE_COLOR = AssemblyCapability.BASE_COLOR;
    public static final AssemblyCapability HIGHLIGHT_COLOR = AssemblyCapability.HIGHLIGHT_COLOR;
    public static final AssemblyCapability SHADOW_COLOR = AssemblyCapability.SHADOW_COLOR;
    public static final AssemblyCapability BRIGHTNESS = AssemblyCapability.BRIGHTNESS;
    public static final AssemblyCapability EMISSIVE_STRENGTH = AssemblyCapability.EMISSIVE_STRENGTH;
    public static final AssemblyCapability METALLICITY = AssemblyCapability.METALLICITY;
    public static final AssemblyCapability AMBIENT_TEMPERATURE = AssemblyCapability.AMBIENT_TEMPERATURE;
    public static final AssemblyCapability CAST_TEMPERATURE = AssemblyCapability.CAST_TEMPERATURE;
    public static final AssemblyCapability RADIOACTIVITY = AssemblyCapability.RADIOACTIVITY;
    public static final AssemblyCapability FURNACE_FUEL_POTENTIAL = AssemblyCapability.FURNACE_FUEL_POTENTIAL;
    public static final AssemblyCapability FURNACE_BURN_TIME_TICKS = AssemblyCapability.FURNACE_BURN_TIME_TICKS;

    public static final AssemblyCapability FRICTION_COEFFICIENT = AssemblyCapability.FRICTION_COEFFICIENT;
    public static final AssemblyCapability PIPE_CAPABILITY_SCORE = AssemblyCapability.PIPE_CAPABILITY_SCORE;
    public static final AssemblyCapability PIPE_THROUGHPUT = AssemblyCapability.PIPE_THROUGHPUT;
    public static final AssemblyCapability PUMP_FLOW_SCORE = AssemblyCapability.PUMP_FLOW_SCORE;
    public static final AssemblyCapability PUMP_FLOW_RATE = AssemblyCapability.PUMP_FLOW_RATE;
    public static final AssemblyCapability PUMP_STRESS_IMPACT = AssemblyCapability.PUMP_STRESS_IMPACT;
    public static final AssemblyCapability MAX_FLUID_TEMPERATURE = AssemblyCapability.MAX_FLUID_TEMPERATURE;
    public static final AssemblyCapability TANK_CAPABILITY_SCORE = AssemblyCapability.TANK_CAPABILITY_SCORE;
    public static final AssemblyCapability TANK_CAPACITY = AssemblyCapability.TANK_CAPACITY;


    // Non-numeric MaterialProperties. These use typed equality through .is(...).
    public static final AssemblyProperty<List<Integer>> ELECTRON_SHELLS =
            new AssemblyProperty<>("Electron Shells", MaterialProperties::electronShells);
    public static final AssemblyProperty<MaterialProperties.ElectronicFamily> ELECTRONIC_FAMILY =
            new AssemblyProperty<>("Electronic Family", MaterialProperties::electronicFamily);
    public static final AssemblyProperty<MaterialProperties.ElectricalBehavior> ELECTRICAL_BEHAVIOR =
            new AssemblyProperty<>("Electrical Behavior", MaterialProperties::electricalBehavior);
    public static final AssemblyProperty<MaterialProperties.CrystalStructure> CRYSTAL_STRUCTURE =
            new AssemblyProperty<>("Crystal Structure", MaterialProperties::crystalStructure);
    public static final AssemblyProperty<MaterialProperties.FractureBehavior> FRACTURE_BEHAVIOR =
            new AssemblyProperty<>("Fracture Behavior", MaterialProperties::fractureBehavior);
    public static final AssemblyProperty<MaterialProperties.PhysicalState> STATE =
            new AssemblyProperty<>("State", MaterialProperties::state);
    public static final AssemblyProperty<MaterialProperties.MetallicityClass> METALLICITY_CLASS =
            new AssemblyProperty<>("Metallicity Class", MaterialProperties::metallicityClass);

    public static final AssemblyProperty<Boolean> METAL =
            new AssemblyProperty<>("Metal", MaterialProperties::metal);
    public static final AssemblyProperty<Boolean> MAGNETIC =
            new AssemblyProperty<>("Magnetic", MaterialProperties::magnetic);
    public static final AssemblyProperty<Boolean> CRYSTALLINE =
            new AssemblyProperty<>("Crystalline", MaterialProperties::crystalline);
    public static final AssemblyProperty<Boolean> GEM_CANDIDATE =
            new AssemblyProperty<>("Gem Candidate", MaterialProperties::gemCandidate);
    public static final AssemblyProperty<Boolean> HEAT_RESISTANT =
            new AssemblyProperty<>("Heat Resistant", MaterialProperties::heatResistant);
    public static final AssemblyProperty<Boolean> PRESSURE_RESISTANT =
            new AssemblyProperty<>("Pressure Resistant", MaterialProperties::pressureResistant);
    public static final AssemblyProperty<Boolean> FURNACE_FUEL =
            new AssemblyProperty<>("Furnace Fuel", MaterialProperties::furnaceFuel);

    public static final AssemblyProperty<Boolean> ELECTRICALLY_CONDUCTIVE =
            new AssemblyProperty<>("Electrically Conductive", MaterialProperties::electricallyConductive);
    public static final AssemblyProperty<Boolean> ELECTRICALLY_INSULATING =
            new AssemblyProperty<>("Electrically Insulating", MaterialProperties::electricallyInsulating);

    private Stats() {
    }
}
