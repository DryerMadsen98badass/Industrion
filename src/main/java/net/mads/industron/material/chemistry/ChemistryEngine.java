package net.mads.industron.material.chemistry;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.MaterialPropertyCalculator;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Generic chemistry/material-property resolver for compounds.
 *
 * <p>Element properties remain owned by the existing atomic/material calculator. This engine blends
 * those intrinsic values, applies bond/structure effects, classifies the resulting material and then
 * derives the compound tier. No named material pair is special-cased here.</p>
 */
public final class ChemistryEngine {
    private static final List<String> BLENDED_PROPERTIES = List.of(
            "density", "hardness", "structuralstrength", "tensilestrength", "yieldstrength",
            "compressivestrength", "fracturetoughness", "ductility", "elasticity", "brittleness",
            "wearresistance", "fatigueresistance", "meltingpoint", "boilingpoint",
            "electricalconductivity", "insulationstrength", "thermalconductivity", "specificheatcapacity",
            "thermalexpansion", "maxoperatingtemperature", "thermalshockresistance",
            "chemicalstability", "corrosionresistance", "reactivity", "oxidationresistance", "acidity",
            "pressureresistance", "maxpressure", "machinability", "formability", "weldability",
            "castability", "crystalstability", "transparency", "refractiveindex", "luster", "cleavage",
            "crystalhardness", "impuritytolerance", "opticalpurity", "gemquality",
            "polarity", "catalyticactivity", "viscosity", "volatility", "atomicradius", "metallicity",
            "electronmobility", "bondstrength", "bondenergy", "electrondonationtendency",
            "electronacceptancetendency", "electrochemicalpotential", "chargestoragepotential",
            "batterypotential", "magnetictendency", "magneticstrength", "radioactivity"
    );

    public MaterialAnalysis analyze(MaterialSnapshot material, Map<String, MaterialSnapshot> registry) {
        // From this point onward every classifier/effect/ProcessPlan sees the same explicit-or-generated graph.
        material = EffectiveChemicalStructure.apply(material, registry);
        List<ChemistryDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Double> values = new LinkedHashMap<>(material.properties());
        Set<MaterialClassification> kinds = EnumSet.noneOf(MaterialClassification.class);
        kinds.addAll(material.classifications());

        if (material.composition().isEmpty()) {
            deriveMissingBaseProperties(values);
            kinds.add(MaterialClassification.ELEMENT);
        } else {
            // Blend real component properties first. Derived chemistry-only values (for example
            // polarity/catalytic activity) must be calculated from that blend afterwards; deriving
            // them before composition would freeze missing values at zero and prevent correct routes.
            blendComponents(material, registry, values, diagnostics);
            deriveMissingBaseProperties(values);
            classify(material, registry, values, kinds, diagnostics);
            applyStructureEffects(material, registry, values, kinds, diagnostics);
        }

        normalizeDerivedProperties(values, kinds);
        ChemistryPhase phase = inferPhase(material, values, kinds);
        int color = material.color() >= 0 ? material.color() & 0xFFFFFF : blendColor(material, registry);
        boolean elemental = material.composition().isEmpty() && kinds.contains(MaterialClassification.ELEMENT);
        int tier = elemental
                ? Math.max(0, material.tierIndex())
                : calculateTier(values, kinds, material.tierIndex());
        String tierName = elemental && material.tierName() != null && !material.tierName().isBlank()
                ? material.tierName()
                : tierName(tier);

        if (!material.naturallyAcquired()
                && !material.hasSource(MaterialSourceType.PROCESS_OUTPUT)
                && !material.hasSource(MaterialSourceType.PROCESS_BYPRODUCT)
                && !material.hasSource(MaterialSourceType.CHEMICAL_SYNTHESIS)
                && material.composition().isEmpty()) {
            diagnostics.add(new ChemistryDiagnostic(
                    ChemistryStatus.OK_WITH_REQUIREMENTS,
                    material.id(),
                    "NO_ACQUISITION_SOURCE",
                    "Material has no discoverable acquisition source yet.",
                    List.of("Register a deposit, external mapping, biological source, process output, or synthesis route."),
                    ""
            ));
        }

        return new MaterialAnalysis(
                material,
                new DerivedMaterialProperties(values, color),
                Set.copyOf(kinds),
                phase,
                tier,
                tierName,
                diagnostics
        );
    }

