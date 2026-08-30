package net.mads.industron.worldgen;

import com.mojang.serialization.Codec;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialOreHost;
import net.mads.industron.material.MaterialOrePolicy;
import net.mads.industron.material.chemistry.ChemistryBootstrap;
import net.mads.industron.material.chemistry.geology.DepositDefinition;
import net.mads.industron.material.chemistry.geology.DepositGeometry;
import net.mads.industron.material.chemistry.geology.OreMineral;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chunk-local runtime placer for the 9x9 geology-region plan.
 *
 * <p>Every chunk independently reconstructs the same immutable region descriptor from world seed,
 * dimension and region coordinates, then touches only positions inside the currently generated
 * chunk. Generation order therefore cannot move a deposit and no neighbouring chunk is loaded.</p>
 */
public final class GeologyDepositFeature extends Feature<NoneFeatureConfiguration> {
    private static final int REGION_CHUNKS = 9;
    private static final int REGION_BLOCKS = REGION_CHUNKS * 16;
    private static final int HOST_HALO_BLOCKS = 2;
    private static volatile RuntimePlan runtimePlan;

    public GeologyDepositFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        int chunkX = Math.floorDiv(context.origin().getX(), 16);
        int chunkZ = Math.floorDiv(context.origin().getZ(), 16);
        int regionX = Math.floorDiv(chunkX, REGION_CHUNKS);
        int regionZ = Math.floorDiv(chunkZ, REGION_CHUNKS);
        String dimension = level.getLevel().dimension().location().toString();

        RuntimePlan plan = plan();
        List<RuntimeDeposit> eligible = plan.byDimension().getOrDefault(dimension, List.of());
        if (eligible.isEmpty()) return false;

        long regionSeed = regionSeed(level.getSeed(), dimension, regionX, regionZ);
        RuntimeDeposit selected = selectDeposit(eligible, regionSeed);
        if (selected == null) return false;

