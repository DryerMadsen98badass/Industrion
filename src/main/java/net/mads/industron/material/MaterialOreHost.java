package net.mads.industron.material;

import net.mads.industron.Industron;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.structure.StoneMaterial;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** One ore host derived from a registered {@link StoneMaterial}. */
public record MaterialOreHost(StoneMaterial stone) {
    private static final Set<String> CREATE_NATURAL_STONES = Set.of(
            "asurine",
            "crimsite",
            "ochrum",
            "veridium"
    );
    private static final double ORE_HOST_THRESHOLD = 0.55D;
    private static final double GEOLOGY_HOST_THRESHOLD = 0.45D;
    private static final int MAX_ORE_HOSTS = 4;
    private static final int MAX_GEOLOGY_HOSTS = 4;

    public MaterialOreHost {
        if (stone == null) throw new IllegalArgumentException("Ore host stone cannot be null");
    }

    public static List<MaterialOreHost> all() {
        return StoneMaterials.ALL.stream().map(MaterialOreHost::new).toList();
    }

    public static List<MaterialOreHost> forDimension(MaterialOrePolicy.DimensionBand dimension) {
        return all().stream().filter(host -> host.dimension() == dimension).toList();
    }

    /**
     * Physical hosts used inside the deposit body. The result is deliberately capped to the best
     * few stones so a chemically broad StoneMaterial list cannot make every ore occur everywhere.
     */
    public static List<MaterialOreHost> compatibleHosts(IndustrialMaterial material) {
        return rankedHosts(material, false, ORE_HOST_THRESHOLD, MAX_ORE_HOSTS);
    }

    /**
     * Non-base stones used as the visible regional geology/prospecting signal around a deposit.
     * Stone, Deepslate, Netherrack and End Stone remain background rock and are never emitted as
     * this signal, even though they remain valid replaceable terrain and may host ore in the body.
     */
    public static List<MaterialOreHost> geologyHosts(IndustrialMaterial material) {
        return rankedHosts(material, true, GEOLOGY_HOST_THRESHOLD, MAX_GEOLOGY_HOSTS);
    }

    private static List<MaterialOreHost> rankedHosts(
            IndustrialMaterial material,
            boolean excludeBaseRock,
            double relativeThreshold,
            int limit
    ) {
        if (material == null || !hasNaturalOre(material)) return List.of();
        MaterialOrePolicy.DimensionBand dimension = MaterialTierResolver.geologyDimension(material);
        List<MaterialOreHost> candidates = forDimension(dimension).stream()
                .filter(host -> !excludeBaseRock || !host.isBaseRock())
                .toList();
        if (candidates.isEmpty()) return List.of();

        Map<MaterialOreHost, Integer> scores = new LinkedHashMap<>();
        int maximum = 0;
        for (MaterialOreHost host : candidates) {
            int score = host.affinity(material);
            scores.put(host, score);
            maximum = Math.max(maximum, score);
        }
        if (maximum <= 0) return List.of();

        int threshold = Math.max(1, (int) Math.ceil(maximum * relativeThreshold));
        return scores.entrySet().stream()
                .filter(entry -> entry.getValue() >= threshold)
                .sorted(Map.Entry.<MaterialOreHost, Integer>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().id()))
                .limit(Math.max(1, limit))
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Normalized 0..10000 affinity derived from StoneMaterial .contains(...).
     *
     * <p>The component amount keeps its original meaning as a relative selection weight.
     * Geology reads that same number as signal strength. The score is normalized by
     * the stone's total declared trace amount, so adding unrelated .contains entries cannot give a
     * stone a free affinity bonus.</p>
     */
    public int affinity(IndustrialMaterial ore) {
        if (ore == null || stone.components().isEmpty()) return 0;

        Set<String> oreLeaves = leafIds(ore);
        if (oreLeaves.isEmpty()) return 0;

        double weightedMatch = 0.0D;
        double totalAmount = 0.0D;
        double strongestAssociation = 0.0D;
        Set<String> allTraceLeaves = new HashSet<>();

        for (MaterialComponent component : stone.components()) {
            IndustrialSubstance trace = component.substance();
            int amount = Math.max(1, component.amount());
            Set<String> traceLeaves = leafIds(trace);
            allTraceLeaves.addAll(traceLeaves);

            double structuralMatch;
            if (trace.id().equals(ore.id())) {
                structuralMatch = 1.0D;
            } else {
                double overlap = jaccard(oreLeaves, traceLeaves);
                structuralMatch = overlap > 0.0D ? 0.70D + overlap * 0.30D : 0.0D;
            }

            double chemicalMatch = trace instanceof IndustrialMaterial traceMaterial
                    ? chemistryAffinity(ore.properties(), traceMaterial.properties())
                    : 0.0D;

            // Exact/shared chemistry dominates. Bulk-property similarity is a weaker fallback so
            // completely unrelated trace lists cannot beat a stone that shares actual leaves.
            double match = Math.max(structuralMatch, chemicalMatch * 0.35D);
            weightedMatch += amount * match;
            totalAmount += amount;

            // Absolute trace weight still matters: weight 2 is a stronger signal
            // than 1, but it saturates instead of growing without bound.
            double amountStrength = 1.0D - Math.exp(-amount / 2.0D);
            strongestAssociation = Math.max(strongestAssociation, amountStrength * match);
        }

        double profileMatch = totalAmount <= 0.0D ? 0.0D : weightedMatch / totalAmount;
        double leafCoverage = coverage(oreLeaves, allTraceLeaves);
        double normalized = profileMatch * 0.60D
                + strongestAssociation * 0.25D
                + leafCoverage * 0.15D;
        return Math.max(0, Math.min(10_000, (int) Math.round(normalized * 10_000.0D)));
    }

