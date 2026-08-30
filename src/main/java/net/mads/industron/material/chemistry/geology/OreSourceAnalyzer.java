package net.mads.industron.material.chemistry.geology;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialOreHost;
import net.mads.industron.material.MaterialOrePolicy;
import net.mads.industron.material.MaterialProperties;
import net.mads.industron.material.chemistry.ChemicalStructure;
import net.mads.industron.material.chemistry.ChemistryPhase;
import net.mads.industron.material.chemistry.CompositionEntry;
import net.mads.industron.material.chemistry.MaterialAnalysis;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.structure.StoneMaterial;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Dynamically checks every registered element against the current source/mineral graph.
 *
 * <p>No atomic-number list and no real-world ore-family names live here. Adding another
 * {@code ElementDefinition} automatically enters the analysis through IndustrialMaterials.ALL.
 * Missing mineral suggestions are derived from the fictional electronic/material properties.</p>
 */
public final class OreSourceAnalyzer {
    public List<OreSourceAnalysis> analyze(
            Map<String, MaterialAnalysis> materials,
            List<OreMineral> minerals
    ) {
        Map<String, List<String>> mineralCoverage = mineralCoverage(materials, minerals);
        Set<String> dedicatedOreMinerals = dedicatedOreMinerals(materials, minerals);
        Set<String> stoneTraceMinerals = GeologyMaterialRoles.stoneTraceMineralIds();
        List<OreSourceAnalysis> result = new ArrayList<>();

        materials.values().stream()
                .filter(value -> isElement(value.source().backingMaterial()))
                .sorted(Comparator.comparingInt(value -> ((IndustrialMaterial) value.source().backingMaterial()).atomicNumber()))
                .forEach(analysis -> result.add(analyzeElement(
                        analysis, materials, mineralCoverage, dedicatedOreMinerals, stoneTraceMinerals
                )));
        return List.copyOf(result);
    }

