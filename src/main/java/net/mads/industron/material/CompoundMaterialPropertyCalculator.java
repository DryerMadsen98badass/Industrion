package net.mads.industron.material;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.chemistry.ChemicalStructure;
import net.mads.industron.material.chemistry.ChemistryEngine;
import net.mads.industron.material.chemistry.ChemistryPhase;
import net.mads.industron.material.chemistry.CompositionEntry;
import net.mads.industron.material.chemistry.MaterialAnalysis;
import net.mads.industron.material.chemistry.MaterialClassification;
import net.mads.industron.material.chemistry.MaterialSnapshot;
import net.mads.industron.material.chemistry.MaterialSource;
import net.mads.industron.material.structure.StructureMaterial;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves bulk properties for a compound from its component materials and optional chemistry metadata.
 * Atomic identity stays on {@link ElementDefinition}; a compound has no fake atomic number/electron shell.
 */
public final class CompoundMaterialPropertyCalculator {
    public record Result(
            MaterialProperties properties,
            MachineTier tier,
            ChemistryPhase phase,
            Set<MaterialClassification> classifications,
            MaterialAnalysis analysis
    ) {
        public Result {
            classifications = Set.copyOf(classifications);
        }
    }

    private CompoundMaterialPropertyCalculator() {
    }

    /** Resolves properties for any definition type accepted by .contains(...). */
    public static MaterialProperties propertiesFor(IndustrialSubstance substance) {
        return propertiesOf(substance);
    }

    public static Result calculate(
            String id,
            String displayName,
            int color,
            List<MaterialComponent> components,
            Optional<ChemicalStructure> structure,
            Optional<ChemistryPhase> explicitPhase,
            Set<MaterialClassification> explicitClassifications,
            Set<MaterialSource> sources,
            Map<String, Double> propertyOverrides
    ) {
        if (components == null || components.isEmpty()) {
            throw new IllegalArgumentException("Compound material requires at least one .contains(...) component: " + id);
        }

        Map<String, MaterialSnapshot> registry = new LinkedHashMap<>();
        List<CompositionEntry> composition = new ArrayList<>();
        int strongestComponentTier = 0;

        for (MaterialComponent component : components) {
            IndustrialSubstance substance = component.substance();
            MaterialProperties properties = propertiesOf(substance);
            int tierIndex = tierIndexOf(substance);
            strongestComponentTier = Math.max(strongestComponentTier, tierIndex);
            ChemistryPhase phase = phaseOf(properties);

            MaterialSnapshot child = new MaterialSnapshot(
                    substance.id(),
                    substance.displayName(),
                    substance.color(),
                    phase,
                    nestedComposition(substance),
                    Optional.empty(),
                    numericPropertyMap(properties),
                    baseClassifications(properties),
                    tierIndex,
                    MachineTier.ALL.get(tierIndex).id().toUpperCase(Locale.ROOT),
                    Set.of(),
                    backingMaterial(substance)
            );
            registry.put(child.id(), child);
            composition.add(new CompositionEntry(substance.id(), component.amount(), phase));
        }

        Map<String, Double> initialProperties = new LinkedHashMap<>();
        propertyOverrides.forEach((key, value) -> initialProperties.put(normalize(key), value));
        MaterialSnapshot target = new MaterialSnapshot(
                id,
                displayName,
                color,
                explicitPhase.orElse(ChemistryPhase.UNKNOWN),
                composition,
                structure,
                initialProperties,
                new LinkedHashSet<>(explicitClassifications),
                strongestComponentTier,
                MachineTier.ALL.get(strongestComponentTier).id().toUpperCase(Locale.ROOT),
                sources,
                null
        );
        registry.put(id, target);

        MaterialAnalysis analysis = new ChemistryEngine().analyze(target, registry);
        int tierIndex = clamp(analysis.calculatedTierIndex(), 0, MachineTier.ALL.size() - 1);
        MachineTier tier = MachineTier.ALL.get(tierIndex);
        MaterialProperties properties = createBulkProperties(
                components,
                analysis,
                structure,
                tierIndex
        );

        return new Result(properties, tier, analysis.phase(), analysis.classifications(), analysis);
    }

