package net.mads.industron.material.chemistry.geology;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialOrePolicy;
import net.mads.industron.material.MaterialOreHost;
import net.mads.industron.material.MaterialTierResolver;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.chemistry.CompositionEntry;
import net.mads.industron.material.chemistry.ChemicalStructure;
import net.mads.industron.material.chemistry.MaterialAnalysis;
import net.mads.industron.material.chemistry.MaterialClassification;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Builds geology definitions from the registered fictional material chemistry. */
public final class GeologyPlanner {
    public record Result(List<OreMineral> minerals, List<DepositDefinition> deposits) {
        public Result {
            minerals = List.copyOf(minerals);
            deposits = List.copyOf(deposits);
        }
    }

    public Result plan(Map<String, MaterialAnalysis> materials) {
        List<OreMineral> minerals = new ArrayList<>();

        for (MaterialAnalysis analysis : materials.values().stream()
                .sorted(Comparator.comparing(value -> value.source().id()))
                .toList()) {
            if (!isOreMineral(analysis)) continue;

            Map<String, Integer> recoverable = new LinkedHashMap<>();
            flattenRecoverable(analysis.source().id(), 1, materials, recoverable, new HashSet<>());
            if (recoverable.isEmpty()) continue;

            OreMineral mineral = new OreMineral(
                    analysis.source().id(),
                    analysis.source().displayName(),
                    analysis.source().id(),
                    recoverable,
                    Set.of(),
                    analysis.properties().get("magneticstrength") >= 35.0D,
                    processingDifficulty(analysis)
            );
            minerals.add(mineral);
        }

        for (OreMineral explicit : GeologyDefinitions.minerals()) {
            if (minerals.stream().noneMatch(value -> value.id().equals(explicit.id()))) minerals.add(explicit);
        }
        minerals.sort(Comparator.comparing(OreMineral::id));

        List<DepositDefinition> deposits = new ArrayList<>();
        for (OreMineral mineral : minerals) {
            MaterialAnalysis analysis = materials.get(mineral.materialId());
            if (analysis == null || !(analysis.source().backingMaterial() instanceof IndustrialMaterial material)) continue;
            if (!GeologyMaterialRoles.isDedicatedOre(analysis)) continue;
            deposits.add(defaultDeposit(analysis, material, mineral, minerals, materials));
        }
        for (DepositDefinition explicit : GeologyDefinitions.deposits()) {
            if (deposits.stream().noneMatch(value -> value.id().equals(explicit.id()))) deposits.add(explicit);
        }
        deposits.sort(Comparator.comparing(DepositDefinition::id));
        return new Result(minerals, deposits);
    }

    private static boolean isOreMineral(MaterialAnalysis analysis) {
        return GeologyMaterialRoles.isOreMineral(analysis);
    }


    private static MaterialOrePolicy.DimensionBand dimensionFor(MaterialAnalysis analysis, IndustrialMaterial material) {
        return MaterialTierResolver.geologyDimension(material);
    }