    private OreSourceAnalysis analyzeElement(
            MaterialAnalysis analysis,
            Map<String, MaterialAnalysis> materials,
            Map<String, List<String>> mineralCoverage,
            Set<String> dedicatedOreMinerals,
            Set<String> stoneTraceMinerals
    ) {
        IndustrialMaterial material = (IndustrialMaterial) analysis.source().backingMaterial();
        MaterialProperties properties = material.properties();
        MaterialOrePolicy.DimensionBand dimension = MaterialOrePolicy.dimensionForTier(material.tier());

        List<String> covering = mineralCoverage.getOrDefault(material.id(), List.of());
        List<String> oreCoverage = covering.stream().filter(dedicatedOreMinerals::contains).toList();
        if (!oreCoverage.isEmpty()) {
            return covered(analysis, material, dimension, OreSourceAnalysis.SourceKind.DEDICATED_ORE,
                    "Covered by reviewed fictional OreMaterials definition(s): "
                            + String.join(", ", oreCoverage) + ".",
                    oreCoverage.get(0));
        }

        // Stone trace occurrence is a valid natural acquisition path, but it is deliberately NOT
        // counted as having a dedicated ore. The report must continue to propose a real ore until
        // a named OreMaterials definition covers the element.
        List<String> traceCoverage = covering.stream().filter(stoneTraceMinerals::contains).toList();
        String traceDetail = traceCoverage.isEmpty()
                ? ""
                : " Trace-only coverage currently exists through StoneMaterial mineral(s): "
                + String.join(", ", traceCoverage) + "; this does not count as a dedicated ore.";

        if (analysis.phase() == ChemistryPhase.GAS
                || properties.electronicFamily() == MaterialProperties.ElectronicFamily.NOBLE_GAS_LIKE) {
            return noOre(analysis, material, dimension, OreSourceAnalysis.SourceKind.ATMOSPHERIC_GAS,
                    "Gas/noble-gas-like behaviour indicates atmosphere/gas-reservoir acquisition rather than a solid ore block.");
        }

        if (analysis.phase() == ChemistryPhase.LIQUID) {
            return noOre(analysis, material, dimension, OreSourceAnalysis.SourceKind.NATURAL_FLUID_RESERVOIR,
                    "Liquid behaviour indicates a natural fluid/reservoir source rather than a normal solid ore block.");
        }

        MaterialAnalysis companion = bestCompanion(analysis, materials);
        if (companion == null || !(companion.source().backingMaterial() instanceof IndustrialMaterial companionMaterial)) {
            return new OreSourceAnalysis(
                    material.id(), material.displayName(), material.atomicNumber(), analysis.calculatedTierName(), dimension,
                    OreSourceAnalysis.Status.BLOCKED,
                    traceCoverage.isEmpty() ? OreSourceAnalysis.SourceKind.MISSING : OreSourceAnalysis.SourceKind.STONE_TRACE,
                    "No dedicated ore covers this element and no compatible fictional companion could be inferred from registered material properties." + traceDetail,
                    "", ChemicalStructure.Topology.UNKNOWN, "", 0, 0, "", null
            );
        }

        ChemicalStructure.Topology topology = suggestedTopology(properties, companionMaterial.properties());
        int[] ratio = stoichiometricRatio(properties, companionMaterial.properties());
        String targetConstant = javaConstant(material.id());
        String companionConstant = javaConstant(companionMaterial.id());
        String suggestedContains = ".contains(component(" + targetConstant + ", " + ratio[0] + "), component("
                + companionConstant + ", " + ratio[1] + "))";

        OreSourceAnalysis.SuggestedGeology geology = suggestedGeology(
                analysis, material, companion, companionMaterial, topology
        );

        return new OreSourceAnalysis(
                material.id(), material.displayName(), material.atomicNumber(), analysis.calculatedTierName(), dimension,
                OreSourceAnalysis.Status.MISSING_ORE_MINERAL,
                traceCoverage.isEmpty() ? OreSourceAnalysis.SourceKind.MISSING : OreSourceAnalysis.SourceKind.STONE_TRACE,
                "No dedicated OreMaterials definition currently covers this element." + traceDetail
                        + " The anonymous suggestion is calculated from the registered fictional chemistry. Define its name/ID and reviewed .contains(...) in OreMaterials yourself; only then does it become a real ore.",
                "", topology, companionMaterial.id(), ratio[0], ratio[1], suggestedContains, geology
        );
    }

    private static Map<String, List<String>> mineralCoverage(
            Map<String, MaterialAnalysis> materials,
            List<OreMineral> minerals
    ) {
        Map<String, List<String>> coverage = new LinkedHashMap<>();
        for (OreMineral mineral : minerals) {
            Set<String> elements = new LinkedHashSet<>();
            collectCoveredElements(mineral.materialId(), materials, elements, new HashSet<>());
            for (String elementId : elements) {
                coverage.computeIfAbsent(elementId, ignored -> new ArrayList<>()).add(mineral.id());
            }
        }
        coverage.replaceAll((key, value) -> value.stream().distinct().sorted().toList());
        return coverage;
    }

    private static void collectCoveredElements(
            String materialId,
            Map<String, MaterialAnalysis> materials,
            Set<String> output,
            Set<String> stack
    ) {
        if (!stack.add(materialId)) return;
        try {
            MaterialAnalysis analysis = materials.get(materialId);
            if (analysis == null) return;
            if (isElement(analysis.source().backingMaterial())) {
                output.add(materialId);
                return;
            }
            for (CompositionEntry component : analysis.source().composition()) {
                collectCoveredElements(component.substanceId(), materials, output, stack);
            }
        } finally {
            stack.remove(materialId);
        }
    }

    private static Set<String> dedicatedOreMinerals(
            Map<String, MaterialAnalysis> materials,
            List<OreMineral> minerals
    ) {
        Set<String> result = new LinkedHashSet<>();
        for (OreMineral mineral : minerals) {
            MaterialAnalysis analysis = materials.get(mineral.materialId());
            if (GeologyMaterialRoles.isDedicatedOre(analysis)) result.add(mineral.id());
        }
        return Set.copyOf(result);
    }