    private static void deriveMissingBaseProperties(Map<String, Double> values) {
        double donation = values.getOrDefault("electrondonationtendency", 0.0);
        double acceptance = values.getOrDefault("electronacceptancetendency", 0.0);
        double reactivity = values.getOrDefault("reactivity", 0.0);
        double stability = values.getOrDefault("chemicalstability", 50.0);
        double conductivity = values.getOrDefault("electricalconductivity", 0.0);
        double bond = values.getOrDefault("bondstrength", 0.0);
        double melting = values.getOrDefault("meltingpoint", 20.0);
        double boiling = values.getOrDefault("boilingpoint", 100.0);

        values.putIfAbsent("polarity", clamp100(Math.abs(acceptance - donation)));
        values.putIfAbsent("electronmobility", clamp100(conductivity > 0 ? conductivity : donation * 0.55 + (100 - stability) * 0.20));
        values.putIfAbsent("metalliccharacter", clamp100(values.getOrDefault("metallicity", conductivity)));
        values.putIfAbsent("catalyticactivity", clamp100(
                reactivity * 0.38
                        + Math.max(donation, acceptance) * 0.28
                        + values.getOrDefault("electronmobility", 0.0) * 0.18
                        + Math.max(0.0, 100.0 - stability) * 0.16
        ));
        values.putIfAbsent("volatility", clamp100(
                50.0
                        + Math.max(0.0, 20.0 - boiling) * 0.35
                        - Math.max(0.0, boiling - 20.0) * 0.035
                        - bond * 0.15
        ));
        values.putIfAbsent("viscosity", clamp100(
                Math.max(0.0, bond * 0.35 + stability * 0.20 - values.getOrDefault("volatility", 0.0) * 0.25)
        ));
        values.putIfAbsent("phasebias", melting <= 20 && boiling > 20 ? 50.0 : boiling <= 20 ? 100.0 : 0.0);
    }

    private static void blendComponents(
            MaterialSnapshot material,
            Map<String, MaterialSnapshot> registry,
            Map<String, Double> values,
            List<ChemistryDiagnostic> diagnostics
    ) {
        int total = material.composition().stream().mapToInt(CompositionEntry::amount).sum();
        if (total <= 0) return;

        for (String property : BLENDED_PROPERTIES) {
            double weighted = 0;
            int found = 0;
            for (CompositionEntry component : material.composition()) {
                MaterialSnapshot child = registry.get(component.substanceId());
                if (child == null) continue;
                Double childValue = child.properties().get(property);
                if (childValue == null || !Double.isFinite(childValue)) continue;
                weighted += childValue * component.amount();
                found += component.amount();
            }
            if (found > 0 && !values.containsKey(property)) {
                values.put(property, weighted / found);
            }
        }

        for (CompositionEntry component : material.composition()) {
            if (!registry.containsKey(component.substanceId())) {
                diagnostics.add(new ChemistryDiagnostic(
                        ChemistryStatus.MISSING_MATERIAL,
                        material.id(),
                        "UNKNOWN_COMPONENT",
                        "Composition references missing material " + component.substanceId() + ".",
                        List.of("Create or register the missing IndustrialMaterial before generating recipes."),
                        suggestedMaterial(component.substanceId())
                ));
            }
        }
    }

