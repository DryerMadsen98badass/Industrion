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
     * Returns every StoneMaterial that fits this ore. More than one host is expected.
     *
     * <p>The StoneMaterial .contains(...) graph is the source of truth. Direct mineral occurrence
     * and shared fictional elemental leaves score highest; calculated bulk compatibility of the
     * trace minerals supplies the secondary score. No stone id is hard-coded here.</p>
     */
    public static List<MaterialOreHost> compatibleHosts(IndustrialMaterial material) {
        if (material == null || !hasNaturalOre(material)) return List.of();
        MaterialOrePolicy.DimensionBand dimension = MaterialTierResolver.geologyDimension(material);
        List<MaterialOreHost> candidates = forDimension(dimension);
        if (candidates.isEmpty()) return List.of();

        Map<MaterialOreHost, Integer> scores = new LinkedHashMap<>();
        int maximum = 0;
        for (MaterialOreHost host : candidates) {
            int score = host.affinity(material);
            scores.put(host, score);
            maximum = Math.max(maximum, score);
        }
        if (maximum <= 0) return List.of();

        int threshold = Math.max(1, (int) Math.ceil(maximum * 0.55D));
        return scores.entrySet().stream()
                .filter(entry -> entry.getValue() >= threshold)
                .sorted(Map.Entry.<MaterialOreHost, Integer>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().id()))
                .map(Map.Entry::getKey)
                .toList();
    }

    /** Higher means the stone's declared trace-mineral chemistry fits the ore better. */
    public int affinity(IndustrialMaterial ore) {
        if (ore == null || stone.components().isEmpty()) return 0;
        Set<String> oreLeaves = leafIds(ore);
        int score = 0;
        for (MaterialComponent component : stone.components()) {
            IndustrialSubstance trace = component.substance();
            int amount = Math.max(1, component.amount());
            if (trace.id().equals(ore.id())) score += amount * 240;

            Set<String> traceLeaves = leafIds(trace);
            int shared = 0;
            for (String leaf : oreLeaves) if (traceLeaves.contains(leaf)) shared++;
            score += amount * shared * 48;

            if (trace instanceof IndustrialMaterial traceMaterial) {
                score += amount * chemistryAffinity(ore.properties(), traceMaterial.properties());
            }
        }
        return score;
    }

    private static int chemistryAffinity(MaterialProperties ore, MaterialProperties trace) {
        double crystal = similarity(ore.crystalStability(), trace.crystalStability());
        double chemical = similarity(ore.chemicalStability(), trace.chemicalStability());
        double bond = similarity(ore.bondStrength(), trace.bondStrength());
        double pressure = similarity(ore.pressureResistance(), trace.pressureResistance());
        double reactivity = similarity(ore.reactivity(), trace.reactivity());
        double density = similarity(ore.density(), trace.density());
        double score = crystal * 0.24D + chemical * 0.20D + bond * 0.18D
                + pressure * 0.14D + reactivity * 0.12D + density * 0.12D;
        return Math.max(0, (int) Math.round(score / 5.0D));
    }

    private static double similarity(double a, double b) {
        return Math.max(0.0D, 100.0D - Math.min(100.0D, Math.abs(a - b)));
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

    public ResourceLocation blockModel() {
        ResourceLocation block = hostBlock();
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