    private static MaterialProperties createBulkProperties(
            List<MaterialComponent> components,
            MaterialAnalysis analysis,
            Optional<ChemicalStructure> structure,
            int tierIndex
    ) {
        try {
            RecordComponent[] fields = MaterialProperties.class.getRecordComponents();
            Class<?>[] parameterTypes = new Class<?>[fields.length];
            Object[] args = new Object[fields.length];
            MaterialProperties dominant = dominantProperties(components);
            Map<String, Double> derived = analysis.properties().values();

            for (int i = 0; i < fields.length; i++) {
                RecordComponent field = fields[i];
                parameterTypes[i] = field.getType();
                args[i] = resolvedFieldValue(field, dominant, components, derived, analysis, structure, tierIndex);
            }

            Constructor<MaterialProperties> constructor = MaterialProperties.class.getDeclaredConstructor(parameterTypes);
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not build compound MaterialProperties for " + analysis.source().id(), e);
        }
    }

    private static Object resolvedFieldValue(
            RecordComponent field,
            MaterialProperties dominant,
            List<MaterialComponent> components,
            Map<String, Double> derived,
            MaterialAnalysis analysis,
            Optional<ChemicalStructure> structure,
            int tierIndex
    ) throws ReflectiveOperationException {
        String name = field.getName();
        String key = normalize(name);
        Class<?> type = field.getType();

        if (name.equals("unavailableProperties")) return Set.of();
        if (name.equals("tierMultiplier")) return tierIndex + 1;
        if (name.equals("protons") || name.equals("neutrons") || name.equals("electrons")
                || name.equals("outerShell") || name.equals("outerShellCapacity")
                || name.equals("outerShellElectrons") || name.equals("stableValenceTarget")
                || name.equals("electronsToStableShell") || name.equals("electronsFromStableShell")
                || name.equals("preferredIonCharge") || name.equals("unpairedElectrons")
                || name.equals("effectiveNuclearCharge") || name.equals("valenceSElectrons")
                || name.equals("valencePElectrons") || name.equals("activeDElectrons")
                || name.equals("activeFElectrons")) {
            return 0;
        }
        if (name.equals("electronShells")) return List.of();
        if (name.equals("baseColor")) return analysis.properties().color();
        if (name.equals("highlightColor")) return adjustColor(analysis.properties().color(), 1.16);
        if (name.equals("shadowColor")) return adjustColor(analysis.properties().color(), 0.72);
        if (name.equals("ambientTemperature")) return 20;
        if (name.equals("castTemperature")) {
            int melting = compoundMeltingPoint(components, derived, analysis, tierIndex);
            return analysis.phase() == ChemistryPhase.SOLID ? Math.max(20, melting + 40) : 20;
        }
        if (name.equals("state")) return switch (analysis.phase()) {
            case GAS, PLASMA -> MaterialProperties.PhysicalState.GAS;
            case LIQUID, MOLTEN -> MaterialProperties.PhysicalState.LIQUID;
            default -> MaterialProperties.PhysicalState.SOLID;
        };
        if (name.equals("electricalBehavior")) {
            return analysis.classifications().contains(MaterialClassification.CONDUCTOR)
                    || analysis.classifications().contains(MaterialClassification.SEMICONDUCTOR)
                    ? MaterialProperties.ElectricalBehavior.CONDUCTOR
                    : MaterialProperties.ElectricalBehavior.INSULATOR;
        }
        if (name.equals("metal")) {
            // A compound is a bulk metal only when the bonding model resolves to a metallic lattice.
            // High metallicity of the ingredients alone must not turn an ionic/network mineral into
            // ingots and plates.
            return analysis.classifications().contains(MaterialClassification.ALLOY);
        }
        if (name.equals("magnetic")) return derived.getOrDefault("magneticstrength", 0.0) >= 20;
        if (name.equals("crystalline")) {
            return analysis.classifications().contains(MaterialClassification.IONIC_COMPOUND)
                    || analysis.classifications().contains(MaterialClassification.ALLOY)
                    || derived.getOrDefault("crystalstability", 0.0) >= 55.0;
        }
        if (name.equals("gemCandidate")) return false;
        if (name.equals("heatResistant")) return derived.getOrDefault("maxoperatingtemperature", weightedNumber(components, "maxOperatingTemperature")) >= 700;
        if (name.equals("pressureResistant")) return derived.getOrDefault("pressureresistance", weightedNumber(components, "pressureResistance")) >= 50;
        if (name.equals("furnaceFuel")) return false;
        if (name.equals("furnaceFuelPotential") || name.equals("furnaceBurnTimeTicks")) return 0;
        if (name.equals("crystalStructure")) return crystalStructure(structure, analysis, dominant);
        if (name.equals("fractureBehavior")) return fractureBehavior(derived, dominant);
        if (name.equals("metallicityClass")) return metallicityClass(derived);
        if (name.equals("electronicFamily")) return invoke(dominant, name);
        if (name.equals("meltingPoint")) {
            return compoundMeltingPoint(components, derived, analysis, tierIndex);
        }
        if (name.equals("boilingPoint")) {
            int melting = compoundMeltingPoint(components, derived, analysis, tierIndex);
            int derivedBoiling = intValue(derived.getOrDefault("boilingpoint", weightedNumber(components, "boilingPoint")));
            return analysis.phase() == ChemistryPhase.SOLID ? Math.max(melting + 80, derivedBoiling) : derivedBoiling;
        }

        if (type == int.class) {
            double value = derived.containsKey(key) ? derived.get(key) : weightedNumber(components, name);
            return intValue(value);
        }
        if (type == double.class) {
            return derived.containsKey(key) ? derived.get(key) : weightedNumber(components, name);
        }
        if (type == boolean.class) {
            return booleanValue(invoke(dominant, name));
        }
        if (type.isEnum()) {
            return invoke(dominant, name);
        }
        if (List.class.isAssignableFrom(type)) return List.of();
        return invoke(dominant, name);
    }