    private static void classify(
            MaterialSnapshot material,
            Map<String, MaterialSnapshot> registry,
            Map<String, Double> values,
            Set<MaterialClassification> kinds,
            List<ChemistryDiagnostic> diagnostics
    ) {
        ChemicalStructure.Topology topology = ChemicalTopologyResolver.resolve(material, registry);

        if (topology == ChemicalStructure.Topology.PHYSICAL_MIXTURE) {
            kinds.add(MaterialClassification.PHYSICAL_MIXTURE);
        } else if (topology == ChemicalStructure.Topology.METALLIC_LATTICE) {
            kinds.add(MaterialClassification.ALLOY);
        } else if (topology == ChemicalStructure.Topology.IONIC_LATTICE) {
            kinds.add(MaterialClassification.IONIC_COMPOUND);
        } else if (topology == ChemicalStructure.Topology.POLYMER_CHAIN
                || topology == ChemicalStructure.Topology.POLYMER_NETWORK) {
            kinds.add(MaterialClassification.POLYMER);
        } else if (topology == ChemicalStructure.Topology.DISCRETE_MOLECULE
                || topology == ChemicalStructure.Topology.NETWORK) {
            kinds.add(MaterialClassification.MOLECULAR_COMPOUND);
        }

        double acidity = signed100(values.getOrDefault("acidity", 0.0));
        values.put("acidity", acidity);
        if (acidity >= 1) kinds.add(MaterialClassification.ACID);
        else if (acidity <= -1) kinds.add(MaterialClassification.BASE);

        if (values.getOrDefault("catalyticactivity", 0.0) >= 50) {
            kinds.add(MaterialClassification.CATALYST);
        }

        double conductivity = values.getOrDefault("electricalconductivity", 0.0);
        double insulation = values.getOrDefault("insulationstrength", 0.0);
        if (conductivity >= 65) kinds.add(MaterialClassification.CONDUCTOR);
        else if (conductivity >= 15) kinds.add(MaterialClassification.SEMICONDUCTOR);
        else if (insulation > 0 || conductivity < 15) kinds.add(MaterialClassification.INSULATOR);
    }

    private static void applyStructureEffects(
            MaterialSnapshot material,
            Map<String, MaterialSnapshot> registry,
            Map<String, Double> values,
            Set<MaterialClassification> kinds,
            List<ChemistryDiagnostic> diagnostics
    ) {
        if (kinds.contains(MaterialClassification.ALLOY)) {
            double meanRadius = weightedComponentProperty(material, registry, "atomicradius");
            double variance = weightedVariance(material, registry, "atomicradius", meanRadius);
            double mismatch = Math.sqrt(Math.max(0, variance));
            double compatibility = clamp100(100 - mismatch);
            double electronMobility = weightedComponentProperty(material, registry, "electronmobility");

            values.put("structuralstrength", nonNegative(values.getOrDefault("structuralstrength", 0.0)
                    + 10 + Math.min(35, mismatch * 2.5)));
            values.put("hardness", nonNegative(values.getOrDefault("hardness", 0.0)
                    + 8 + Math.min(30, mismatch * 2.0)));
            values.put("ductility", clamp100(values.getOrDefault("ductility", 0.0)
                    - Math.min(28, mismatch * 1.7)));
            values.put("electricalconductivity", nonNegative(
                    values.getOrDefault("electricalconductivity", 0.0) * 0.65
                            + electronMobility * 0.25
                            + compatibility * 0.10
            ));
            values.put("metalliccharacter", clamp100(Math.max(
                    values.getOrDefault("metalliccharacter", 0.0),
                    55 + compatibility * 0.35
            )));
        }

        Optional<ChemicalStructure> effectiveStructure = material.structure();
        validateStructureComposition(material, registry, diagnostics);
        if (material.structure().isPresent()) {
            diagnostics.addAll(ChemicalStructureValidator.validate(material, registry).diagnostics());
        }

        if (effectiveStructure.isPresent()
                && effectiveStructure.get().topology() == ChemicalStructure.Topology.DISCRETE_MOLECULE) {
            applyMolecularThermalEffects(material, registry, values, effectiveStructure.get());
        }

        if (kinds.contains(MaterialClassification.POLYMER)) {
            ChemicalStructure structure = effectiveStructure.orElse(
                    ChemicalStructure.builder(ChemicalStructure.Topology.POLYMER_CHAIN)
                            .repeatUnit(material.id(), 1)
                            .chainFlexibility(0.5)
                            .crosslinkDensity(0.1)
                            .build()
            );
            double flexibility = structure.chainFlexibility();
            double crosslinks = structure.crosslinkDensity();
            double mobility = values.getOrDefault("electronmobility", 0.0);

            values.put("insulationstrength", nonNegative(Math.max(
                    values.getOrDefault("insulationstrength", 0.0),
                    55 + 35 * (1 - clamp100(mobility) / 100.0)
            )));
            values.put("electricalconductivity", nonNegative(values.getOrDefault("electricalconductivity", 0.0) * 0.25));
            values.put("elasticity", clamp100(values.getOrDefault("elasticity", 0.0) + flexibility * 55 - crosslinks * 15));
            values.put("structuralstrength", nonNegative(values.getOrDefault("structuralstrength", 0.0) + crosslinks * 45));
            values.put("thermalconductivity", nonNegative(values.getOrDefault("thermalconductivity", 0.0) * 0.55));
            values.put("chainflexibility", flexibility * 100.0);
            values.put("crosslinkdensity", crosslinks * 100.0);

            if (flexibility >= 0.55 && crosslinks < 0.45) kinds.add(MaterialClassification.ELASTOMER);
            if (crosslinks >= 0.55) kinds.add(MaterialClassification.THERMOSET);
            else kinds.add(MaterialClassification.THERMOPLASTIC);
        }

        if (kinds.contains(MaterialClassification.IONIC_COMPOUND)) {
            values.put("electricalconductivity", nonNegative(values.getOrDefault("electricalconductivity", 0.0) * 0.25));
            values.put("chemicalstability", nonNegative(values.getOrDefault("chemicalstability", 0.0) + 12));
            values.put("hardness", nonNegative(values.getOrDefault("hardness", 0.0) + 8));
        }

        effectiveStructure.ifPresent(structure -> {
            if (structure.netCharge() != 0 && !kinds.contains(MaterialClassification.IONIC_COMPOUND)) {
                diagnostics.add(new ChemistryDiagnostic(
                        ChemistryStatus.CHANGE_REQUIRED,
                        material.id(),
                        "UNBALANCED_CHARGE",
                        "Effective structure has net charge " + structure.netCharge() + ".",
                        List.of("Balance formal charges or model the material as an ionic compound."),
                        ""
                ));
            }
        });
    }