        Body body = body(selected.definition(), regionSeed, regionX, regionZ);
        return placeChunkSlice(level, chunkX, chunkZ, selected, body, regionSeed);
    }

    private static boolean placeChunkSlice(
            WorldGenLevel level,
            int chunkX,
            int chunkZ,
            RuntimeDeposit deposit,
            Body body,
            long seed
    ) {
        int minX = chunkX * 16;
        int minZ = chunkZ * 16;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        if (maxX < body.minX() - HOST_HALO_BLOCKS || minX > body.maxX() + HOST_HALO_BLOCKS
                || maxZ < body.minZ() - HOST_HALO_BLOCKS || minZ > body.maxZ() + HOST_HALO_BLOCKS) {
            return false;
        }

        int minimumY = Math.max(level.getMinBuildHeight(), Math.max(
                deposit.definition().conditions().minimumY(), body.minY() - HOST_HALO_BLOCKS));
        int maximumY = Math.min(level.getMaxBuildHeight() - 1, Math.min(
                deposit.definition().conditions().maximumY(), body.maxY() + HOST_HALO_BLOCKS));
        if (minimumY > maximumY) return false;

        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!body.horizontalCandidate(x, z, HOST_HALO_BLOCKS)) continue;

                int biomeY = Math.max(minimumY, Math.min(maximumY, (int) Math.round(body.centerY())));
                pos.set(x, biomeY, z);
                if (!matchesBiome(level, pos, deposit.definition().conditions().biomeTags())) continue;

                for (int y = minimumY; y <= maximumY; y++) {
                    if (!body.containsHalo(x, y, z, HOST_HALO_BLOCKS)) continue;
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    ResourceLocation currentId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    if (!deposit.replaceableHostBlocks().contains(currentId)) continue;

                    MineralTarget target = selectHostTarget(deposit.targets(), seed, x, y, z);
                    if (target == null || target.hostBlock() == null) continue;

                    boolean insideOreBody = body.contains(x, y, z);
                    boolean oreBand = insideOreBody && body.oreBand(x, y, z);
                    if (oreBand) {
                        double localGrade = body.localGrade(x, y, z, deposit.definition());
                        long positionSeed = mix64(seed ^ BlockPos.asLong(x, y, z) ^ 0x6F72655F62616E64L);
                        if (unitDouble(positionSeed) < localGrade && target.oreBlock() != null) {
                            level.setBlock(pos, target.oreBlock().defaultBlockState(), 2);
                            placed = true;
                            continue;
                        }
                    }

                    // Every non-ore cell in the rounded body/halo becomes one of the compatible
                    // StoneMaterial hosts. This creates the geological stone envelope around the
                    // vein instead of requiring the correct host stone to pre-exist in terrain.
                    level.setBlock(pos, target.hostBlock().defaultBlockState(), 2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    private static MineralTarget selectHostTarget(List<MineralTarget> targets, long seed, int x, int y, int z) {
        if (targets.isEmpty()) return null;
        if (targets.size() == 1) return targets.get(0);

        // Select by a coarse 4-block cell so compatible stones form patches/bands rather than
        // salt-and-pepper noise. Affinity is a weight, not an exclusive winner.
        int cellX = Math.floorDiv(x, 4);
        int cellY = Math.floorDiv(y, 4);
        int cellZ = Math.floorDiv(z, 4);
        long cell = BlockPos.asLong(cellX, cellY, cellZ);
        int total = targets.stream().mapToInt(value -> Math.max(1, value.affinity())).sum();
        int choice = bounded(mix64(seed ^ cell ^ 0x686F73745F706174L), total);
        for (MineralTarget target : targets) {
            choice -= Math.max(1, target.affinity());
            if (choice < 0) return target;
        }
        return targets.get(targets.size() - 1);
    }


    private static boolean matchesBiome(WorldGenLevel level, BlockPos pos, Set<String> criteria) {
        if (criteria == null || criteria.isEmpty()) return true;
        Holder<Biome> biome = level.getBiome(pos);
        for (String criterion : criteria) {
            if (criterion == null || criterion.isBlank()) continue;
            boolean tag = criterion.charAt(0) == '#';
            ResourceLocation id = ResourceLocation.tryParse(tag ? criterion.substring(1) : criterion);
            if (id == null) continue;
            if (tag) {
                if (biome.is(TagKey.create(Registries.BIOME, id))) return true;
            } else if (biome.is(ResourceKey.create(Registries.BIOME, id))) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeDeposit selectDeposit(List<RuntimeDeposit> deposits, long seed) {
        double total = deposits.stream().mapToDouble(value -> value.definition().rarity()).sum();
        if (!(total > 0.0D)) return null;
        double choice = unitDouble(mix64(seed ^ 0x4E554C4C44504F53L)) * total;
        for (RuntimeDeposit deposit : deposits) {
            choice -= deposit.definition().rarity();
            if (choice <= 0.0D) return deposit;
        }
        return deposits.get(deposits.size() - 1);
    }

    private static Body body(DepositDefinition deposit, long seed, int regionX, int regionZ) {
        long a = mix64(seed ^ 0x63A9F84D5B17E2C1L);
        long b = mix64(seed ^ 0x9E3779B97F4A7C15L);
        int margin = 8;
        int centerX = regionX * REGION_BLOCKS + margin
                + bounded(a, REGION_BLOCKS - margin * 2);
        int centerZ = regionZ * REGION_BLOCKS + margin
                + bounded(b, REGION_BLOCKS - margin * 2);

        int span = Math.max(0, deposit.maximumSize() - deposit.minimumSize());
        int nominal = deposit.minimumSize() + bounded(mix64(a ^ b), span + 1);
        double radius = clamp(nominal * 0.50D, 8.0D, 70.0D);
        int centerY = deposit.conditions().preferredY();
        int verticalSpread = Math.max(3, deposit.conditions().verticalSpread());
        int yJitter = boundedSigned(mix64(seed ^ 0xD1B54A32D192ED03L), Math.max(1, verticalSpread / 2));
        centerY = Math.max(deposit.conditions().minimumY(), Math.min(deposit.conditions().maximumY(), centerY + yJitter));

        double angle = unitDouble(mix64(seed ^ 0x94D049BB133111EBL)) * Math.PI * 2.0D;
        return Body.forGeometry(deposit.geometry(), centerX, centerY, centerZ, radius, verticalSpread, angle);
    }

    private static RuntimePlan plan() {
        RuntimePlan cached = runtimePlan;
        if (cached != null) return cached;
        synchronized (GeologyDepositFeature.class) {
            cached = runtimePlan;
            if (cached == null) {
                cached = buildRuntimePlan();
                runtimePlan = cached;
            }
        }
        return cached;
    }

    private static RuntimePlan buildRuntimePlan() {
        ChemistryBootstrap.AnalysisResult analysis = ChemistryBootstrap.currentOrAnalyze();
        Map<String, OreMineral> minerals = new LinkedHashMap<>();
        for (OreMineral mineral : analysis.geology().minerals()) minerals.put(mineral.id(), mineral);

        Map<String, IndustrialMaterial> materials = new LinkedHashMap<>();
        for (var materialAnalysis : analysis.analyses().values()) {
            if (materialAnalysis.source().backingMaterial() instanceof IndustrialMaterial material) {
                materials.put(material.id(), material);
            }
        }

        Map<String, List<RuntimeDeposit>> byDimension = new LinkedHashMap<>();
        for (DepositDefinition deposit : analysis.geology().deposits()) {
            RuntimeDeposit runtime = runtimeDeposit(deposit, minerals, materials);
            if (runtime == null) continue;
            for (String dimension : deposit.conditions().dimensions()) {
                byDimension.computeIfAbsent(dimension, ignored -> new ArrayList<>()).add(runtime);
            }
        }
        byDimension.replaceAll((key, value) -> value.stream()
                .sorted(Comparator.comparing(entry -> entry.definition().id()))
                .toList());
        return new RuntimePlan(Map.copyOf(byDimension));
    }

    private static RuntimeDeposit runtimeDeposit(
            DepositDefinition deposit,
            Map<String, OreMineral> minerals,
            Map<String, IndustrialMaterial> materials
    ) {
        IndustrialMaterial primaryMaterial = null;
        for (DepositDefinition.WeightedMineral weighted : deposit.primaryMinerals()) {
            OreMineral mineral = minerals.get(weighted.mineralId());
            if (mineral == null) continue;
            IndustrialMaterial candidate = materials.get(mineral.materialId());
            if (candidate != null && MaterialOreHost.hasNaturalOre(candidate)) {
                primaryMaterial = candidate;
                break;
            }
        }
        if (primaryMaterial == null) return null;

        Set<String> plannedHostIds = deposit.conditions().hostRockTags();
        List<MineralTarget> targets = new ArrayList<>();
        for (MaterialOreHost host : MaterialOreHost.compatibleHosts(primaryMaterial)) {
            ResourceLocation hostId = host.hostBlock();
            if (!plannedHostIds.isEmpty() && !plannedHostIds.contains(hostId.toString())) continue;
            Block ore = oreBlock(primaryMaterial, host);
            Block hostBlock = BuiltInRegistries.BLOCK.get(hostId);
            if (ore == null || hostBlock == null) continue;
            targets.add(new MineralTarget(
                    primaryMaterial.id(),
                    host,
                    hostBlock,
                    ore,
                    Math.max(1, host.affinity(primaryMaterial))
            ));
        }
        if (targets.isEmpty()) return null;
        targets.sort(Comparator.comparing((MineralTarget target) -> target.host().id()));

        Set<ResourceLocation> replaceable = new java.util.LinkedHashSet<>();
        MaterialOrePolicy.DimensionBand dimension = targets.get(0).host().dimension();
        for (MaterialOreHost host : MaterialOreHost.forDimension(dimension)) {
            replaceable.add(host.hostBlock());
        }
        return new RuntimeDeposit(deposit, List.copyOf(targets), Set.copyOf(replaceable));
    }


    private static Block oreBlock(IndustrialMaterial material, MaterialOreHost host) {
        ResourceLocation existing = host.existingOreBlock(material, false).orElse(null);
        if (existing != null) return BuiltInRegistries.BLOCK.get(existing);

        DeferredHolder<Block, ? extends Block> generated = BlockRegistry.getMaterialOreHostBlock(material, host, false);
        return generated == null ? null : generated.get();
    }

    private static long regionSeed(long worldSeed, String dimension, int regionX, int regionZ) {
        long seed = mix64(worldSeed ^ dimension.hashCode());
        seed = mix64(seed ^ ((long) regionX * 0x9E3779B97F4A7C15L));
        return mix64(seed ^ ((long) regionZ * 0xC2B2AE3D27D4EB4FL));
    }

    private static long mix64(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static double unitDouble(long value) {
        return (double) (value >>> 11) * 0x1.0p-53;
    }

    private static int bounded(long value, int bound) {
        if (bound <= 1) return 0;
        return (int) Math.floorMod(value, bound);
    }

    private static int boundedSigned(long value, int magnitude) {
        if (magnitude <= 0) return 0;
        return bounded(value, magnitude * 2 + 1) - magnitude;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record RuntimePlan(Map<String, List<RuntimeDeposit>> byDimension) {
    }

    private record RuntimeDeposit(
            DepositDefinition definition,
            List<MineralTarget> targets,
            Set<ResourceLocation> replaceableHostBlocks
    ) {
    }

    private record MineralTarget(
            String materialId,
            MaterialOreHost host,
            Block hostBlock,
            Block oreBlock,
            int affinity
    ) {
    }

    private record Body(
            DepositGeometry geometry,
            double centerX,
            double centerY,
            double centerZ,
            double radiusX,
            double radiusY,
            double radiusZ,
            double sin,
            double cos
    ) {
        static Body forGeometry(
                DepositGeometry geometry,
                int centerX,
                int centerY,
                int centerZ,
                double radius,
                int verticalSpread,
                double angle
        ) {
            double rx = radius;
            double rz = radius;
            double ry = Math.max(3.0D, Math.min(radius * 0.55D, verticalSpread));
            switch (geometry) {
                case VEIN, HYDROTHERMAL, PEGMATITE_LIKE, SKARN_LIKE -> {
                    rz = Math.max(3.0D, radius * 0.22D);
                    ry = Math.max(3.0D, Math.min(verticalSpread, radius * 0.28D));
                }
                case BANDED, LAYER, SEDIMENTARY, EVAPORITE -> ry = Math.max(2.0D, Math.min(7.0D, verticalSpread * 0.35D));
                case PIPE -> {
                    rx = Math.max(4.0D, radius * 0.30D);
                    rz = rx;
                    ry = Math.max(8.0D, verticalSpread * 1.4D);
                }
                case PLACER, OCEAN_FLOOR_MASS -> ry = Math.max(2.0D, Math.min(5.0D, verticalSpread * 0.25D));
                case PORPHYRY, DISSEMINATED, MAGMATIC, METAMORPHIC, LENS -> {
                    // Broad ellipsoids use the defaults.
                }
                case FLUID_RESERVOIR, GAS_RESERVOIR -> {
                    rx = radius * 0.75D;
                    rz = radius * 0.75D;
                }
            }
            return new Body(geometry, centerX, centerY, centerZ, rx, ry, rz, Math.sin(angle), Math.cos(angle));
        }

        int minX() { return (int) Math.floor(centerX - Math.max(radiusX, radiusZ)); }
        int maxX() { return (int) Math.ceil(centerX + Math.max(radiusX, radiusZ)); }
        int minY() { return (int) Math.floor(centerY - radiusY); }
        int maxY() { return (int) Math.ceil(centerY + radiusY); }
        int minZ() { return (int) Math.floor(centerZ - Math.max(radiusX, radiusZ)); }
        int maxZ() { return (int) Math.ceil(centerZ + Math.max(radiusX, radiusZ)); }

        boolean horizontalCandidate(int x, int z, int halo) {
            double[] rotated = rotate(x - centerX, z - centerZ);
            double nx = rotated[0] / (radiusX + Math.max(0, halo));
            double nz = rotated[1] / (radiusZ + Math.max(0, halo));
            return nx * nx + nz * nz <= 1.12D;
        }

        boolean contains(int x, int y, int z) {
            double[] rotated = rotate(x - centerX, z - centerZ);
            double nx = rotated[0] / radiusX;
            double ny = (y - centerY) / radiusY;
            double nz = rotated[1] / radiusZ;
            double distance = nx * nx + ny * ny + nz * nz;
            return distance <= switch (geometry) {
                case DISSEMINATED, PORPHYRY -> 1.0D;
                case BANDED, LAYER, SEDIMENTARY, EVAPORITE -> 1.08D;
                default -> 1.0D;
            };
        }


        boolean containsHalo(int x, int y, int z, int halo) {
            double[] rotated = rotate(x - centerX, z - centerZ);
            double nx = rotated[0] / (radiusX + Math.max(0, halo));
            double ny = (y - centerY) / (radiusY + Math.max(0, halo));
            double nz = rotated[1] / (radiusZ + Math.max(0, halo));
            return nx * nx + ny * ny + nz * nz <= 1.0D;
        }

        /**
         * Radial host/ore banding. From the outside inward the body starts with the two-block host
         * halo, then approximately five ore-capable blocks, two host-stone blocks and two more
         * ore-capable blocks. Deeper material repeats the 2-stone/5-ore/2-stone/2-ore rhythm.
         * Grade still controls how much of an ore-capable band is actual ore.
         */
        boolean oreBand(int x, int y, int z) {
            double radial = normalizedRadius(x, y, z);
            if (radial > 1.0D) return false;
            double scale = Math.max(3.0D, Math.min(radiusX, Math.min(radiusY, radiusZ)));
            double depth = Math.max(0.0D, (1.0D - radial) * scale);
            if (depth < 5.0D) return true;
            double cycle = (depth - 5.0D) % 11.0D;
            if (cycle < 2.0D) return false;
            if (cycle < 7.0D) return true;
            if (cycle < 9.0D) return false;
            return true;
        }

        private double normalizedRadius(int x, int y, int z) {
            double[] rotated = rotate(x - centerX, z - centerZ);
            double nx = rotated[0] / radiusX;
            double ny = (y - centerY) / radiusY;
            double nz = rotated[1] / radiusZ;
            return Math.sqrt(nx * nx + ny * ny + nz * nz);
        }
        double localGrade(int x, int y, int z, DepositDefinition deposit) {
            double[] rotated = rotate(x - centerX, z - centerZ);
            double nx = rotated[0] / radiusX;
            double ny = (y - centerY) / radiusY;
            double nz = rotated[1] / radiusZ;
            double radial = Math.min(1.0D, Math.sqrt(nx * nx + ny * ny + nz * nz));
            double grade = deposit.minimumGrade()
                    + (deposit.maximumGrade() - deposit.minimumGrade()) * (1.0D - radial);
            if (radial < 0.28D) {
                grade = grade + (deposit.maximumGrade() - grade) * deposit.highGradeCoreChance();
            }
            return clamp(grade, 0.001D, 1.0D);
        }

        private double[] rotate(double dx, double dz) {
            return new double[]{dx * cos + dz * sin, -dx * sin + dz * cos};
        }
    }
}