    private static void flattenRecoverable(
            String materialId,
            int multiplier,
            Map<String, MaterialAnalysis> materials,
            Map<String, Integer> output,
            Set<String> stack
    ) {
        if (!stack.add(materialId)) return;
        MaterialAnalysis analysis = materials.get(materialId);
        if (analysis == null) {
            stack.remove(materialId);
            return;
        }
        if (analysis.source().backingMaterial() instanceof IndustrialMaterial material && material.atomicNumber() > 0) {
            output.merge(materialId, Math.max(1, multiplier), Integer::sum);
            stack.remove(materialId);
            return;
        }
        for (CompositionEntry component : analysis.source().composition()) {
            long scaled = (long) multiplier * component.amount();
            int safe = scaled > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) scaled;
            flattenRecoverable(component.substanceId(), safe, materials, output, stack);
        }
        stack.remove(materialId);
    }

    private static double processingDifficulty(MaterialAnalysis analysis) {
        double bond = analysis.properties().get("bondstrength");
        double stability = analysis.properties().get("chemicalstability");
        double hardness = analysis.properties().get("hardness");
        double componentComplexity = Math.min(30.0D, analysis.source().composition().size() * 7.0D);
        double tier = Math.min(35.0D, analysis.calculatedTierIndex() * 4.0D);
        double topologyAdjustment = analysis.classifications().contains(MaterialClassification.PHYSICAL_MIXTURE) ? -22.0D
                : analysis.classifications().contains(MaterialClassification.ALLOY) ? 8.0D
                : analysis.classifications().contains(MaterialClassification.IONIC_COMPOUND) ? 4.0D
                : 12.0D;
        return clamp(0.22D * bond + 0.20D * stability + 0.16D * hardness
                + componentComplexity + tier + topologyAdjustment, 5.0D, 100.0D);
    }

    private static DepositDefinition defaultDeposit(
            MaterialAnalysis analysis,
            IndustrialMaterial material,
            OreMineral ore,
            List<OreMineral> allMinerals,
            Map<String, MaterialAnalysis> materials
    ) {
        MaterialOrePolicy.DimensionBand dimension = dimensionFor(analysis, material);
        DepositGeometry geometry = geometryFor(analysis);
        int geologyTierIndex = MaterialTierResolver.strongestComponentTierIndex(material);
        HeightProfile height = heightFor(dimension, geologyTierIndex, geometry);
        int[] size = sizeFor(geometry, geologyTierIndex);
        double grade = gradeFor(analysis, ore.processingDifficulty());
        double rarity = rarityWeight(geologyTierIndex, ore.processingDifficulty());
        Set<TerrainProfile> terrain = terrainFor(geometry, dimension);
        Set<String> hosts = hostRocks(material);
        Set<String> dimensions = Set.of(dimensionId(dimension));
        Set<String> biomes = biomeTagsFor(geometry, dimension);
        List<DepositDefinition.WeightedMineral> secondary = secondaryMinerals(ore, allMinerals, materials, dimension);
        List<SurfaceIndicator> indicators = indicatorsFor(analysis, geometry);

        double minimumGrade = clamp(grade * 0.35D, 0.01D, 1.0D);
        double maximumGrade = clamp(grade * (geometry == DepositGeometry.VEIN ? 1.8D : 1.45D), minimumGrade, 0.95D);
        double coreChance = switch (geometry) {
            case PORPHYRY -> 0.22D;
            case VEIN, PEGMATITE_LIKE, SKARN_LIKE -> 0.14D;
            case DISSEMINATED -> 0.04D;
            default -> 0.08D;
        };

        return new DepositDefinition(
                ore.id() + "_deposit",
                ore.displayName() + " Deposit",
                geometry,
                List.of(new DepositDefinition.WeightedMineral(ore.id(), 100)),
                secondary,
                gangueFor(hosts),
                new DepositCondition(
                        dimensions,
                        biomes,
                        hosts,
                        Set.of(),
                        terrain,
                        height.minimumY(),
                        height.maximumY(),
                        height.preferredY(),
                        height.verticalSpread()
                ),
                size[0],
                size[1],
                grade,
                minimumGrade,
                maximumGrade,
                coreChance,
                rarity,
                indicators
        );
    }

    private static DepositGeometry geometryFor(MaterialAnalysis analysis) {
        // Several bulk properties scale with the material tier multiplier. Geometry must describe
        // chemistry, not progression, so normalize those values before comparing thresholds.
        double tierScale = Math.max(1.0D, analysis.properties().get("tiermultiplier"));
        double density = analysis.properties().get("density") / tierScale;
        double stability = analysis.properties().get("chemicalstability") / tierScale;
        double reactivity = analysis.properties().get("reactivity");
        double crystal = analysis.properties().get("crystalstability");
        double magnetic = analysis.properties().get("magneticstrength") / tierScale;
        double brittleness = analysis.properties().get("brittleness");
        double pressure = analysis.properties().get("pressureresistance") / tierScale;
        double volatility = analysis.properties().get("volatility");
        ChemicalStructure.Topology topology = analysis.source().structure()
                .map(ChemicalStructure::topology)
                .orElse(ChemicalStructure.Topology.UNKNOWN);

        if (analysis.classifications().contains(MaterialClassification.PHYSICAL_MIXTURE)) return DepositGeometry.DISSEMINATED;
        if (magnetic >= 62.0D && crystal >= 52.0D) return DepositGeometry.BANDED;
        if (density >= 78.0D && stability >= 68.0D) return DepositGeometry.MAGMATIC;
        if (reactivity >= 66.0D && volatility >= 18.0D) return DepositGeometry.HYDROTHERMAL;
        if (crystal >= 74.0D && brittleness >= 56.0D) return DepositGeometry.PEGMATITE_LIKE;
        if (topology == ChemicalStructure.Topology.IONIC_LATTICE
                && pressure >= 86.0D && reactivity >= 52.0D) return DepositGeometry.SKARN_LIKE;
        if (density >= 62.0D && analysis.source().composition().size() >= 3) return DepositGeometry.PORPHYRY;
        if (stability >= 82.0D && density < 56.0D) return DepositGeometry.LENS;
        if ((analysis.classifications().contains(MaterialClassification.ALLOY)
                || topology == ChemicalStructure.Topology.METALLIC_LATTICE) && density >= 62.0D) {
            return DepositGeometry.MAGMATIC;
        }
        if (topology == ChemicalStructure.Topology.IONIC_LATTICE && reactivity >= 72.0D) {
            return DepositGeometry.HYDROTHERMAL;
        }
        return DepositGeometry.VEIN;
    }

    private static HeightProfile heightFor(
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
                yield new HeightProfile(min, max, preferred, Math.max(12, (max - min) / 4));
            }
            case NETHER -> {
                int netherTier = Math.max(0, safeTier - MachineTier.ALL.indexOf(MachineTier.EV));
                int min = 8;
                int max = 120;
                int preferred = Math.max(20, 82 - netherTier * 24);
                yield new HeightProfile(min, max, preferred, 24);
            }
            case END -> {
                int endTier = Math.max(0, safeTier - MachineTier.ALL.indexOf(MachineTier.ZPM));
                int min = 8;
                int max = 96;
                int preferred = Math.max(24, 62 - Math.min(4, endTier) * 7);
                yield new HeightProfile(min, max, preferred, 22);
            }
        };
    }

    private static int[] sizeFor(DepositGeometry geometry, int tierIndex) {
        int tierPenalty = Math.min(18, Math.max(0, tierIndex) * 2);
        return switch (geometry) {
            case PORPHYRY -> new int[]{72 - tierPenalty, 176 - tierPenalty};
            case DISSEMINATED -> new int[]{64 - tierPenalty, 160 - tierPenalty};
            case BANDED, LAYER -> new int[]{56 - tierPenalty, 144 - tierPenalty};
            case VEIN, HYDROTHERMAL -> new int[]{44 - tierPenalty, 132 - tierPenalty};
            case PEGMATITE_LIKE, SKARN_LIKE, LENS -> new int[]{32 - tierPenalty / 2, 96 - tierPenalty};
            case PIPE -> new int[]{28 - tierPenalty / 2, 84 - tierPenalty};
            default -> new int[]{36 - tierPenalty / 2, 108 - tierPenalty};
        };
    }

    private static double gradeFor(MaterialAnalysis analysis, double difficulty) {
        double tierPenalty = analysis.calculatedTierIndex() * 0.028D;
        double complexityPenalty = Math.max(0, analysis.source().composition().size() - 2) * 0.025D;
        return clamp(0.42D - tierPenalty - complexityPenalty - difficulty * 0.0011D, 0.055D, 0.48D);
    }

    /** Relative selection weight, not a direct percentage. */
    private static double rarityWeight(int tierIndex, double difficulty) {
        double[] base = {100, 70, 46, 28, 17, 10, 6, 3.5, 2.5, 1.8, 1.3, 1.0, 0.8, 0.6, 0.45};
        int index = Math.max(0, Math.min(base.length - 1, tierIndex));
        return Math.max(0.1D, base[index] * (1.15D - difficulty / 180.0D));
    }

    private static List<DepositDefinition.WeightedMineral> secondaryMinerals(
            OreMineral primary,
            List<OreMineral> minerals,
            Map<String, MaterialAnalysis> materials,
            MaterialOrePolicy.DimensionBand dimension
    ) {
        Set<String> primaryElements = primary.recoverableMaterials().keySet();
        return minerals.stream()
                .filter(candidate -> !candidate.id().equals(primary.id()))
                .filter(candidate -> {
                    MaterialAnalysis candidateAnalysis = materials.get(candidate.materialId());
                    if (candidateAnalysis == null || !(candidateAnalysis.source().backingMaterial() instanceof IndustrialMaterial material)) return false;
                    return dimensionFor(candidateAnalysis, material) == dimension;
                })
                .map(candidate -> Map.entry(candidate, similarity(primaryElements, candidate.recoverableMaterials().keySet())))
                .filter(entry -> entry.getValue() > 0.18D)
                .sorted(Map.Entry.<OreMineral, Double>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().id()))
                .limit(2)
                .map(entry -> new DepositDefinition.WeightedMineral(
                        entry.getKey().id(),
                        Math.max(5, (int) Math.round(entry.getValue() * 35.0D))
                ))
                .toList();
    }

    private static double similarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0D;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0D : (double) intersection.size() / union.size();
    }

    private static Set<TerrainProfile> terrainFor(
            DepositGeometry geometry,
            MaterialOrePolicy.DimensionBand dimension
    ) {
        if (dimension != MaterialOrePolicy.DimensionBand.OVERWORLD) return Set.of(TerrainProfile.ANY);
        return switch (geometry) {
            case PEGMATITE_LIKE -> Set.of(TerrainProfile.HIGH_ELEVATION);
            case PLACER -> Set.of(TerrainProfile.LOW_ELEVATION, TerrainProfile.COASTAL);
            case OCEAN_FLOOR_MASS -> Set.of(TerrainProfile.OCEAN, TerrainProfile.SUBMERGED);
            default -> Set.of(TerrainProfile.ANY);
        };
    }

    private static Set<String> hostRocks(IndustrialMaterial material) {
        return MaterialOreHost.compatibleHosts(material).stream()
                .map(MaterialOreHost::hostBlock)
                .map(ResourceLocation::toString)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private static Set<String> biomeTagsFor(DepositGeometry geometry, MaterialOrePolicy.DimensionBand dimension) {
        if (dimension == MaterialOrePolicy.DimensionBand.NETHER) return Set.of("#minecraft:is_nether");
        if (dimension == MaterialOrePolicy.DimensionBand.END) return Set.of("#minecraft:is_end");
        return switch (geometry) {
            case PEGMATITE_LIKE, SKARN_LIKE, MAGMATIC, PORPHYRY -> Set.of("#minecraft:is_mountain");
            case PLACER, OCEAN_FLOOR_MASS -> Set.of("#minecraft:is_ocean");
            default -> Set.of("#minecraft:is_overworld");
        };
    }

    private static Map<String, Integer> gangueFor(Set<String> hostBlocks) {
        if (hostBlocks.isEmpty()) return Map.of();
        java.util.LinkedHashMap<String, Integer> result = new java.util.LinkedHashMap<>();
        int baseWeight = Math.max(1, 100 / hostBlocks.size());
        for (String host : hostBlocks) result.put(host, baseWeight);
        return Map.copyOf(result);
    }

    private static List<SurfaceIndicator> indicatorsFor(MaterialAnalysis analysis, DepositGeometry geometry) {
        List<SurfaceIndicator> result = new ArrayList<>();
        result.add(SurfaceIndicator.FLOAT_STONE);
        if (analysis.properties().get("reactivity") >= 48) result.add(SurfaceIndicator.COLOR_STAINING);
        if (analysis.properties().get("crystalstability") >= 58) result.add(SurfaceIndicator.MINERAL_OUTCROP);
        if (analysis.properties().get("volatility") >= 58) result.add(SurfaceIndicator.VENT);
        if (geometry == DepositGeometry.HYDROTHERMAL || geometry == DepositGeometry.SKARN_LIKE) {
            result.add(SurfaceIndicator.ALTERED_ROCK);
        }
        return result.stream().distinct().toList();
    }

    private static String dimensionId(MaterialOrePolicy.DimensionBand dimension) {
        return switch (dimension) {
            case OVERWORLD -> "minecraft:overworld";
            case NETHER -> "minecraft:the_nether";
            case END -> "minecraft:the_end";
        };
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record HeightProfile(int minimumY, int maximumY, int preferredY, int verticalSpread) {
    }
}