    private static void normalizeDerivedProperties(Map<String, Double> values, Set<MaterialClassification> kinds) {
        // Tier-banded physical capabilities are intentionally open-ended. Element properties already
        // grow by MaterialPropertyCalculator.DEFAULT_TIER_BAND_SIZE per tier, so clamping them to 100
        // would collapse IV/LuV/ZPM/UV/... into the same value and break casing/tier qualification.
        for (String key : List.of(
                "hardness", "structuralstrength", "tensilestrength", "yieldstrength", "compressivestrength",
                "fracturetoughness", "wearresistance", "fatigueresistance", "electricalconductivity",
                "insulationstrength", "thermalconductivity", "thermalshockresistance", "chemicalstability",
                "corrosionresistance", "oxidationresistance", "pressureresistance", "maxpressure",
                "magneticstrength", "crystalhardness", "bondstrength", "bondenergy"
        )) {
            if (values.containsKey(key)) values.put(key, nonNegative(values.get(key)));
        }

        // These are normalized chemistry axes/ratios, not tier-banded engineering capabilities.
        for (String key : List.of(
                "ductility", "elasticity", "brittleness", "reactivity", "polarity", "catalyticactivity",
                "viscosity", "volatility", "metalliccharacter", "electronmobility", "chainflexibility",
                "crosslinkdensity", "molecularassociation", "bondpolarity"
        )) {
            if (values.containsKey(key)) values.put(key, clamp100(values.get(key)));
        }
        if (values.containsKey("acidity")) values.put("acidity", signed100(values.get("acidity")));

        if (kinds.contains(MaterialClassification.CONDUCTOR)) {
            values.put("insulationstrength", Math.min(values.getOrDefault("insulationstrength", 0.0), 20.0));
        }
    }

