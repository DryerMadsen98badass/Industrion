package net.mads.industron.worldgen;

import com.mojang.serialization.Codec;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.ClayMaterialRules;
import net.mads.industron.material.MaterialCatalog;
import net.mads.industron.material.MaterialOreHost;
import net.mads.industron.material.MaterialOrePolicy;
import net.mads.industron.material.MaterialTierResolver;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.chemistry.ChemistryBootstrap;
import net.mads.industron.material.chemistry.geology.DepositDefinition;
import net.mads.industron.material.chemistry.geology.DepositGeometry;
import net.mads.industron.material.chemistry.geology.OreMineral;
import net.mads.industron.material.defenitions.ClayMaterials;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cached, deterministic, chunk-local geology placer.
 *
 * <p>Every 9x9-chunk region owns exactly one immutable {@link RegionPlan}. The plan chooses one
 * anchor chunk, ore/deposit type, center, geometry, hosts and secondaries from world seed + region
 * coordinates. Plans are cached in a {@link ConcurrentHashMap}, so NeoForge's own parallel chunk
 * generation can safely reuse them from several worker threads without rebuilding the same region.
 * This class deliberately does not create its own worker threads: world/chunk mutation must stay on
 * Minecraft's worldgen execution path.</p>
 *
 * <p>Large ore bodies remain block-accurate, but broad prospecting geology is generated as coarse,
 * deterministic 3D patches instead of scanning every block in the full regional ellipsoid. Each
 * chunk only writes its own 16x16 slice, so deposits may cross 9x9-region borders without loading
 * or recursively generating neighbouring chunks.</p>
 */
public final class GeologyDepositFeature extends Feature<NoneFeatureConfiguration> {
    private static final int REGION_CHUNKS = 9;
    private static final int REGION_BLOCKS = REGION_CHUNKS * 16;
    private static final int HOST_HALO_BLOCKS = 2;

    private static final double MAX_BODY_RADIUS = 70.0D;
    private static final double REGIONAL_GEOLOGY_SCALE = 2.10D;
    private static final double SMALL_ORE_SCALE = 1.35D;
    private static final double GEOLOGY_WARP = 0.12D;
    private static final int MAX_REGIONAL_FIELDS_PER_CHUNK = 2;

    // Regional geology is sampled on a coarse global grid, then expanded into small irregular
    // patches. This keeps the visual signal broad while cutting the number of block-state probes
    // by orders of magnitude compared with a full 3D scan.
    private static final int GEOLOGY_CELL_XZ = 6;
    private static final int GEOLOGY_CELL_Y = 5;
    private static final int GEOLOGY_MAX_PATCH_RADIUS_XZ = 2;
    private static final int GEOLOGY_NOISE_XZ = 24;

    // Precise bodies are generated from deterministic, non-overlapping 3x3x3 cells. A selected
    // cell expands into a compact ore/host patch, so an extensive deposit keeps its physical size
    // without probing every block in its ellipsoid. The target curve closely follows the intended
    // radius-to-ore-count progression (roughly 16k ore blocks at radius 66).
    private static final int BODY_CELL_XZ = 3;
    private static final int BODY_CELL_Y = 3;
    private static final int BODY_PATCH_RADIUS_XZ = 1;
    private static final int BODY_PATCH_RADIUS_Y = 1;
    private static final double TARGET_ORE_PER_RADIUS_CUBED = 0.055D;
    private static final double ESTIMATED_ORE_BAND_FRACTION = 0.72D;
    private static final double ORE_PATCH_FILL_CHANCE = 0.82D;
    private static final double BODY_HOST_CELL_CHANCE = 0.16D;
    private static final double HALO_HOST_CELL_CHANCE = 0.08D;
    private static final double HOST_PATCH_FILL_CHANCE = 0.76D;

    private static final int REGION_SEARCH_RADIUS = (int) Math.ceil(
            (MAX_BODY_RADIUS * REGIONAL_GEOLOGY_SCALE * (1.0D + GEOLOGY_WARP)) / REGION_BLOCKS);

    private static volatile RuntimePlan runtimePlan;
    private static final Map<RegionPlanKey, RegionPlan> REGION_PLAN_CACHE = new ConcurrentHashMap<>();

    public GeologyDepositFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** Result used by the admin/debug geology locator command. */
    public record LocatedDeposit(
            String depositId,
            String primaryMaterialId,
            int x,
            int y,
            int z,
            DepositGeometry geometry,
            double distanceBlocks
    ) {
    }

    /**
     * Chooses the clay family for one chunk from the same deterministic 9x9 geology plans used by
     * ore bodies and regional stone signals. Clay tier is a hard dimension gate; composition only
     * decides which of the allowed clays best matches the local host rock + mineral signature.
     *
     * <p>The caller should normally sample once at the chunk centre so a vanilla clay patch cannot
     * change material halfway through a block cluster.</p>
     */
    public static IndustrialMaterial selectClayFor(WorldGenLevel level, BlockPos samplePos) {
        if (level == null || samplePos == null) return ClayMaterials.CLAY;

        String dimension = level.getLevel().dimension().location().toString();
        MaterialOrePolicy.DimensionBand dimensionBand = dimensionBand(dimension);
        if (dimensionBand == null) return ClayMaterials.CLAY;

        List<IndustrialMaterial> candidates = ClayMaterials.ALL.stream()
                .filter(IndustrialMaterial::isClayMaterial)
                .filter(clay -> ClayMaterialRules.worldgenDimension(clay) == dimensionBand)
                .toList();
        if (candidates.isEmpty()) return ClayMaterials.CLAY;

        List<RuntimeDeposit> eligible = plan().byDimension().getOrDefault(dimension, List.of());
        if (eligible.isEmpty()) return fallbackClay(candidates);

        int chunkX = Math.floorDiv(samplePos.getX(), 16);
        int chunkZ = Math.floorDiv(samplePos.getZ(), 16);
        int localRegionX = Math.floorDiv(chunkX, REGION_CHUNKS);
        int localRegionZ = Math.floorDiv(chunkZ, REGION_CHUNKS);

        Map<IndustrialMaterial, Double> scores = new LinkedHashMap<>();
        for (IndustrialMaterial clay : candidates) scores.put(clay, 0.0D);

        for (int regionX = localRegionX - REGION_SEARCH_RADIUS;
             regionX <= localRegionX + REGION_SEARCH_RADIUS;
             regionX++) {
            for (int regionZ = localRegionZ - REGION_SEARCH_RADIUS;
                 regionZ <= localRegionZ + REGION_SEARCH_RADIUS;
                 regionZ++) {
                RegionPlan region = regionPlan(level.getLevel().getSeed(), dimension, regionX, regionZ, eligible);
                if (region == null) continue;

                Body body = region.body();
                double horizontalSquared = body.horizontalRadiusSquared(samplePos.getX(), samplePos.getZ());
                double outerScale = body.geologyOuterScale(samplePos.getX(), samplePos.getZ(), region.seed());
                double radial = Math.sqrt(horizontalSquared);
                double influence = radial <= 1.0D ? 1.0D : body.geologyIntensity(radial, outerScale);

                // The owning 9x9 plan is always a weak background signal, even where its visible
                // regional stone halo does not physically reach. Neighbour plans contribute only
                // when their actual geology field overlaps this chunk.
                if (regionX == localRegionX && regionZ == localRegionZ) {
                    influence = Math.max(0.25D, influence);
                }
                if (influence <= 0.0D) continue;

                RuntimeDeposit deposit = region.deposit();
                HostPalette hostPalette = !deposit.geologyHosts().isEmpty()
                        ? deposit.geologyHosts()
                        : deposit.bodyHosts();
                HostTarget host = selectHostTarget(
                        hostPalette,
                        region.seed(),
                        samplePos.getX(),
                        (int) Math.round(body.centerY()),
                        samplePos.getZ(),
                        GEOLOGY_CELL_XZ * 2
                );

                for (IndustrialMaterial clay : candidates) {
                    int stoneScore = host == null ? 0 : ClayMaterialRules.stoneAffinity(clay, host.host());
                    int mineralScore = clayMineralAffinity(clay, deposit);

                    // Clay is primarily weathered/local rock; ore/mineral occurrence is a weaker
                    // secondary clue. Both signals come from the same cached region plan.
                    double geologyScore = stoneScore * 0.72D + mineralScore * 0.28D;
                    scores.put(clay, scores.getOrDefault(clay, 0.0D) + geologyScore * influence);
                }
            }
        }

        IndustrialMaterial best = fallbackClay(candidates);
        double bestScore = scores.getOrDefault(best, 0.0D);
        for (IndustrialMaterial clay : candidates) {
            double score = scores.getOrDefault(clay, 0.0D);
            if (score > bestScore + 0.0001D
                    || (Math.abs(score - bestScore) <= 0.0001D && clay.id().compareTo(best.id()) < 0)) {
                best = clay;
                bestScore = score;
            }
        }
        return bestScore > 0.0D ? best : fallbackClay(candidates);
    }