    private static MaterialAnalysis bestCompanion(
            MaterialAnalysis target,
            Map<String, MaterialAnalysis> materials
    ) {
        return materials.values().stream()
                .filter(value -> isElement(value.source().backingMaterial()))
                .filter(value -> !value.source().id().equals(target.source().id()))
                .max(Comparator
                        .comparingDouble((MaterialAnalysis value) -> compatibilityScore(target, value))
                        .thenComparing(value -> value.source().id()))
                .orElse(null);
    }

    /**
     * General fictional-chemistry compatibility score. Opposite ion tendency, donor/acceptor
     * complement and reasonable crystal-size compatibility are preferred. No named element pair
     * is special-cased.
     */
    private static double compatibilityScore(MaterialAnalysis target, MaterialAnalysis candidate) {
        IndustrialMaterial a = (IndustrialMaterial) target.source().backingMaterial();
        IndustrialMaterial b = (IndustrialMaterial) candidate.source().backingMaterial();
        MaterialProperties pa = a.properties();
        MaterialProperties pb = b.properties();

        int qa = pa.preferredIonCharge();
        int qb = pb.preferredIonCharge();
        double oppositeCharge = qa != 0 && qb != 0 && Integer.signum(qa) != Integer.signum(qb)
                ? 80.0 + Math.min(Math.abs(qa), Math.abs(qb)) * 10.0
                : 0.0;
        double donorAcceptance = Math.max(
                pa.electronDonationTendency() * 0.75 + pb.electronAcceptanceTendency() * 0.75,
                pb.electronDonationTendency() * 0.75 + pa.electronAcceptanceTendency() * 0.75
        );
        double radius = 100.0 - Math.min(100.0, Math.abs(pa.atomicRadius() - pb.atomicRadius()) * 0.75);
        double bond = (pa.bondStrength() + pb.bondStrength()) * 0.35;
        double crystal = (pa.crystalStability() + pb.crystalStability()) * 0.20;
        double stable = (pa.chemicalStability() + pb.chemicalStability()) * 0.12;
        double sameMetalPenalty = pa.metal() && pb.metal() ? 8.0 : 0.0;
        return oppositeCharge + donorAcceptance + radius * 0.35 + bond + crystal + stable - sameMetalPenalty;
    }

    private static ChemicalStructure.Topology suggestedTopology(MaterialProperties a, MaterialProperties b) {
        if (a.metal() && b.metal()) return ChemicalStructure.Topology.METALLIC_LATTICE;
        int qa = a.preferredIonCharge();
        int qb = b.preferredIonCharge();
        if (qa != 0 && qb != 0 && Integer.signum(qa) != Integer.signum(qb)) {
            return ChemicalStructure.Topology.IONIC_LATTICE;
        }
        int transferContrast = Math.max(
                a.electronDonationTendency() + b.electronAcceptanceTendency(),
                b.electronDonationTendency() + a.electronAcceptanceTendency()
        );
        if (transferContrast >= 105) return ChemicalStructure.Topology.IONIC_LATTICE;
        return ChemicalStructure.Topology.NETWORK;
    }

    private static int[] stoichiometricRatio(MaterialProperties target, MaterialProperties companion) {
        int targetCharge = target.preferredIonCharge();
        int companionCharge = companion.preferredIonCharge();
        if (targetCharge != 0 && companionCharge != 0
                && Integer.signum(targetCharge) != Integer.signum(companionCharge)) {
            int a = Math.abs(targetCharge);
            int b = Math.abs(companionCharge);
            int gcd = gcd(a, b);
            return new int[]{Math.max(1, b / gcd), Math.max(1, a / gcd)};
        }

        int targetTransfer = Math.max(target.electronsToStableShell(), target.electronsFromStableShell());
        int companionTransfer = Math.max(companion.electronsToStableShell(), companion.electronsFromStableShell());
        if (targetTransfer > 0 && companionTransfer > 0) {
            int gcd = gcd(targetTransfer, companionTransfer);
            return new int[]{Math.max(1, companionTransfer / gcd), Math.max(1, targetTransfer / gcd)};
        }
        return new int[]{1, 1};
    }