    private static ChemistryPhase inferPhase(
            MaterialSnapshot material,
            Map<String, Double> values,
            Set<MaterialClassification> kinds
    ) {
        if (material.phase() != ChemistryPhase.UNKNOWN) return material.phase();

        if (kinds.contains(MaterialClassification.PHYSICAL_MIXTURE)) {
            ChemistryPhase mixturePhase = inferPhysicalMixturePhase(material);
            if (mixturePhase != ChemistryPhase.UNKNOWN) return mixturePhase;
        }

        if (kinds.contains(MaterialClassification.ALLOY)
                || kinds.contains(MaterialClassification.POLYMER)
                || kinds.contains(MaterialClassification.IONIC_COMPOUND)) {
            return ChemistryPhase.SOLID;
        }

        double boiling = values.getOrDefault("boilingpoint", Double.NaN);
        double melting = values.getOrDefault("meltingpoint", Double.NaN);

        // Thermal transition points are authoritative whenever they are known. Viscosity describes
        // how a fluid flows; it must never turn a material whose melting point is above ambient
        // temperature into a liquid. This is especially important for network/organic compounds.
        if (Double.isFinite(boiling) && boiling <= 20) return ChemistryPhase.GAS;
        if (Double.isFinite(melting) && melting > 20) return ChemistryPhase.SOLID;
        if (Double.isFinite(melting) && melting <= 20
                && Double.isFinite(boiling) && boiling > 20) {
            return ChemistryPhase.LIQUID;
        }

        double volatility = values.getOrDefault("volatility", 0.0);
        if (volatility >= 72) return ChemistryPhase.GAS;
        if (Double.isFinite(melting) && melting <= 20) return ChemistryPhase.LIQUID;
        if (volatility >= 22) return ChemistryPhase.LIQUID;
        return ChemistryPhase.SOLID;
    }

    /**
     * Physical mixtures keep the phase of their continuous/dominant phase. Composition ratios matter here:
     * four liquid units plus one solid unit is still a liquid mixture. This rule is intentionally limited to
     * PHYSICAL_MIXTURE; a chemically bonded compound gets a new phase from its own bulk properties.
     */
    private static ChemistryPhase inferPhysicalMixturePhase(MaterialSnapshot material) {
        int total = material.composition().stream().mapToInt(CompositionEntry::amount).sum();
        if (total <= 0) return ChemistryPhase.UNKNOWN;

        int gas = 0;
        int liquid = 0;
        int solid = 0;
        for (CompositionEntry component : material.composition()) {
            switch (component.phase()) {
                case GAS -> gas += component.amount();
                case LIQUID, MOLTEN -> liquid += component.amount();
                case SOLID -> solid += component.amount();
                default -> { }
            }
        }

        if (gas * 2 > total) return ChemistryPhase.GAS;
        if (liquid * 2 > total) return ChemistryPhase.LIQUID;
        if (solid * 2 > total) return ChemistryPhase.SOLID;

        // No strict majority: prefer a liquid continuous phase over suspended solids, then gas,
        // otherwise let thermal-property inference decide.
        if (liquid > 0 && liquid >= solid && liquid >= gas) return ChemistryPhase.LIQUID;
        if (gas > 0 && gas > solid) return ChemistryPhase.GAS;

        int representedPhases = (gas > 0 ? 1 : 0) + (liquid > 0 ? 1 : 0) + (solid > 0 ? 1 : 0);
        if (representedPhases > 1) return ChemistryPhase.MIXED;
        return ChemistryPhase.UNKNOWN;
    }