    private static MaterialProperties.CrystalStructure crystalStructure(
            Optional<ChemicalStructure> structure,
            MaterialAnalysis analysis,
            MaterialProperties dominant
    ) {
        if (structure.isPresent()) {
            return switch (structure.get().topology()) {
                case IONIC_LATTICE, METALLIC_LATTICE -> MaterialProperties.CrystalStructure.CUBIC;
                case POLYMER_CHAIN, POLYMER_NETWORK, PHYSICAL_MIXTURE -> MaterialProperties.CrystalStructure.AMORPHOUS;
                case NETWORK -> MaterialProperties.CrystalStructure.IRREGULAR;
                default -> dominant.crystalStructure();
            };
        }
        if (analysis.classifications().contains(MaterialClassification.IONIC_COMPOUND)
                || analysis.classifications().contains(MaterialClassification.ALLOY)) {
            return MaterialProperties.CrystalStructure.CUBIC;
        }
        return dominant.crystalStructure();
    }

    private static MaterialProperties.FractureBehavior fractureBehavior(
            Map<String, Double> derived,
            MaterialProperties dominant
    ) {
        double ductility = derived.getOrDefault("ductility", 0.0);
        double brittleness = derived.getOrDefault("brittleness", 0.0);
        if (ductility >= 55) return MaterialProperties.FractureBehavior.DUCTILE;
        if (brittleness >= 70) return MaterialProperties.FractureBehavior.SHATTERING;
        return dominant.fractureBehavior();
    }

    private static MaterialProperties.MetallicityClass metallicityClass(Map<String, Double> derived) {
        double value = derived.getOrDefault("metalliccharacter", derived.getOrDefault("metallicity", 0.0));
        if (value >= 80) return MaterialProperties.MetallicityClass.STRONGLY_METALLIC;
        if (value >= 55) return MaterialProperties.MetallicityClass.METALLIC;
        if (value >= 25) return MaterialProperties.MetallicityClass.SEMI_METALLIC;
        return MaterialProperties.MetallicityClass.NON_METALLIC;
    }