    private static OreSourceAnalysis.SuggestedGeology suggestedGeology(
            MaterialAnalysis target,
            IndustrialMaterial targetMaterial,
            MaterialAnalysis companion,
            IndustrialMaterial companionMaterial,
            ChemicalStructure.Topology topology
    ) {
        int targetTier = MachineTier.ALL.indexOf(targetMaterial.tier());
        int companionTier = MachineTier.ALL.indexOf(companionMaterial.tier());
        int tierIndex = Math.max(0, Math.max(targetTier, companionTier));
        MachineTier tier = MachineTier.ALL.get(tierIndex);
        MaterialOrePolicy.DimensionBand dimension = MaterialOrePolicy.dimensionForTier(tier);
        DepositGeometry geometry = switch (topology) {
            case IONIC_LATTICE -> DepositGeometry.SKARN_LIKE;
            case METALLIC_LATTICE -> DepositGeometry.MAGMATIC;
            case DISCRETE_MOLECULE -> DepositGeometry.HYDROTHERMAL;
            case NETWORK, POLYMER_NETWORK, POLYMER_CHAIN -> DepositGeometry.VEIN;
            default -> DepositGeometry.VEIN;
        };

        int[] height = projectedHeight(dimension, tierIndex, geometry);
        List<String> biomes = projectedBiomes(dimension, geometry);
        List<String> hosts = projectedHosts(dimension, targetMaterial, companionMaterial);
        return new OreSourceAnalysis.SuggestedGeology(
                tier.displayName(), dimension, geometry, height[0], height[1], height[2], biomes, hosts
        );
    }

    private static int[] projectedHeight(
            MaterialOrePolicy.DimensionBand dimension,
            int tierIndex,
            DepositGeometry geometry
    ) {
        int safeTier = Math.max(0, tierIndex);
        return switch (dimension) {
            case OVERWORLD -> {
                int min = Math.max(-64, -16 - safeTier * 14);
                int max = Math.max(8, 112 - safeTier * 24);
                int preferred = Math.max(min, Math.min(max, 54 - safeTier * 25));
                if (geometry == DepositGeometry.PEGMATITE_LIKE) {
                    min = Math.max(-16, min);
                    max = Math.max(max, 144);
                    preferred = Math.max(72, preferred);
                }
                yield new int[]{min, max, preferred};
            }
            case NETHER -> {
                int netherTier = Math.max(0, safeTier - MachineTier.ALL.indexOf(MachineTier.EV));
                yield new int[]{8, 120, Math.max(20, 82 - netherTier * 24)};
            }
            case END -> {
                int endTier = Math.max(0, safeTier - MachineTier.ALL.indexOf(MachineTier.ZPM));
                yield new int[]{8, 96, Math.max(24, 62 - Math.min(4, endTier) * 7)};
            }
        };
    }

    private static List<String> projectedBiomes(
            MaterialOrePolicy.DimensionBand dimension,
            DepositGeometry geometry
    ) {
        if (dimension == MaterialOrePolicy.DimensionBand.NETHER) return List.of("#minecraft:is_nether");
        if (dimension == MaterialOrePolicy.DimensionBand.END) return List.of("#minecraft:is_end");
        return switch (geometry) {
            case PEGMATITE_LIKE, SKARN_LIKE, MAGMATIC, PORPHYRY -> List.of("#minecraft:is_mountain");
            case PLACER, OCEAN_FLOOR_MASS -> List.of("#minecraft:is_ocean");
            default -> List.of("#minecraft:is_overworld");
        };
    }