    /**
     * Molecular compounds do not inherit elemental melting/boiling points by simple averaging.
     * New covalent/coordinate bonds create a new substance. This generic model derives molecular
     * thermal behaviour from molecular mass, bond polarity, bond strength and intermolecular
     * association potential. No named element or material pair is special-cased.
     */
    private static void applyMolecularThermalEffects(
            MaterialSnapshot material,
            Map<String, MaterialSnapshot> registry,
            Map<String, Double> values,
            ChemicalStructure structure
    ) {
        if (structure.atoms().isEmpty()) return;

        Map<String, ChemicalAtom> atomsById = new LinkedHashMap<>();
        for (ChemicalAtom atom : structure.atoms()) atomsById.put(atom.id(), atom);

        double molecularMass = 0.0;
        double radiusSum = 0.0;
        int resolvedAtoms = 0;
        int donorSites = 0;
        int acceptorSites = 0;
        Map<String, Integer> degree = new LinkedHashMap<>();

        for (ChemicalAtom atom : structure.atoms()) {
            MaterialSnapshot element = registry.get(atom.elementId());
            if (element == null) continue;
            double protons = Math.max(1.0, element.property("protons"));
            double neutrons = Math.max(0.0, element.property("neutrons"));
            molecularMass += protons + neutrons;
            radiusSum += Math.max(1.0, element.property("atomicradius"));
            resolvedAtoms++;

            double affinity = electronAffinityAxis(element);
            if (affinity <= -35) donorSites++;
            if (affinity >= 35) acceptorSites++;
            degree.put(atom.id(), 0);
        }
        if (resolvedAtoms == 0) return;

        double bondPolaritySum = 0.0;
        double bondStrengthSum = 0.0;
        double bondOrderSum = 0.0;
        int resolvedBonds = 0;
        for (ChemicalBond bond : structure.bonds()) {
            ChemicalAtom firstAtom = atomsById.get(bond.firstAtom());
            ChemicalAtom secondAtom = atomsById.get(bond.secondAtom());
            if (firstAtom == null || secondAtom == null) continue;
            MaterialSnapshot first = registry.get(firstAtom.elementId());
            MaterialSnapshot second = registry.get(secondAtom.elementId());
            if (first == null || second == null) continue;

            double polarity = clamp100(Math.abs(electronAffinityAxis(first) - electronAffinityAxis(second)) / 2.0);
            double strength = (first.property("bondstrength") + second.property("bondstrength")) / 2.0;
            double order = Math.max(0.5, bond.order().value());
            double typeFactor = switch (bond.type()) {
                case COVALENT -> 1.00;
                case COORDINATE -> 0.92;
                case IONIC -> 1.15;
                case HYDROGEN -> 0.42;
                case VAN_DER_WAALS -> 0.18;
                case MIXED -> 0.82;
                case METALLIC -> 0.70;
                case NONE -> 0.05;
            };

            bondPolaritySum += polarity;
            bondStrengthSum += strength * order * typeFactor;
            bondOrderSum += order;
            resolvedBonds++;
            degree.computeIfPresent(bond.firstAtom(), (ignored, value) -> value + 1);
            degree.computeIfPresent(bond.secondAtom(), (ignored, value) -> value + 1);
        }

        double averageBondPolarity = resolvedBonds == 0 ? 0.0 : bondPolaritySum / resolvedBonds;
        double averageBondStrength = resolvedBonds == 0 ? 0.0 : bondStrengthSum / resolvedBonds;
        double averageBondOrder = resolvedBonds == 0 ? 0.0 : bondOrderSum / resolvedBonds;
        double siteBalance = 100.0 * Math.min(donorSites, acceptorSites) / Math.max(1.0, resolvedAtoms);

        double bridging = 0.0;
        for (ChemicalAtom atom : structure.atoms()) {
            int atomDegree = degree.getOrDefault(atom.id(), 0);
            if (atomDegree < 2) continue;
            MaterialSnapshot element = registry.get(atom.elementId());
            if (element == null) continue;
            double polarityAxis = Math.abs(electronAffinityAxis(element));
            if (polarityAxis < 35) continue;
            bridging = Math.max(bridging, clamp100((atomDegree - 1) * 30.0 + polarityAxis * 0.5));
        }

        double association = clamp100(
                averageBondPolarity * 0.45
                        + siteBalance * 0.10
                        + bridging * 0.45
        );
        double averageRadius = radiusSum / resolvedAtoms;
        double compactness = clamp100(100.0 - averageRadius);

        // Small molecules start volatile. Mass and intermolecular association raise the boiling point.
        // Multi-connected polar centres raise association without checking for any named element.
        double boiling = -95.0
                + molecularMass * 1.60
                + averageBondStrength * 0.35
                + association * 0.75
                + averageBondPolarity * 0.15
                + Math.max(0, resolvedAtoms - 1) * 5.0
                + compactness * 0.05;

        // Molecular solids/liquids need a finite liquid range. Symmetry/rigidity can narrow it;
        // association and molecular size generally widen the liquid window in this gameplay model.
        double liquidWindow = 52.0
                + resolvedAtoms * 4.0
                + association * 0.14
                + Math.max(0.0, 1.5 - averageBondOrder) * 8.0;
        double melting = boiling - liquidWindow;

        values.put("molecularmass", molecularMass);
        values.put("molecularassociation", association);
        values.put("bondpolarity", averageBondPolarity);
        values.put("bondstrength", nonNegative(Math.max(values.getOrDefault("bondstrength", 0.0), averageBondStrength)));
        values.put("meltingpoint", melting);
        values.put("boilingpoint", Math.max(melting + 1.0, boiling));
        values.put("polarity", clamp100(Math.max(values.getOrDefault("polarity", 0.0),
                averageBondPolarity * 0.70 + association * 0.30)));
        values.put("volatility", clamp100(
                70.0
                        - (boiling - 20.0) * 0.45
                        - association * 0.15
                        + Math.max(0.0, 30.0 - molecularMass) * 0.20
        ));
        values.put("viscosity", clamp100(
                8.0
                        + association * 0.35
                        + resolvedAtoms * 2.0
                        + Math.max(0.0, 20.0 - melting) * 0.03
        ));
    }