    private static int compoundMeltingPoint(
            List<MaterialComponent> components,
            Map<String, Double> derived,
            MaterialAnalysis analysis,
            int tierIndex
    ) {
        double intrinsic = derived.getOrDefault("meltingpoint", weightedNumber(components, "meltingPoint"));
        boolean metallicSolid = analysis.phase() == ChemistryPhase.SOLID
                && analysis.classifications().contains(MaterialClassification.ALLOY);
        if (!metallicSolid) {
            return intValue(intrinsic);
        }
        int base = MaterialPropertyCalculator.normalizeMetalMeltingBase(intrinsic);
        return MaterialPropertyCalculator.tierBandedMetalMeltingPoint(base, tierIndex);
    }

    private static MaterialProperties dominantProperties(List<MaterialComponent> components) {
        return components.stream()
                .max(Comparator.comparingInt(MaterialComponent::amount))
                .map(component -> propertiesOf(component.substance()))
                .orElseThrow();
    }

    private static double weightedNumber(List<MaterialComponent> components, String accessor) {
        double total = 0;
        int weight = 0;
        for (MaterialComponent component : components) {
            MaterialProperties componentProperties = propertiesOf(component.substance());
            if (!componentProperties.hasProperty(accessor)) continue;
            Object value = invoke(componentProperties, accessor);
            if (value instanceof Number number) {
                total += number.doubleValue() * component.amount();
                weight += component.amount();
            }
        }
        return weight == 0 ? 0 : total / weight;
    }

    private static Object invoke(MaterialProperties properties, String accessor) {
        try {
            return MaterialProperties.class.getMethod(accessor).invoke(properties);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unknown MaterialProperties accessor " + accessor, e);
        }
    }

    private static Map<String, Double> numericPropertyMap(MaterialProperties properties) {
        Map<String, Double> values = new LinkedHashMap<>();
        for (RecordComponent component : MaterialProperties.class.getRecordComponents()) {
            Class<?> type = component.getType();
            if (type != int.class && type != double.class) continue;
            if (!properties.hasProperty(component.getName())) continue;
            Object value = invoke(properties, component.getName());
            if (value instanceof Number number) values.put(normalize(component.getName()), number.doubleValue());
        }
        values.putIfAbsent("metalliccharacter", (double) properties.metallicity());
        if (properties.hasProperty("electricalConductivity")) {
            values.putIfAbsent("electronmobility", (double) properties.electricalConductivity());
        }
        return values;
    }

    private static Set<MaterialClassification> baseClassifications(MaterialProperties properties) {
        Set<MaterialClassification> result = EnumSet.noneOf(MaterialClassification.class);
        result.add(MaterialClassification.ELEMENT);
        if (properties.hasProperty("electricalConductivity")) {
            if (properties.electricalConductivity() >= 65) result.add(MaterialClassification.CONDUCTOR);
            else if (properties.electricalConductivity() >= 15) result.add(MaterialClassification.SEMICONDUCTOR);
            else result.add(MaterialClassification.INSULATOR);
        }
        return result;
    }

    private static List<CompositionEntry> nestedComposition(IndustrialSubstance substance) {
        List<MaterialComponent> components = componentsOf(substance);
        if (components.isEmpty()) return List.of();
        List<CompositionEntry> result = new ArrayList<>();
        for (MaterialComponent component : components) {
            result.add(new CompositionEntry(
                    component.substance().id(),
                    component.amount(),
                    phaseOf(propertiesOf(component.substance()))
            ));
        }
        return List.copyOf(result);
    }

    private static Object backingMaterial(IndustrialSubstance substance) {
        return substance instanceof IndustrialMaterial material ? material : null;
    }