    private static double chemistryAffinity(MaterialProperties ore, MaterialProperties trace) {
        double crystal = similarity(ore.crystalStability(), trace.crystalStability());
        double chemical = similarity(ore.chemicalStability(), trace.chemicalStability());
        double bond = similarity(ore.bondStrength(), trace.bondStrength());
        double pressure = similarity(ore.pressureResistance(), trace.pressureResistance());
        double reactivity = similarity(ore.reactivity(), trace.reactivity());
        double density = similarity(ore.density(), trace.density());
        return clamp01(crystal * 0.24D + chemical * 0.20D + bond * 0.18D
                + pressure * 0.14D + reactivity * 0.12D + density * 0.12D);
    }

    private static double similarity(double a, double b) {
        return clamp01(1.0D - Math.min(100.0D, Math.abs(a - b)) / 100.0D);
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0D;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0D : (double) intersection.size() / union.size();
    }

    private static double coverage(Set<String> expected, Set<String> present) {
        if (expected.isEmpty()) return 0.0D;
        int matches = 0;
        for (String id : expected) if (present.contains(id)) matches++;
        return (double) matches / expected.size();
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static Set<String> leafIds(IndustrialSubstance substance) {
        Set<String> result = new HashSet<>();
        collectLeaves(substance, result, new HashSet<>());
        return Set.copyOf(result);
    }

    private static void collectLeaves(IndustrialSubstance substance, Set<String> output, Set<String> stack) {
        if (substance == null || !stack.add(substance.id())) return;
        try {
            if (substance instanceof IndustrialMaterial material && !material.components().isEmpty()) {
                for (MaterialComponent component : material.components()) {
                    collectLeaves(component.substance(), output, stack);
                }
            } else {
                output.add(substance.id());
            }
        } finally {
            stack.remove(substance.id());
        }
    }

    public String id() {
        return stone.id();
    }

    public MaterialOrePolicy.DimensionBand dimension() {
        return stone.dimension();
    }

    public boolean isBaseRock() {
        return stone.isBaseRock();
    }

    public ResourceLocation hostBlock() {
        // Resolve the natural/base block from the StoneMaterial id. StoneModel only describes the
        // model family and is deliberately not used as host identity.
        ResourceLocation byExactId = existingBlockByPath(stone.id());
        if (byExactId != null) return byExactId;

        ResourceLocation byBlockId = existingBlockByPath(stone.id() + "_block");
        if (byBlockId != null) return byBlockId;

        if (!stone.isWithout(MaterialPart.STONE) && !stone.hasExistingPart(MaterialPart.STONE)) {
            return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, MaterialPart.STONE.registryName(stone));
        }

        ResourceLocation cobbled = stone.existingPart(MaterialPart.COBBLED_STONE);
        if (cobbled != null) return cobbled;
        ResourceLocation stonePart = stone.existingPart(MaterialPart.STONE);
        if (stonePart != null) return stonePart;

        throw new IllegalStateException("Stone material " + stone.id() + " has no resolvable worldgen host block");
    }