    private static double electronAffinityAxis(MaterialSnapshot atom) {
        return atom.property("electronacceptancetendency") - atom.property("electrondonationtendency");
    }

    /** Validates atom-exact formula only when .contains(...) directly references elemental snapshots. */
    private static void validateStructureComposition(
            MaterialSnapshot material,
            Map<String, MaterialSnapshot> registry,
            List<ChemistryDiagnostic> diagnostics
    ) {
        if (material.structure().isEmpty() || material.structure().get().atoms().isEmpty() || material.composition().isEmpty()) return;

        boolean directElementsOnly = material.composition().stream().allMatch(component -> {
            MaterialSnapshot child = registry.get(component.substanceId());
            return child != null && child.composition().isEmpty();
        });
        if (!directElementsOnly) return;

        Map<String, Integer> expected = new LinkedHashMap<>();
        for (CompositionEntry component : material.composition()) {
            expected.merge(component.substanceId(), component.amount(), Integer::sum);
        }
        Map<String, Integer> actual = new LinkedHashMap<>();
        for (ChemicalAtom atom : material.structure().get().atoms()) {
            actual.merge(atom.elementId(), 1, Integer::sum);
        }

        if (!expected.equals(actual)) {
            diagnostics.add(new ChemistryDiagnostic(
                    ChemistryStatus.CHANGE_REQUIRED,
                    material.id(),
                    "STRUCTURE_COMPOSITION_MISMATCH",
                    "ChemicalStructure formula " + actual + " does not match .contains(...) composition " + expected + ".",
                    List.of("Make the effective atom graph and .contains(...) use the same atom counts."),
                    ""
            ));
        }
    }

    private static int blendColor(MaterialSnapshot material, Map<String, MaterialSnapshot> registry) {
        if (material.composition().isEmpty()) return material.color() & 0xFFFFFF;
        int total = 0;
        double r = 0;
        double g = 0;
        double b = 0;
        for (CompositionEntry component : material.composition()) {
            MaterialSnapshot child = registry.get(component.substanceId());
            if (child == null) continue;
            int color = child.color();
            r += ((color >> 16) & 255) * component.amount();
            g += ((color >> 8) & 255) * component.amount();
            b += (color & 255) * component.amount();
            total += component.amount();
        }
        if (total == 0) return material.color() & 0xFFFFFF;
        return ((int) Math.round(r / total) << 16)
                | ((int) Math.round(g / total) << 8)
                | (int) Math.round(b / total);
    }