    private static List<String> projectedHosts(
            MaterialOrePolicy.DimensionBand dimension,
            IndustrialMaterial target,
            IndustrialMaterial companion
    ) {
        List<MaterialOreHost> hosts = MaterialOrePolicy.oreHosts(dimension);
        if (hosts.isEmpty()) return List.of();

        Map<MaterialOreHost, Double> scores = new LinkedHashMap<>();
        double maximum = 0.0D;
        for (MaterialOreHost host : hosts) {
            double score = projectedHostAffinity(host.stone(), target, companion);
            scores.put(host, score);
            maximum = Math.max(maximum, score);
        }
        if (!(maximum > 0.0D)) return List.of();
        double threshold = maximum * 0.55D;
        return scores.entrySet().stream()
                .filter(entry -> entry.getValue() >= threshold)
                .sorted(Map.Entry.<MaterialOreHost, Double>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().id()))
                .map(entry -> entry.getKey().hostBlock().toString())
                .distinct()
                .toList();
    }

    private static double projectedHostAffinity(
            StoneMaterial stone,
            IndustrialMaterial target,
            IndustrialMaterial companion
    ) {
        double score = 0.0D;
        for (MaterialComponent component : stone.components()) {
            int amount = Math.max(1, component.amount());
            if (containsLeaf(component.substance(), target.id(), new HashSet<>())) score += amount * 80.0D;
            if (containsLeaf(component.substance(), companion.id(), new HashSet<>())) score += amount * 80.0D;
            if (component.substance() instanceof IndustrialMaterial trace) {
                score += amount * (propertyAffinity(target.properties(), trace.properties())
                        + propertyAffinity(companion.properties(), trace.properties())) * 0.5D;
            }
        }
        return score;
    }

    private static double propertyAffinity(MaterialProperties a, MaterialProperties b) {
        return similarity100(a.crystalStability(), b.crystalStability()) * 0.24D
                + similarity100(a.chemicalStability(), b.chemicalStability()) * 0.20D
                + similarity100(a.bondStrength(), b.bondStrength()) * 0.18D
                + similarity100(a.pressureResistance(), b.pressureResistance()) * 0.14D
                + similarity100(a.reactivity(), b.reactivity()) * 0.12D
                + similarity100(a.density(), b.density()) * 0.12D;
    }

    private static double similarity100(double a, double b) {
        return Math.max(0.0D, 100.0D - Math.min(100.0D, Math.abs(a - b)));
    }

    private static boolean containsLeaf(StoneMaterial stone, String materialId) {
        for (MaterialComponent component : stone.components()) {
            if (containsLeaf(component.substance(), materialId, new HashSet<>())) return true;
        }
        return false;
    }

    private static boolean containsLeaf(IndustrialSubstance substance, String materialId, Set<String> stack) {
        if (substance.id().equals(materialId)) return true;
        if (!stack.add(substance.id())) return false;
        try {
            if (substance instanceof IndustrialMaterial material) {
                for (MaterialComponent component : material.components()) {
                    if (containsLeaf(component.substance(), materialId, stack)) return true;
                }
            }
            return false;
        } finally {
            stack.remove(substance.id());
        }
    }

    private static int gcd(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            int t = a % b;
            a = b;
            b = t;
        }
        return Math.max(1, a);
    }

    private static boolean isElement(Object backing) {
        return backing instanceof IndustrialMaterial material && material.atomicNumber() > 0;
    }

    private static String javaConstant(String id) {
        return id.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
    }

    private static OreSourceAnalysis covered(
            MaterialAnalysis analysis,
            IndustrialMaterial material,
            MaterialOrePolicy.DimensionBand dimension,
            OreSourceAnalysis.SourceKind kind,
            String detail,
            String mineralId
    ) {
        return new OreSourceAnalysis(
                material.id(), material.displayName(), material.atomicNumber(), analysis.calculatedTierName(), dimension,
                OreSourceAnalysis.Status.COVERED, kind, detail, mineralId,
                ChemicalStructure.Topology.UNKNOWN, "", 0, 0, "", null
        );
    }

    private static OreSourceAnalysis noOre(
            MaterialAnalysis analysis,
            IndustrialMaterial material,
            MaterialOrePolicy.DimensionBand dimension,
            OreSourceAnalysis.SourceKind kind,
            String detail
    ) {
        return new OreSourceAnalysis(
                material.id(), material.displayName(), material.atomicNumber(), analysis.calculatedTierName(), dimension,
                OreSourceAnalysis.Status.NO_ORE_REQUIRED, kind, detail, "",
                ChemicalStructure.Topology.UNKNOWN, "", 0, 0, "", null
        );
    }
}