    private static int clayMineralAffinity(IndustrialMaterial clay, RuntimeDeposit deposit) {
        long weighted = 0L;
        int total = 0;
        for (RuntimeMineral mineral : deposit.minerals()) {
            IndustrialMaterial material = MaterialCatalog.find(mineral.materialId());
            if (material == null) continue;
            int weight = Math.max(1, mineral.weight());
            weighted += (long) weight * ClayMaterialRules.materialAffinity(clay, material);
            total += weight;
        }
        return total <= 0 ? 0 : (int) Math.min(10_000L, weighted / total);
    }

    private static IndustrialMaterial fallbackClay(List<IndustrialMaterial> candidates) {
        if (candidates.contains(ClayMaterials.CLAY)) return ClayMaterials.CLAY;
        return candidates.get(0);
    }

    private static MaterialOrePolicy.DimensionBand dimensionBand(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld" -> MaterialOrePolicy.DimensionBand.OVERWORLD;
            case "minecraft:the_nether" -> MaterialOrePolicy.DimensionBand.NETHER;
            case "minecraft:the_end" -> MaterialOrePolicy.DimensionBand.END;
            default -> null;
        };
    }

    /** One non-base stone signal active at a surface position, derived from the same cached geology plan. */
    public record SurfaceStoneCandidate(StoneMaterial stone, int weight) {
    }

    /**
     * Returns non-base StoneMaterials whose real regional geology field reaches this X/Z position.
     * This is intentionally derived from the same 9x9 {@link RegionPlan}s and geology host palettes
     * that place the underground/regional stone signal, so surface indicators such as Pebbles never
     * invent a stone merely because it exists somewhere in the dimension.
     */
    public static List<SurfaceStoneCandidate> surfaceStoneCandidates(WorldGenLevel level, BlockPos samplePos) {
        if (level == null || samplePos == null) return List.of();
        String dimension = level.getLevel().dimension().location().toString();
        List<RuntimeDeposit> eligible = plan().byDimension().getOrDefault(dimension, List.of());
        if (eligible.isEmpty()) return List.of();

        int chunkX = Math.floorDiv(samplePos.getX(), 16);
        int chunkZ = Math.floorDiv(samplePos.getZ(), 16);
        int localRegionX = Math.floorDiv(chunkX, REGION_CHUNKS);
        int localRegionZ = Math.floorDiv(chunkZ, REGION_CHUNKS);
        Map<StoneMaterial, Double> scores = new LinkedHashMap<>();

        for (int regionX = localRegionX - REGION_SEARCH_RADIUS;
             regionX <= localRegionX + REGION_SEARCH_RADIUS; regionX++) {
            for (int regionZ = localRegionZ - REGION_SEARCH_RADIUS;
                 regionZ <= localRegionZ + REGION_SEARCH_RADIUS; regionZ++) {
                RegionPlan region = regionPlan(level.getLevel().getSeed(), dimension, regionX, regionZ, eligible);
                if (region == null) continue;

                Body body = region.body();
                double radial = Math.sqrt(body.horizontalRadiusSquared(samplePos.getX(), samplePos.getZ()));
                double outerScale = body.geologyOuterScale(samplePos.getX(), samplePos.getZ(), region.seed());
                double influence = radial <= 1.0D ? 1.0D : body.geologyIntensity(radial, outerScale);
                if (influence <= 0.0D) continue;

                RuntimeDeposit deposit = region.deposit();
                HostPalette palette = !deposit.geologyHosts().isEmpty()
                        ? deposit.geologyHosts()
                        : deposit.bodyHosts();
                for (HostTarget target : palette.targets()) {
                    StoneMaterial stone = target.host().stone();
                    if (stone.isBaseRock()) continue;
                    scores.merge(stone, influence * Math.max(1, target.affinity()), Double::sum);
                }
            }
        }

        return scores.entrySet().stream()
                .filter(entry -> entry.getValue() > 0.0D)
                .sorted(Map.Entry.<StoneMaterial, Double>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().id()))
                .map(entry -> new SurfaceStoneCandidate(
                        entry.getKey(),
                        Math.max(1, (int) Math.round(entry.getValue() * 100.0D))
                ))
                .toList();
    }

    /** Registered dedicated ore ids currently present in the runtime geology plan. */
    public static List<String> oreIds() {
        return plan().byDimension().values().stream()
                .flatMap(List::stream)
                .flatMap(deposit -> deposit.minerals().stream())
                .map(RuntimeMineral::materialId)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Finds the nearest deterministic planned deposit containing the requested ore as primary or
     * secondary. This uses the same cached region plans as worldgen and never scans ore blocks.
     */
    public static LocatedDeposit locateNearest(
            ServerLevel level,
            BlockPos origin,
            String oreId,
            int maximumRegionRadius
    ) {
        if (level == null || origin == null || oreId == null || oreId.isBlank()) return null;
        String normalizedOre = oreId.trim().toLowerCase(java.util.Locale.ROOT);
        String dimension = level.dimension().location().toString();
        List<RuntimeDeposit> eligible = plan().byDimension().getOrDefault(dimension, List.of());
        if (eligible.isEmpty()) return null;

        int originChunkX = Math.floorDiv(origin.getX(), 16);
        int originChunkZ = Math.floorDiv(origin.getZ(), 16);
        int originRegionX = Math.floorDiv(originChunkX, REGION_CHUNKS);
        int originRegionZ = Math.floorDiv(originChunkZ, REGION_CHUNKS);
        int radius = Math.max(0, maximumRegionRadius);

        LocatedDeposit best = null;
        double bestDistanceSquared = Double.POSITIVE_INFINITY;
        for (int regionX = originRegionX - radius; regionX <= originRegionX + radius; regionX++) {
            for (int regionZ = originRegionZ - radius; regionZ <= originRegionZ + radius; regionZ++) {
                RegionPlan region = regionPlan(level.getSeed(), dimension, regionX, regionZ, eligible);
                if (region == null || !region.deposit().containsMaterial(normalizedOre)) continue;

                Body body = region.body();
                double dx = body.centerX() - origin.getX();
                double dy = body.centerY() - origin.getY();
                double dz = body.centerZ() - origin.getZ();
                double distanceSquared = dx * dx + dy * dy + dz * dz;
                if (distanceSquared >= bestDistanceSquared) continue;

                bestDistanceSquared = distanceSquared;
                best = new LocatedDeposit(
                        region.deposit().definition().id(),
                        region.deposit().primaryMaterialId(),
                        (int) Math.round(body.centerX()),
                        (int) Math.round(body.centerY()),
                        (int) Math.round(body.centerZ()),
                        region.deposit().definition().geometry(),
                        Math.sqrt(distanceSquared)
                );
            }
        }
        return best;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        int chunkX = Math.floorDiv(context.origin().getX(), 16);
        int chunkZ = Math.floorDiv(context.origin().getZ(), 16);
        int localRegionX = Math.floorDiv(chunkX, REGION_CHUNKS);
        int localRegionZ = Math.floorDiv(chunkZ, REGION_CHUNKS);
        String dimension = level.getLevel().dimension().location().toString();

        List<RuntimeDeposit> eligible = plan().byDimension().getOrDefault(dimension, List.of());
        if (eligible.isEmpty()) return false;

        List<ChunkCandidate> bodyCandidates = new ArrayList<>(4);
        List<ChunkCandidate> regionalCandidates = new ArrayList<>(8);
        double chunkCenterX = chunkX * 16.0D + 7.5D;
        double chunkCenterZ = chunkZ * 16.0D + 7.5D;

        // This remains a 5x5 region lookup in the worst case, but it is now only a set of cheap
        // ConcurrentHashMap reads + bounding-box tests. The expensive ore/host/geometry plan for a
        // region is computed once and shared by all chunks that touch it.
        for (int regionX = localRegionX - REGION_SEARCH_RADIUS;
             regionX <= localRegionX + REGION_SEARCH_RADIUS;
             regionX++) {
            for (int regionZ = localRegionZ - REGION_SEARCH_RADIUS;
                 regionZ <= localRegionZ + REGION_SEARCH_RADIUS;
                 regionZ++) {
                RegionPlan region = regionPlan(level.getSeed(), dimension, regionX, regionZ, eligible);
                if (region == null) continue;

                Body body = region.body();
                if (!body.couldReachChunk(chunkX, chunkZ, REGIONAL_GEOLOGY_SCALE, GEOLOGY_WARP)) continue;

                double dx = body.centerX() - chunkCenterX;
                double dz = body.centerZ() - chunkCenterZ;
                ChunkCandidate candidate = new ChunkCandidate(region, dx * dx + dz * dz);

                if (body.couldReachChunkWithHalo(chunkX, chunkZ, HOST_HALO_BLOCKS)) {
                    bodyCandidates.add(candidate);
                }
                if (!region.deposit().geologyHosts().isEmpty()) {
                    regionalCandidates.add(candidate);
                }
            }
        }

        // Broad regional stone is deliberately placed first. Precise ore bodies/host halos are
        // placed afterwards, so ore geology always wins when formations overlap.
        regionalCandidates.sort(Comparator
                .comparingDouble(ChunkCandidate::distanceSquared)
                .thenComparingInt(candidate -> candidate.region().regionX())
                .thenComparingInt(candidate -> candidate.region().regionZ()));

        boolean placed = false;
        int regionalCount = Math.min(MAX_REGIONAL_FIELDS_PER_CHUNK, regionalCandidates.size());
        for (int i = 0; i < regionalCount; i++) {
            placed |= placeRegionalGeologyPatches(level, chunkX, chunkZ, regionalCandidates.get(i).region());
        }

        bodyCandidates.sort(Comparator
                .comparingInt((ChunkCandidate candidate) -> candidate.region().regionX())
                .thenComparingInt(candidate -> candidate.region().regionZ()));
        for (ChunkCandidate candidate : bodyCandidates) {
            placed |= placeBodySlice(level, chunkX, chunkZ, candidate.region());
        }
        return placed;
    }

    /**
     * Precise ore + host halo generation from deterministic 3D cells.
     *
     * <p>The old implementation walked every replaceable block inside the body/halo and changed
     * almost every non-ore position into host stone. A broad radius-70 body could therefore cause
     * hundreds of thousands of block-state reads and writes. This implementation evaluates the
     * cheap shape math once per 3x3x3 cell and touches world state only for selected patches. Ore
     * density is budgeted from the nominal radius, independently of geometry volume.</p>
     */
    private static boolean placeBodySlice(
            WorldGenLevel level,
            int chunkX,
            int chunkZ,
            RegionPlan region
    ) {
        RuntimeDeposit deposit = region.deposit();
        Body body = region.body();
        long seed = region.seed();

        int minX = chunkX * 16;
        int minZ = chunkZ * 16;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        int minimumY = Math.max(level.getMinBuildHeight(), deposit.definition().conditions().minimumY());
        int maximumY = Math.min(level.getMaxBuildHeight() - 1, deposit.definition().conditions().maximumY());
        if (minimumY > maximumY) return false;

        double shapeLimit = body.radiusSquaredLimit();
        double haloRadiusX = body.radiusX() + HOST_HALO_BLOCKS;
        double haloRadiusY = body.radiusY() + HOST_HALO_BLOCKS;
        double haloRadiusZ = body.radiusZ() + HOST_HALO_BLOCKS;
        double nominalRadius = body.nominalRadius();
        double targetOreBlocks = TARGET_ORE_PER_RADIUS_CUBED
                * nominalRadius * nominalRadius * nominalRadius;
        double estimatedBodyVolume = body.estimatedVolume();
        double baseOreCellChance = clamp(
                targetOreBlocks / Math.max(1.0D, estimatedBodyVolume
                        * ESTIMATED_ORE_BAND_FRACTION * ORE_PATCH_FILL_CHANCE),
                0.002D, 0.92D);

        int cellMinX = Math.floorDiv(minX - BODY_PATCH_RADIUS_XZ, BODY_CELL_XZ);
        int cellMaxX = Math.floorDiv(maxX + BODY_PATCH_RADIUS_XZ, BODY_CELL_XZ);
        int cellMinZ = Math.floorDiv(minZ - BODY_PATCH_RADIUS_XZ, BODY_CELL_XZ);
        int cellMaxZ = Math.floorDiv(maxZ + BODY_PATCH_RADIUS_XZ, BODY_CELL_XZ);
        int cellMinY = Math.floorDiv(
                Math.max(minimumY, (int) Math.floor(body.centerY() - haloRadiusY)) - BODY_PATCH_RADIUS_Y,
                BODY_CELL_Y);
        int cellMaxY = Math.floorDiv(
                Math.min(maximumY, (int) Math.ceil(body.centerY() + haloRadiusY)) + BODY_PATCH_RADIUS_Y,
                BODY_CELL_Y);

        // A cell whose centre is just outside the mathematical body may still have blocks inside
        // it. This normalized margin prevents clipped edges without returning to a dense scan.
        double bodyBoundaryMargin = Math.sqrt(
                square(BODY_PATCH_RADIUS_XZ / body.radiusX())
                        + square(BODY_PATCH_RADIUS_Y / body.radiusY())
                        + square(BODY_PATCH_RADIUS_XZ / body.radiusZ()));
        double haloBoundaryMargin = Math.sqrt(
                square(BODY_PATCH_RADIUS_XZ / haloRadiusX)
                        + square(BODY_PATCH_RADIUS_Y / haloRadiusY)
                        + square(BODY_PATCH_RADIUS_XZ / haloRadiusZ));
        double bodyCellLimit = square(Math.sqrt(shapeLimit) + bodyBoundaryMargin);
        double haloCellLimit = square(1.0D + haloBoundaryMargin);

        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        byte[][] biomeCache = new byte[4][4];

        for (int cellX = cellMinX; cellX <= cellMaxX; cellX++) {
            int centerX = cellX * BODY_CELL_XZ + BODY_CELL_XZ / 2;
            for (int cellZ = cellMinZ; cellZ <= cellMaxZ; cellZ++) {
                int centerZ = cellZ * BODY_CELL_XZ + BODY_CELL_XZ / 2;
                for (int cellY = cellMinY; cellY <= cellMaxY; cellY++) {
                    int centerY = cellY * BODY_CELL_Y + BODY_CELL_Y / 2;
                    double centerBodyRadiusSquared = body.radiusSquaredAt(centerX, centerY, centerZ);
                    double centerHaloRadiusSquared = body.radiusSquaredAt(
                            centerX, centerY, centerZ, haloRadiusX, haloRadiusY, haloRadiusZ);
                    boolean couldContainBody = centerBodyRadiusSquared <= bodyCellLimit;
                    boolean couldContainHalo = centerHaloRadiusSquared <= haloCellLimit;
                    if (!couldContainBody && !couldContainHalo) continue;

                    long cellSeed = mix64(seed
                            ^ ((long) cellX * 0x9E3779B97F4A7C15L)
                            ^ ((long) cellY * 0xC2B2AE3D27D4EB4FL)
                            ^ ((long) cellZ * 0x165667B19E3779F9L)
                            ^ 0x424F44595F43454CL);

                    double radial = Math.min(1.0D, Math.sqrt(Math.max(0.0D, centerBodyRadiusSquared)));
                    double gradeFactor = deposit.definition().grade() <= 0.0D ? 1.0D
                            : body.localGrade(radial, deposit.definition()) / deposit.definition().grade();
                    double oreCellChance = clamp(baseOreCellChance * clamp(gradeFactor, 0.45D, 1.80D),
                            0.0D, 0.92D);
                    boolean oreCell = couldContainBody
                            && body.oreBand(radial)
                            && unitDouble(mix64(cellSeed ^ 0x4F52455F43454C4CL)) < oreCellChance;

                    double hostCellChance = couldContainBody
                            ? (oreCell ? 0.72D : BODY_HOST_CELL_CHANCE)
                            : HALO_HOST_CELL_CHANCE;
                    boolean hostCell = unitDouble(mix64(cellSeed ^ 0x484F53545F43454CL)) < hostCellChance;
                    if (!oreCell && !hostCell) continue;

                    int sampleX = Math.max(minX, Math.min(maxX, centerX));
                    int sampleZ = Math.max(minZ, Math.min(maxZ, centerZ));
                    int biomeX = (sampleX - minX) >> 2;
                    int biomeZ = (sampleZ - minZ) >> 2;
                    byte biomeState = biomeCache[biomeX][biomeZ];
                    if (biomeState == 0) {
                        pos.set(sampleX, Math.max(minimumY, Math.min(maximumY, centerY)), sampleZ);
                        biomeState = (byte) (matchesBiome(level, pos, deposit.biomeFilter()) ? 1 : 2);
                        biomeCache[biomeX][biomeZ] = biomeState;
                    }
                    if (biomeState == 2) continue;

                    HostTarget host = selectHostTarget(
                            deposit.bodyHosts(), seed, centerX, centerY, centerZ, BODY_CELL_XZ * 2);
                    if (host == null) continue;

                    for (int x = Math.max(minX, centerX - BODY_PATCH_RADIUS_XZ);
                         x <= Math.min(maxX, centerX + BODY_PATCH_RADIUS_XZ); x++) {
                        for (int z = Math.max(minZ, centerZ - BODY_PATCH_RADIUS_XZ);
                             z <= Math.min(maxZ, centerZ + BODY_PATCH_RADIUS_XZ); z++) {
                            for (int y = Math.max(minimumY, centerY - BODY_PATCH_RADIUS_Y);
                                 y <= Math.min(maximumY, centerY + BODY_PATCH_RADIUS_Y); y++) {
                                double bodyRadiusSquared = body.radiusSquaredAt(x, y, z);
                                boolean insideBody = bodyRadiusSquared <= shapeLimit;
                                boolean insideHalo = !insideBody
                                        && body.radiusSquaredAt(x, y, z,
                                        haloRadiusX, haloRadiusY, haloRadiusZ) <= 1.0D;
                                if (!insideBody && !insideHalo) continue;

                                long positionSeed = mix64(cellSeed ^ BlockPos.asLong(x, y, z));
                                boolean placeOre = false;
                                if (oreCell && insideBody) {
                                    double blockRadial = Math.sqrt(Math.max(0.0D, bodyRadiusSquared));
                                    placeOre = body.oreBand(blockRadial)
                                            && unitDouble(mix64(positionSeed ^ 0x4F52455F46494C4CL))
                                            < ORE_PATCH_FILL_CHANCE;
                                }
                                boolean placeHost = !placeOre && hostCell
                                        && unitDouble(mix64(positionSeed ^ 0x484F53545F46494CL))
                                        < HOST_PATCH_FILL_CHANCE;
                                if (!placeOre && !placeHost) continue;

                                pos.set(x, y, z);
                                BlockState state = level.getBlockState(pos);
                                if (!deposit.replaceableHostBlocks().contains(state.getBlock())) continue;

                                if (placeOre) {
                                    Block oreBlock = selectOreBlock(
                                            deposit, host.host().id(), false, positionSeed);
                                    if (oreBlock != null && !state.is(oreBlock)) {
                                        level.setBlock(pos, oreBlock.defaultBlockState(), 2);
                                        placed = true;
                                    }
                                } else if (!state.is(host.hostBlock())) {
                                    level.setBlock(pos, host.hostBlock().defaultBlockState(), 2);
                                    placed = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return placed;
    }

    /**
     * Broad prospecting geology using deterministic coarse cells + small patches instead of a
     * block-by-block ellipsoid scan. A neighbouring chunk independently computes the same global
     * cells and writes only the part that lies inside itself, so formations cross chunk/region
     * borders cleanly without cascading generation.
     */
    private static boolean placeRegionalGeologyPatches(
            WorldGenLevel level,
            int chunkX,
            int chunkZ,
            RegionPlan region
    ) {
        RuntimeDeposit deposit = region.deposit();
        if (deposit.geologyHosts().isEmpty()) return false;

        Body body = region.body();
        long seed = region.seed();
        int minX = chunkX * 16;
        int minZ = chunkZ * 16;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        int minimumY = Math.max(level.getMinBuildHeight(), Math.max(
                deposit.definition().conditions().minimumY(),
                body.scaledMinY(REGIONAL_GEOLOGY_SCALE, GEOLOGY_WARP)));
        int maximumY = Math.min(level.getMaxBuildHeight() - 1, Math.min(
                deposit.definition().conditions().maximumY(),
                body.scaledMaxY(REGIONAL_GEOLOGY_SCALE, GEOLOGY_WARP)));
        if (minimumY > maximumY) return false;

        int cellMinX = Math.floorDiv(minX - GEOLOGY_MAX_PATCH_RADIUS_XZ, GEOLOGY_CELL_XZ);
        int cellMaxX = Math.floorDiv(maxX + GEOLOGY_MAX_PATCH_RADIUS_XZ, GEOLOGY_CELL_XZ);
        int cellMinZ = Math.floorDiv(minZ - GEOLOGY_MAX_PATCH_RADIUS_XZ, GEOLOGY_CELL_XZ);
        int cellMaxZ = Math.floorDiv(maxZ + GEOLOGY_MAX_PATCH_RADIUS_XZ, GEOLOGY_CELL_XZ);
        int cellMinY = Math.floorDiv(minimumY - 1, GEOLOGY_CELL_Y);
        int cellMaxY = Math.floorDiv(maximumY + 1, GEOLOGY_CELL_Y);

        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double bodyLimit = body.radiusSquaredLimit();

        for (int cellX = cellMinX; cellX <= cellMaxX; cellX++) {
            int centerX = cellX * GEOLOGY_CELL_XZ + GEOLOGY_CELL_XZ / 2;
            for (int cellZ = cellMinZ; cellZ <= cellMaxZ; cellZ++) {
                int centerZ = cellZ * GEOLOGY_CELL_XZ + GEOLOGY_CELL_XZ / 2;
                double outerScale = body.geologyOuterScale(centerX, centerZ, seed);
                double horizontalSquared = body.horizontalRadiusSquared(centerX, centerZ);
                if (horizontalSquared >= outerScale * outerScale) continue;

                double maxVerticalNormalized = Math.sqrt(Math.max(0.0D,
                        outerScale * outerScale - horizontalSquared));
                int localMinY = Math.max(cellMinY,
                        Math.floorDiv((int) Math.floor(body.centerY() - body.radiusY() * maxVerticalNormalized) - 1,
                                GEOLOGY_CELL_Y));
                int localMaxY = Math.min(cellMaxY,
                        Math.floorDiv((int) Math.ceil(body.centerY() + body.radiusY() * maxVerticalNormalized) + 1,
                                GEOLOGY_CELL_Y));
                if (localMinY > localMaxY) continue;

                for (int cellY = localMinY; cellY <= localMaxY; cellY++) {
                    int centerY = cellY * GEOLOGY_CELL_Y + GEOLOGY_CELL_Y / 2;
                    if (centerY < minimumY || centerY > maximumY) continue;

                    double ny = (centerY - body.centerY()) / body.radiusY();
                    double radialSquared = horizontalSquared + ny * ny;
                    if (radialSquared <= bodyLimit || radialSquared >= outerScale * outerScale) continue;

                    double radial = Math.sqrt(radialSquared);
                    double intensity = body.geologyIntensity(radial, outerScale);
                    if (intensity <= 0.0D) continue;

                    long cellSeed = mix64(seed
                            ^ ((long) cellX * 0x9E3779B97F4A7C15L)
                            ^ ((long) cellY * 0xC2B2AE3D27D4EB4FL)
                            ^ ((long) cellZ * 0x165667B19E3779F9L)
                            ^ 0x47454F5F50415443L);
                    double patchChance = clamp(0.055D + Math.pow(intensity, 0.78D) * 0.50D, 0.0D, 0.56D);
                    if (unitDouble(cellSeed) >= patchChance) continue;

                    pos.set(centerX, centerY, centerZ);
                    if (!matchesBiome(level, pos, deposit.biomeFilter())) continue;

                    HostTarget geologyHost = selectHostTarget(
                            deposit.geologyHosts(), seed, centerX, centerY, centerZ, GEOLOGY_CELL_XZ * 2);
                    if (geologyHost == null) continue;

                    int patchRadiusXZ = 1;
                    if (intensity > 0.72D && unitDouble(mix64(cellSeed ^ 0x50415443485F4247L)) < 0.28D) {
                        patchRadiusXZ = 2;
                    }
                    int patchRadiusY = intensity > 0.30D ? 1 : 0;

                    // Sparse small ore is one deterministic clue per selected patch at most; we do
                    // not test every stone block for small-ore chance anymore.
                    if (radial <= SMALL_ORE_SCALE) {
                        double inward = clamp((SMALL_ORE_SCALE - radial)
                                / Math.max(0.01D, SMALL_ORE_SCALE - 1.0D), 0.0D, 1.0D);
                        double smallChance = 0.012D + inward * 0.060D;
                        long smallSeed = mix64(cellSeed ^ 0x736D616C6C5F6F72L);
                        if (unitDouble(smallSeed) < smallChance) {
                            int sx = centerX + boundedSigned(mix64(smallSeed ^ 0x11L), patchRadiusXZ);
                            int sy = centerY + boundedSigned(mix64(smallSeed ^ 0x22L), patchRadiusY);
                            int sz = centerZ + boundedSigned(mix64(smallSeed ^ 0x33L), patchRadiusXZ);
                            if (sx >= minX && sx <= maxX && sz >= minZ && sz <= maxZ
                                    && sy >= minimumY && sy <= maximumY) {
                                pos.set(sx, sy, sz);
                                BlockState state = level.getBlockState(pos);
                                if (deposit.replaceableHostBlocks().contains(state.getBlock())) {
                                    Block smallOre = selectOreBlock(
                                            deposit, geologyHost.host().id(), true, smallSeed);
                                    if (smallOre != null) {
                                        level.setBlock(pos, smallOre.defaultBlockState(), 2);
                                        placed = true;
                                    }
                                }
                            }
                        }
                    }

                    double fillChance = 0.72D + intensity * 0.22D;
                    for (int x = Math.max(minX, centerX - patchRadiusXZ);
                         x <= Math.min(maxX, centerX + patchRadiusXZ);
                         x++) {
                        for (int z = Math.max(minZ, centerZ - patchRadiusXZ);
                             z <= Math.min(maxZ, centerZ + patchRadiusXZ);
                             z++) {
                            for (int y = Math.max(minimumY, centerY - patchRadiusY);
                                 y <= Math.min(maximumY, centerY + patchRadiusY);
                                 y++) {
                                long blockSeed = mix64(cellSeed ^ BlockPos.asLong(x, y, z));
                                if (unitDouble(blockSeed) > fillChance) continue;

                                pos.set(x, y, z);
                                BlockState state = level.getBlockState(pos);
                                if (!deposit.replaceableHostBlocks().contains(state.getBlock())) continue;
                                if (state.is(geologyHost.hostBlock())) continue;

                                level.setBlock(pos, geologyHost.hostBlock().defaultBlockState(), 2);
                                placed = true;
                            }
                        }
                    }
                }
            }
        }
        return placed;
    }

    private static HostTarget selectHostTarget(
            HostPalette palette,
            long seed,
            int x,
            int y,
            int z,
            int cellSize
    ) {
        if (palette.isEmpty()) return null;
        if (palette.targets().size() == 1) return palette.targets().get(0);

        int safeCellSize = Math.max(1, cellSize);
        int cellX = Math.floorDiv(x, safeCellSize);
        int cellY = Math.floorDiv(y, safeCellSize);
        int cellZ = Math.floorDiv(z, safeCellSize);
        long cell = BlockPos.asLong(cellX, cellY, cellZ);
        int choice = bounded(mix64(seed ^ cell ^ 0x686F73745F706174L), palette.totalWeight());
        for (HostTarget target : palette.targets()) {
            choice -= target.affinity();
            if (choice < 0) return target;
        }
        return palette.targets().get(palette.targets().size() - 1);
    }

    private static Block selectOreBlock(
            RuntimeDeposit deposit,
            String hostId,
            boolean small,
            long seed
    ) {
        OrePalette palette = deposit.oreByHost().get(hostId);
        if (palette == null) return null;
        WeightedBlockPool pool = small ? palette.small() : palette.normal();
        if (pool.isEmpty()) return null;

        int choice = bounded(mix64(seed ^ (small ? 0x534D414C4C4F5245L : 0x4F52455F4D495845L)), pool.totalWeight());
        for (WeightedBlock weighted : pool.blocks()) {
            choice -= weighted.weight();
            if (choice < 0) return weighted.block();
        }
        return pool.blocks().get(pool.blocks().size() - 1).block();
    }

    private static boolean matchesBiome(WorldGenLevel level, BlockPos pos, BiomeFilter filter) {
        if (filter.isEmpty()) return true;
        Holder<Biome> biome = level.getBiome(pos);
        for (TagKey<Biome> tag : filter.tags()) {
            if (biome.is(tag)) return true;
        }
        for (ResourceKey<Biome> key : filter.keys()) {
            if (biome.is(key)) return true;
        }
        return false;
    }

    private static BiomeFilter biomeFilter(Set<String> criteria) {
        if (criteria == null || criteria.isEmpty()) return BiomeFilter.EMPTY;
        List<TagKey<Biome>> tags = new ArrayList<>();
        List<ResourceKey<Biome>> keys = new ArrayList<>();
        for (String criterion : criteria) {
            if (criterion == null || criterion.isBlank()) continue;
            boolean tag = criterion.charAt(0) == '#';
            ResourceLocation id = ResourceLocation.tryParse(tag ? criterion.substring(1) : criterion);
            if (id == null) continue;
            if (tag) tags.add(TagKey.create(Registries.BIOME, id));
            else keys.add(ResourceKey.create(Registries.BIOME, id));
        }
        return new BiomeFilter(List.copyOf(tags), List.copyOf(keys));
    }

    private static RuntimeDeposit selectDeposit(List<RuntimeDeposit> deposits, long seed) {
        double total = 0.0D;
        for (RuntimeDeposit deposit : deposits) total += deposit.definition().rarity();
        if (!(total > 0.0D)) return null;

        double choice = unitDouble(mix64(seed ^ 0x4E554C4C44504F53L)) * total;
        for (RuntimeDeposit deposit : deposits) {
            choice -= deposit.definition().rarity();
            if (choice <= 0.0D) return deposit;
        }
        return deposits.get(deposits.size() - 1);
    }

    private static RegionPlan regionPlan(
            long worldSeed,
            String dimension,
            int regionX,
            int regionZ,
            List<RuntimeDeposit> eligible
    ) {
        RegionPlanKey key = new RegionPlanKey(worldSeed, dimension, regionX, regionZ);
        return REGION_PLAN_CACHE.computeIfAbsent(key,
                ignored -> buildRegionPlan(worldSeed, dimension, regionX, regionZ, eligible));
    }

    private static RegionPlan buildRegionPlan(
            long worldSeed,
            String dimension,
            int regionX,
            int regionZ,
            List<RuntimeDeposit> eligible
    ) {
        long seed = regionSeed(worldSeed, dimension, regionX, regionZ);
        RuntimeDeposit selected = selectDeposit(eligible, seed);
        if (selected == null) return null;

        // One deterministic owner/anchor chunk inside the 9x9 region. The deposit is centered in
        // that chunk, but its body and geology may extend freely across the region border.
        int anchorChunkX = regionX * REGION_CHUNKS
                + bounded(mix64(seed ^ 0x414E43484F525F58L), REGION_CHUNKS);
        int anchorChunkZ = regionZ * REGION_CHUNKS
                + bounded(mix64(seed ^ 0x414E43484F525F5AL), REGION_CHUNKS);
        Body body = body(selected.definition(), seed, anchorChunkX, anchorChunkZ);
        return new RegionPlan(regionX, regionZ, anchorChunkX, anchorChunkZ, seed, selected, body);
    }

    private static Body body(DepositDefinition deposit, long seed, int anchorChunkX, int anchorChunkZ) {
        long a = mix64(seed ^ 0x63A9F84D5B17E2C1L);
        long b = mix64(seed ^ 0x9E3779B97F4A7C15L);
        int margin = 3;
        int centerX = anchorChunkX * 16 + margin + bounded(a, 16 - margin * 2);
        int centerZ = anchorChunkZ * 16 + margin + bounded(b, 16 - margin * 2);

        int span = Math.max(0, deposit.maximumSize() - deposit.minimumSize());
        int nominal = deposit.minimumSize() + bounded(mix64(a ^ b), span + 1);
        double radius = clamp(nominal * 0.50D, 8.0D, MAX_BODY_RADIUS);
        int centerY = deposit.conditions().preferredY();
        int verticalSpread = Math.max(3, deposit.conditions().verticalSpread());
        int yJitter = boundedSigned(mix64(seed ^ 0xD1B54A32D192ED03L), Math.max(1, verticalSpread / 2));
        centerY = Math.max(deposit.conditions().minimumY(),
                Math.min(deposit.conditions().maximumY(), centerY + yJitter));

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
        ChemistryBootstrap.RuntimeGeology analysis = ChemistryBootstrap.runtimeGeology();
        Map<String, OreMineral> minerals = new LinkedHashMap<>();
        for (OreMineral mineral : analysis.geology().minerals()) minerals.put(mineral.id(), mineral);
        Map<String, IndustrialMaterial> materials = analysis.materials();

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
            Map<String, OreMineral> mineralDefinitions,
            Map<String, IndustrialMaterial> materials
    ) {
        IndustrialMaterial primaryMaterial = firstDedicatedMaterial(
                deposit.primaryMinerals(), mineralDefinitions, materials);
        if (primaryMaterial == null) return null;

        Set<String> plannedHostIds = deposit.conditions().hostRockTags();
        HostPalette bodyHosts = hostPalette(
                MaterialOreHost.compatibleHosts(primaryMaterial), primaryMaterial, plannedHostIds);
        if (bodyHosts.isEmpty()) return null;

        // Regional signal hosts deliberately ignore the body's hostRockTags. They are a wider
        // prospecting signature selected from the best NON-base StoneMaterials for this ore.
        HostPalette geologyHosts = hostPalette(
                MaterialOreHost.geologyHosts(primaryMaterial), primaryMaterial, Set.of());

        MaterialOrePolicy.DimensionBand dimension = bodyHosts.targets().get(0).host().dimension();
        Set<Block> replaceable = new LinkedHashSet<>();
        for (MaterialOreHost host : MaterialOreHost.forDimension(dimension)) {
            Block hostBlock = BuiltInRegistries.BLOCK.get(host.hostBlock());
            if (hostBlock != null) replaceable.add(hostBlock);
        }

        List<RuntimeMineral> runtimeMinerals = new ArrayList<>();
        addRuntimeMinerals(runtimeMinerals, deposit.primaryMinerals(), mineralDefinitions, materials, dimension);
        addRuntimeMinerals(runtimeMinerals, deposit.secondaryMinerals(), mineralDefinitions, materials, dimension);
        if (runtimeMinerals.isEmpty()) return null;

        return new RuntimeDeposit(
                deposit,
                primaryMaterial.id(),
                bodyHosts,
                geologyHosts,
                List.copyOf(runtimeMinerals),
                buildOrePalettes(runtimeMinerals),
                Set.copyOf(replaceable),
                biomeFilter(deposit.conditions().biomeTags())
        );
    }

    private static IndustrialMaterial firstDedicatedMaterial(
            List<DepositDefinition.WeightedMineral> weightedMinerals,
            Map<String, OreMineral> mineralDefinitions,
            Map<String, IndustrialMaterial> materials
    ) {
        for (DepositDefinition.WeightedMineral weighted : weightedMinerals) {
            OreMineral mineral = mineralDefinitions.get(weighted.mineralId());
            if (mineral == null) continue;
            IndustrialMaterial candidate = materials.get(mineral.materialId());
            if (candidate != null && MaterialOreHost.hasNaturalOre(candidate)) return candidate;
        }
        return null;
    }

    private static HostPalette hostPalette(
            List<MaterialOreHost> hosts,
            IndustrialMaterial primaryMaterial,
            Set<String> plannedHostIds
    ) {
        List<HostTarget> result = new ArrayList<>();
        for (MaterialOreHost host : hosts) {
            ResourceLocation hostId = host.hostBlock();
            if (!plannedHostIds.isEmpty() && !plannedHostIds.contains(hostId.toString())) continue;
            Block hostBlock = BuiltInRegistries.BLOCK.get(hostId);
            if (hostBlock == null) continue;
            result.add(new HostTarget(host, hostBlock, Math.max(1, host.affinity(primaryMaterial))));
        }
        result.sort(Comparator.comparing((HostTarget target) -> target.host().id()));
        return HostPalette.of(result);
    }

    private static Map<String, OrePalette> buildOrePalettes(List<RuntimeMineral> minerals) {
        Map<String, List<WeightedBlock>> normal = new LinkedHashMap<>();
        Map<String, List<WeightedBlock>> small = new LinkedHashMap<>();
        for (RuntimeMineral mineral : minerals) {
            for (Map.Entry<String, Block> entry : mineral.normalBlocks().entrySet()) {
                normal.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>())
                        .add(new WeightedBlock(entry.getValue(), Math.max(1, mineral.weight())));
            }
            for (Map.Entry<String, Block> entry : mineral.smallBlocks().entrySet()) {
                small.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>())
                        .add(new WeightedBlock(entry.getValue(), Math.max(1, mineral.weight())));
            }
        }

        Set<String> hostIds = new LinkedHashSet<>();
        hostIds.addAll(normal.keySet());
        hostIds.addAll(small.keySet());
        Map<String, OrePalette> result = new LinkedHashMap<>();
        for (String hostId : hostIds) {
            result.put(hostId, new OrePalette(
                    WeightedBlockPool.of(normal.getOrDefault(hostId, List.of())),
                    WeightedBlockPool.of(small.getOrDefault(hostId, List.of()))));
        }
        return Map.copyOf(result);
    }

    private static void addRuntimeMinerals(
            List<RuntimeMineral> output,
            List<DepositDefinition.WeightedMineral> weightedMinerals,
            Map<String, OreMineral> mineralDefinitions,
            Map<String, IndustrialMaterial> materials,
            MaterialOrePolicy.DimensionBand dimension
    ) {
        for (DepositDefinition.WeightedMineral weighted : weightedMinerals) {
            OreMineral mineral = mineralDefinitions.get(weighted.mineralId());
            if (mineral == null) continue;
            IndustrialMaterial material = materials.get(mineral.materialId());
            if (material == null || !MaterialOreHost.hasNaturalOre(material)) continue;
            if (MaterialTierResolver.geologyDimension(material) != dimension) continue;

            Map<String, Block> normalBlocks = new LinkedHashMap<>();
            Map<String, Block> smallBlocks = new LinkedHashMap<>();
            for (MaterialOreHost host : MaterialOreHost.compatibleHosts(material)) {
                Block normal = oreBlock(material, host, false);
                Block small = oreBlock(material, host, true);
                if (normal != null) normalBlocks.put(host.id(), normal);
                if (small != null) smallBlocks.put(host.id(), small);
            }
            if (normalBlocks.isEmpty()) continue;
            output.add(new RuntimeMineral(
                    material.id(), weighted.weight(), Map.copyOf(normalBlocks), Map.copyOf(smallBlocks)));
        }
    }

    private static Block oreBlock(IndustrialMaterial material, MaterialOreHost host, boolean small) {
        ResourceLocation existing = host.existingOreBlock(material, small).orElse(null);
        if (existing != null) return BuiltInRegistries.BLOCK.get(existing);

        DeferredHolder<Block, ? extends Block> generated = BlockRegistry.getMaterialOreHostBlock(material, host, small);
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

    private static double square(double value) {
        return value * value;
    }

    private record ChunkCandidate(RegionPlan region, double distanceSquared) {
    }

    private record RegionPlanKey(long worldSeed, String dimension, int regionX, int regionZ) {
    }

    private record RegionPlan(
            int regionX,
            int regionZ,
            int anchorChunkX,
            int anchorChunkZ,
            long seed,
            RuntimeDeposit deposit,
            Body body
    ) {
    }

    private record RuntimePlan(Map<String, List<RuntimeDeposit>> byDimension) {
    }

    private record RuntimeDeposit(
            DepositDefinition definition,
            String primaryMaterialId,
            HostPalette bodyHosts,
            HostPalette geologyHosts,
            List<RuntimeMineral> minerals,
            Map<String, OrePalette> oreByHost,
            Set<Block> replaceableHostBlocks,
            BiomeFilter biomeFilter
    ) {
        private boolean containsMaterial(String materialId) {
            for (RuntimeMineral mineral : minerals) {
                if (mineral.materialId().equals(materialId)) return true;
            }
            return false;
        }
    }

    private record HostTarget(MaterialOreHost host, Block hostBlock, int affinity) {
    }

    private record HostPalette(List<HostTarget> targets, int totalWeight) {
        static HostPalette of(List<HostTarget> targets) {
            List<HostTarget> immutable = List.copyOf(targets);
            int total = 0;
            for (HostTarget target : immutable) total += Math.max(1, target.affinity());
            return new HostPalette(immutable, total);
        }

        boolean isEmpty() {
            return targets.isEmpty() || totalWeight <= 0;
        }
    }

    private record RuntimeMineral(
            String materialId,
            int weight,
            Map<String, Block> normalBlocks,
            Map<String, Block> smallBlocks
    ) {
    }

    private record WeightedBlock(Block block, int weight) {
    }

    private record WeightedBlockPool(List<WeightedBlock> blocks, int totalWeight) {
        static WeightedBlockPool of(List<WeightedBlock> blocks) {
            List<WeightedBlock> immutable = List.copyOf(blocks);
            int total = 0;
            for (WeightedBlock block : immutable) total += Math.max(1, block.weight());
            return new WeightedBlockPool(immutable, total);
        }

        boolean isEmpty() {
            return blocks.isEmpty() || totalWeight <= 0;
        }
    }

    private record OrePalette(WeightedBlockPool normal, WeightedBlockPool small) {
    }

    private record BiomeFilter(List<TagKey<Biome>> tags, List<ResourceKey<Biome>> keys) {
        private static final BiomeFilter EMPTY = new BiomeFilter(List.of(), List.of());

        boolean isEmpty() {
            return tags.isEmpty() && keys.isEmpty();
        }
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
                case BANDED, LAYER, SEDIMENTARY, EVAPORITE ->
                        ry = Math.max(2.0D, Math.min(7.0D, verticalSpread * 0.35D));
                case PIPE -> {
                    rx = Math.max(4.0D, radius * 0.30D);
                    rz = rx;
                    ry = Math.max(8.0D, verticalSpread * 1.4D);
                }
                case PLACER, OCEAN_FLOOR_MASS ->
                        ry = Math.max(2.0D, Math.min(5.0D, verticalSpread * 0.25D));
                case PORPHYRY, DISSEMINATED, MAGMATIC, METAMORPHIC, LENS -> {
                    // Broad ellipsoids use the defaults.
                }
                case FLUID_RESERVOIR, GAS_RESERVOIR -> {
                    rx = radius * 0.75D;
                    rz = radius * 0.75D;
                }
            }
            return new Body(geometry, centerX, centerY, centerZ, rx, ry, rz,
                    Math.sin(angle), Math.cos(angle));
        }

        double radiusSquaredLimit() {
            return switch (geometry) {
                case BANDED, LAYER, SEDIMENTARY, EVAPORITE -> 1.08D;
                default -> 1.0D;
            };
        }

        /** Radius before geometry-specific axis compression in {@link #forGeometry}. */
        double nominalRadius() {
            return switch (geometry) {
                case PIPE -> radiusX / 0.30D;
                case FLUID_RESERVOIR, GAS_RESERVOIR -> radiusX / 0.75D;
                default -> Math.max(radiusX, radiusZ);
            };
        }

        double estimatedVolume() {
            double limitScale = Math.pow(radiusSquaredLimit(), 1.5D);
            return (4.0D / 3.0D) * Math.PI * radiusX * radiusY * radiusZ * limitScale;
        }

        double radiusSquaredAt(int x, int y, int z) {
            return radiusSquaredAt(x, y, z, radiusX, radiusY, radiusZ);
        }

        double radiusSquaredAt(
                int x,
                int y,
                int z,
                double targetRadiusX,
                double targetRadiusY,
                double targetRadiusZ
        ) {
            double dx = x - centerX;
            double dz = z - centerZ;
            double rotatedX = dx * cos + dz * sin;
            double rotatedZ = -dx * sin + dz * cos;
            double nx = rotatedX / targetRadiusX;
            double ny = (y - centerY) / targetRadiusY;
            double nz = rotatedZ / targetRadiusZ;
            return nx * nx + ny * ny + nz * nz;
        }

        int scaledMinY(double scale, double warp) {
            return (int) Math.floor(centerY - radiusY * scale * (1.0D + warp));
        }

        int scaledMaxY(double scale, double warp) {
            return (int) Math.ceil(centerY + radiusY * scale * (1.0D + warp));
        }

        boolean couldReachChunk(int chunkX, int chunkZ, double scale, double warp) {
            int minX = chunkX * 16;
            int minZ = chunkZ * 16;
            int maxX = minX + 15;
            int maxZ = minZ + 15;
            double extent = Math.max(radiusX, radiusZ) * scale * (1.0D + warp);
            return !(maxX < centerX - extent || minX > centerX + extent
                    || maxZ < centerZ - extent || minZ > centerZ + extent);
        }

        boolean couldReachChunkWithHalo(int chunkX, int chunkZ, int halo) {
            int minX = chunkX * 16;
            int minZ = chunkZ * 16;
            int maxX = minX + 15;
            int maxZ = minZ + 15;
            double extent = Math.max(radiusX, radiusZ) + Math.max(0, halo) + 1.0D;
            return !(maxX < centerX - extent || minX > centerX + extent
                    || maxZ < centerZ - extent || minZ > centerZ + extent);
        }

        double horizontalRadiusSquared(int x, int z) {
            double dx = x - centerX;
            double dz = z - centerZ;
            double rotatedX = dx * cos + dz * sin;
            double rotatedZ = -dx * sin + dz * cos;
            double nx = rotatedX / radiusX;
            double nz = rotatedZ / radiusZ;
            return nx * nx + nz * nz;
        }

        double geologyOuterScale(int x, int z, long seed) {
            double noise = smoothNoise2D(seed ^ 0x47454F4C4F47594CL, x, z);
            return Math.max(1.45D, REGIONAL_GEOLOGY_SCALE * (1.0D + noise * GEOLOGY_WARP));
        }

        double geologyIntensity(double radial, double outerScale) {
            if (radial <= 1.0D) return 1.0D;
            if (radial >= outerScale) return 0.0D;
            return clamp((outerScale - radial) / (outerScale - 1.0D), 0.0D, 1.0D);
        }

        boolean oreBand(double radial) {
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

        double localGrade(double radial, DepositDefinition deposit) {
            radial = Math.min(1.0D, radial);
            double grade = deposit.minimumGrade()
                    + (deposit.maximumGrade() - deposit.minimumGrade()) * (1.0D - radial);
            if (radial < 0.28D) {
                grade += (deposit.maximumGrade() - grade) * deposit.highGradeCoreChance();
            }
            return clamp(grade, 0.001D, 1.0D);
        }

        private static double smoothNoise2D(long seed, int x, int z) {
            int x0 = Math.floorDiv(x, GEOLOGY_NOISE_XZ);
            int z0 = Math.floorDiv(z, GEOLOGY_NOISE_XZ);
            double tx = fade(Math.floorMod(x, GEOLOGY_NOISE_XZ) / (double) GEOLOGY_NOISE_XZ);
            double tz = fade(Math.floorMod(z, GEOLOGY_NOISE_XZ) / (double) GEOLOGY_NOISE_XZ);

            double n00 = lattice(seed, x0, z0);
            double n10 = lattice(seed, x0 + 1, z0);
            double n01 = lattice(seed, x0, z0 + 1);
            double n11 = lattice(seed, x0 + 1, z0 + 1);
            return lerp(lerp(n00, n10, tx), lerp(n01, n11, tx), tz);
        }

        private static double lattice(long seed, int x, int z) {
            long mixed = mix64(seed
                    ^ ((long) x * 0x9E3779B97F4A7C15L)
                    ^ ((long) z * 0x165667B19E3779F9L));
            return unitDouble(mixed) * 2.0D - 1.0D;
        }

        private static double fade(double value) {
            return value * value * (3.0D - 2.0D * value);
        }

        private static double lerp(double a, double b, double t) {
            return a + (b - a) * t;
        }
    }
}