    /**
     * Calculates the material tier from finished, open-ended compound properties. Component tiers are
     * only a progression floor; they do not cap synergy, so two lower-tier components may legitimately
     * produce a much higher-tier material when the resulting properties support it.
     *
     * <p>The reverse mapping uses the exact tier-band scale from {@link MaterialPropertyCalculator}
     * instead of a chemistry-only ULV..IV threshold table. Therefore every tier currently present in
     * {@link MachineTier#ALL} is reachable, and adding later tiers automatically extends chemistry.</p>
     */
    public static int calculateTier(Map<String, Double> values, Set<MaterialClassification> kinds, int componentTierFloor) {
        double structural = max(values, "structuralstrength", "tensilestrength", "compressivestrength", "hardness");
        double electrical = max(values, "electricalconductivity", "insulationstrength");
        double chemical = max(values, "chemicalstability", "corrosionresistance", "oxidationresistance");
        double special = max(values, "fracturetoughness", "thermalconductivity", "pressureresistance", "magneticstrength");
        double score = structural * 0.34 + electrical * 0.25 + chemical * 0.22 + special * 0.19;

        if (kinds.contains(MaterialClassification.ALLOY)) score += 8.0D;
        if (kinds.contains(MaterialClassification.POLYMER)) {
            score += Math.min(MaterialPropertyCalculator.DEFAULT_TIER_BAND_SIZE * 0.25D,
                    values.getOrDefault("insulationstrength", 0.0) * 0.05D);
        }

        int calculated = MaterialPropertyCalculator.tierIndexForBandedValue(score);

        // Compound processing may become more demanding than its ingredients, but it must never
        // become a lower progression tier than the strongest declared component by accident.
        int floor = Math.max(0, componentTierFloor);
        return Math.min(MachineTier.ALL.size() - 1, Math.max(calculated, floor));
    }

    public static String tierName(int tier) {
        int safe = Math.max(0, tier);
        return safe < MachineTier.ALL.size() ? MachineTier.ALL.get(safe).displayName() : "TIER_" + safe;
    }

    private static double weightedComponentProperty(
            MaterialSnapshot material,
            Map<String, MaterialSnapshot> registry,
            String property
    ) {
        double sum = 0;
        int total = 0;
        for (CompositionEntry component : material.composition()) {
            MaterialSnapshot child = registry.get(component.substanceId());
            if (child == null) continue;
            sum += child.property(property) * component.amount();
            total += component.amount();
        }
        return total == 0 ? 0 : sum / total;
    }

    private static double weightedVariance(
            MaterialSnapshot material,
            Map<String, MaterialSnapshot> registry,
            String property,
            double mean
    ) {
        double sum = 0;
        int total = 0;
        for (CompositionEntry component : material.composition()) {
            MaterialSnapshot child = registry.get(component.substanceId());
            if (child == null) continue;
            double d = child.property(property) - mean;
            sum += d * d * component.amount();
            total += component.amount();
        }
        return total == 0 ? 0 : sum / total;
    }

    private static double max(Map<String, Double> values, Object... keys) {
        double result = 0;
        for (Object key : keys) {
            double value = key instanceof Number n
                    ? n.doubleValue()
                    : values.getOrDefault(String.valueOf(key), 0.0);
            result = Math.max(result, value);
        }
        return result;
    }

    private static double nonNegative(double value) {
        if (!Double.isFinite(value)) return 0.0D;
        return Math.max(0.0D, value);
    }

    private static double clamp100(double value) {
        return Math.max(0, Math.min(100, Double.isFinite(value) ? value : 0));
    }

    private static double signed100(double value) {
        return Math.max(-100, Math.min(100, Double.isFinite(value) ? value : 0));
    }

    private static String suggestedMaterial(String id) {
        String constant = id.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        return "public static final IndustrialMaterial " + constant
                + " = material(\"" + id + "\", \"Change Me\", 0x808080)\n"
                + "        .contains(/* component(...) */)\n"
                + "        .build();";
    }
}