    private ResourceLocation existingBlockByPath(String path) {
        return stone.existingParts().values().stream()
                .filter(id -> id.getPath().equals(path))
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .findFirst()
                .orElse(null);
    }

    public ResourceLocation blockModel(IndustrialMaterial material) {
        ResourceLocation block = hostBlock();

        // Create's natural stone blocks do not have models named block/<block id>. Their
        // blockstates randomly select block/<stone>_natural_0..3 instead. Ore-host composite
        // models cannot inherit a blockstate, so select one of those real models directly.
        if (block.getNamespace().equals("create") && CREATE_NATURAL_STONES.contains(block.getPath())) {
            int variant = Math.floorMod(31 * material.atomicNumber() + id().hashCode(), 4);
            return ResourceLocation.fromNamespaceAndPath(
                    block.getNamespace(),
                    "block/" + block.getPath() + "_natural_" + variant
            );
        }

        return ResourceLocation.fromNamespaceAndPath(block.getNamespace(), "block/" + block.getPath());
    }

    public String key(boolean small) {
        return (small ? "small:" : "normal:") + id();
    }

    public String registryName(IndustrialMaterial material, boolean small) {
        if (id().equals("stone")) {
            return (small ? MaterialPart.SMALL_ORE : MaterialPart.ORE).registryName(material);
        }
        return material.id() + "_" + (small ? "small_" : "") + id() + "_ore";
    }

    public String displayName(IndustrialMaterial material, boolean small) {
        return (small ? "Small " : "") + stone.displayName() + " " + material.displayName() + " Ore";
    }

    /** Compatibility bridge retained only for the dimension/base variants that still exist. */
    public Optional<MaterialPart> legacyPart(boolean small) {
        return Optional.ofNullable(switch (id()) {
            case "stone" -> small ? MaterialPart.SMALL_ORE : MaterialPart.ORE;
            case "deepslate" -> small ? MaterialPart.SMALL_DEEPSLATE_ORE : MaterialPart.DEEPSLATE_ORE;
            case "netherrack" -> small ? MaterialPart.SMALL_NETHERRACK_ORE : MaterialPart.NETHERRACK_ORE;
            case "blackstone" -> small ? MaterialPart.SMALL_BLACKSTONE_ORE : MaterialPart.BLACKSTONE_ORE;
            case "basalt" -> small ? MaterialPart.SMALL_BASALT_ORE : MaterialPart.BASALT_ORE;
            case "end_stone" -> small ? MaterialPart.SMALL_END_STONE_ORE : MaterialPart.END_STONE_ORE;
            default -> null;
        });
    }

    public Optional<ResourceLocation> existingOreBlock(IndustrialMaterial material, boolean small) {
        return legacyPart(small)
                .filter(material::hasExistingPart)
                .map(material::existingPart);
    }

    public boolean shouldGenerate(IndustrialMaterial material, boolean small) {
        return hasNaturalOre(material)
                && compatibleHosts(material).stream().anyMatch(host -> host.id().equals(id()))
                && existingOreBlock(material, small).isEmpty();
    }

    /** Only OreMaterials definitions own generated compound ore blocks/world deposits. */
    public static boolean hasNaturalOre(IndustrialMaterial material) {
        return material != null && material.atomicNumber() == 0 && material.isOreMaterial();
    }
}