    private static MaterialProperties propertiesOf(IndustrialSubstance substance) {
        if (substance instanceof net.mads.industron.fluid.IndustrialFluid fluid) {
            return calculate(fluid.id(), fluid.displayName(), fluid.color(), fluid.components(),
                    Optional.empty(), Optional.of(fluid.isGas() ? ChemistryPhase.GAS : ChemistryPhase.LIQUID),
                    Set.of(), Set.of(), Map.of()).properties();
        }
        if (substance instanceof net.mads.industron.material.organism.BiologicalMaterial biological) {
            return calculate(biological.id(), biological.displayName(), biological.color(),
                    biological.components(), Optional.empty(), Optional.empty(), Set.of(), Set.of(), Map.of()).properties();
        }
        if (substance instanceof net.mads.industron.material.organic.OrganicMaterial
                || substance instanceof net.mads.industron.material.plant.PlantMaterial
                || substance instanceof net.mads.industron.material.plant.PlantDerivedSubstance
                || substance instanceof net.mads.industron.material.plant.PlantProcessIntermediate) {
            return calculate(substance.id(), substance.displayName(), substance.color(), componentsOf(substance),
                    Optional.empty(), Optional.empty(), Set.of(), Set.of(), Map.of()).properties();
        }
        if (substance instanceof IndustrialMaterial material) return material.properties();
        if (substance instanceof ElementDefinition element) return MaterialPropertyCalculator.calculate(element);
        if (substance instanceof StructureMaterial structureMaterial) {
            if (structureMaterial.components().isEmpty()) {
                throw new IllegalArgumentException("Structure material used by .contains(...) has no composition: "
                        + structureMaterial.id());
            }
            return calculate(
                    structureMaterial.id(),
                    structureMaterial.displayName(),
                    structureMaterial.color(),
                    structureMaterial.components(),
                    Optional.empty(),
                    Optional.empty(),
                    Set.of(),
                    Set.of(),
                    Map.of()
            ).properties();
        }
        throw new IllegalArgumentException("Unsupported IndustrialSubstance in compound: " + substance.getClass().getName());
    }

    private static int tierIndexOf(IndustrialSubstance substance) {
        if (substance instanceof net.mads.industron.material.organism.BiologicalMaterial
                || substance instanceof net.mads.industron.material.organic.OrganicMaterial
                || substance instanceof net.mads.industron.material.plant.PlantMaterial
                || substance instanceof net.mads.industron.material.plant.PlantDerivedSubstance
                || substance instanceof net.mads.industron.material.plant.PlantProcessIntermediate
                || substance instanceof net.mads.industron.fluid.IndustrialFluid) {
            return MachineTier.ALL.indexOf(net.mads.industron.material.organism.BiologicalProcessingTier.of(substance));
        }
        MachineTier tier = substance instanceof IndustrialMaterial material
                ? material.tier()
                : substance instanceof ElementDefinition element
                ? element.tier()
                : substance instanceof StructureMaterial
                ? MachineTier.ALL.get(clamp(propertiesOf(substance).tierMultiplier() - 1, 0, MachineTier.ALL.size() - 1))
                : MachineTier.ULV;
        int index = MachineTier.ALL.indexOf(tier);
        return index < 0 ? 0 : index;
    }

    private static List<MaterialComponent> componentsOf(IndustrialSubstance substance) {
        if (substance instanceof net.mads.industron.material.organism.BiologicalMaterial biological) return biological.components();
        if (substance instanceof net.mads.industron.fluid.IndustrialFluid fluid) return fluid.components();
        if (substance instanceof net.mads.industron.material.organic.OrganicMaterial organic) return organic.components();
        if (substance instanceof net.mads.industron.material.plant.PlantMaterial plant) return plant.components();
        if (substance instanceof net.mads.industron.material.plant.PlantDerivedSubstance plant) return plant.components();
        if (substance instanceof net.mads.industron.material.plant.PlantProcessIntermediate plant) return plant.components();
        if (substance instanceof IndustrialMaterial material) return material.components();
        if (substance instanceof StructureMaterial structureMaterial) return structureMaterial.components();
        return List.of();
    }

    private static ChemistryPhase phaseOf(MaterialProperties properties) {
        return switch (properties.state()) {
            case GAS -> ChemistryPhase.GAS;
            case LIQUID -> ChemistryPhase.LIQUID;
            case SOLID -> ChemistryPhase.SOLID;
        };
    }

    private static int adjustColor(int rgb, double multiplier) {
        int r = clamp((int) Math.round(((rgb >> 16) & 0xFF) * multiplier), 0, 255);
        int g = clamp((int) Math.round(((rgb >> 8) & 0xFF) * multiplier), 0, 255);
        int b = clamp((int) Math.round((rgb & 0xFF) * multiplier), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private static int intValue(double value) {
        if (!Double.isFinite(value)) return 0;
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (value < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) Math.round(value);
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean bool && bool;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String normalize(String value) {
        return value.replace("_", "").toLowerCase(Locale.ROOT);
    }
}
