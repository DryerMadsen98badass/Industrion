package net.mads.industron.worldgen;

import com.mojang.serialization.Codec;
import net.mads.industron.material.MaterialOreHost;
import net.mads.industron.material.MaterialOrePolicy;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Places collectible loose Pebbles as local geology indicators.
 *
 * <p>The dimension base rock gets one strong placement attempt per generated chunk (Stone in the
 * Overworld). Non-base Pebbles are selected only from the same cached geology field that actually
 * generates the corresponding StoneMaterial. Surface evidence then biases placement toward places
 * loose fragments make sense: matching gravel/outcrops and the foot of steep terrain.</p>
 */
public final class PebbleSurfaceFeature extends Feature<NoneFeatureConfiguration> {
    private static final int BASE_PLACEMENT_ATTEMPTS = 18;
    private static final int SPECIAL_PLACEMENT_ATTEMPTS = 12;
    private static final int SPECIAL_PLACEMENTS_PER_CHUNK = 2;
    private static final int EVIDENCE_RADIUS = 5;
    private static final int EVIDENCE_DEPTH = 4;
    private static final int SLOPE_RADIUS = 4;
    private static final int CLIFF_RISE = 5;

    public PebbleSurfaceFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        MaterialOrePolicy.DimensionBand dimension = dimension(level);
        if (dimension == null) return false;

        int chunkX = Math.floorDiv(context.origin().getX(), 16);
        int chunkZ = Math.floorDiv(context.origin().getZ(), 16);
        int minX = chunkX * 16;
        int minZ = chunkZ * 16;
        RandomSource random = context.random();
        boolean placed = false;

        StoneMaterial baseRock = defaultBaseRock(dimension);
        if (baseRock != null) {
            placed |= placeBaseRockPebble(level, random, minX, minZ, dimension, baseRock);
        }

        for (int i = 0; i < SPECIAL_PLACEMENTS_PER_CHUNK; i++) {
            placed |= placeGeologyPebble(level, random, minX, minZ, dimension);
        }
        return placed;
    }

    private static boolean placeBaseRockPebble(
            WorldGenLevel level,
            RandomSource random,
            int minX,
            int minZ,
            MaterialOrePolicy.DimensionBand dimension,
            StoneMaterial baseRock
    ) {
        for (int attempt = 0; attempt < BASE_PLACEMENT_ATTEMPTS; attempt++) {
            BlockPos pos = randomSurface(level, random, minX, minZ, dimension);
            if (pos == null) continue;
            if (placePebble(level, pos, baseRock, random)) return true;
        }
        return false;
    }

    private static boolean placeGeologyPebble(
            WorldGenLevel level,
            RandomSource random,
            int minX,
            int minZ,
            MaterialOrePolicy.DimensionBand dimension
    ) {
        for (int attempt = 0; attempt < SPECIAL_PLACEMENT_ATTEMPTS; attempt++) {
            BlockPos pos = randomSurface(level, random, minX, minZ, dimension);
            if (pos == null) continue;

            List<GeologyDepositFeature.SurfaceStoneCandidate> geology =
                    GeologyDepositFeature.surfaceStoneCandidates(level, pos);
            if (geology.isEmpty()) continue;

            List<ScoredStone> scored = new ArrayList<>();
            boolean slope = atFootOfSlope(level, pos, dimension);
            for (GeologyDepositFeature.SurfaceStoneCandidate candidate : geology) {
                StoneMaterial stone = candidate.stone();
                if (stone.dimension() != dimension || stone.isBaseRock()
                        || !stone.generatedForms().contains(MaterialPart.PEBBLE)) {
                    continue;
                }
                int evidence = localEvidence(level, pos, stone, dimension);
                int geologyScore = Math.min(600, Math.max(1, candidate.weight()));
                int score = geologyScore + evidence + (slope ? 260 : 0);
                if (score > 0) scored.add(new ScoredStone(stone, score, evidence, slope));
            }
            if (scored.isEmpty()) continue;

            ScoredStone chosen = weighted(random, scored);
            if (chosen == null) continue;

            // A geology field is enough for a sparse indicator. Matching gravel/exposed stone and
            // a downhill collection point make loose fragments substantially more likely.
            float chance = 0.12F;
            chance += Math.min(0.48F, chosen.evidence() / 1100.0F);
            if (chosen.slope()) chance += 0.18F;
            chance = Math.min(0.88F, chance);
            if (random.nextFloat() > chance) continue;

            if (placePebble(level, pos, chosen.stone(), random)) return true;
        }
        return false;
    }

    private static ScoredStone weighted(RandomSource random, List<ScoredStone> values) {
        int total = 0;
        for (ScoredStone value : values) total += Math.max(1, value.score());
        if (total <= 0) return values.stream().max(Comparator.comparingInt(ScoredStone::score)).orElse(null);
        int roll = random.nextInt(total);
        for (ScoredStone value : values) {
            roll -= Math.max(1, value.score());
            if (roll < 0) return value;
        }
        return values.getLast();
    }

    private static int localEvidence(
            WorldGenLevel level,
            BlockPos pos,
            StoneMaterial stone,
            MaterialOrePolicy.DimensionBand dimension
    ) {
        int score = 0;
        BlockPos below = pos.below();
        Block support = level.getBlockState(below).getBlock();
        if (representsPart(stone, MaterialPart.GRAVEL, support)) score += 520;
        if (representsStone(stone, support)) score += 360;

        // Sample nearby columns rather than scanning a full volume. This catches exposed host rock,
        // material gravel and talus-like surfaces without making a top-layer feature expensive.
        for (int dx = -EVIDENCE_RADIUS; dx <= EVIDENCE_RADIUS; dx += 2) {
            for (int dz = -EVIDENCE_RADIUS; dz <= EVIDENCE_RADIUS; dz += 2) {
                if (dx == 0 && dz == 0) continue;
                BlockPos surface = surfacePosition(level, new BlockPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz), dimension);
                if (surface == null) continue;
                BlockPos.MutableBlockPos cursor = surface.below().mutable();
                for (int depth = 0; depth < EVIDENCE_DEPTH; depth++) {
                    Block block = level.getBlockState(cursor).getBlock();
                    if (representsPart(stone, MaterialPart.GRAVEL, block)) {
                        score += 48;
                        break;
                    }
                    if (representsStone(stone, block)) {
                        score += 30;
                        break;
                    }
                    cursor.move(Direction.DOWN);
                }
            }
        }
        return Math.min(score, 900);
    }

    private static boolean atFootOfSlope(
            WorldGenLevel level,
            BlockPos pos,
            MaterialOrePolicy.DimensionBand dimension
    ) {
        if (dimension == MaterialOrePolicy.DimensionBand.NETHER) return false;
        int own = pos.getY();
        for (int dx : new int[]{-SLOPE_RADIUS, 0, SLOPE_RADIUS}) {
            for (int dz : new int[]{-SLOPE_RADIUS, 0, SLOPE_RADIUS}) {
                if (dx == 0 && dz == 0) continue;
                int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX() + dx, pos.getZ() + dz);
                if (height - own >= CLIFF_RISE) return true;
            }
        }
        return false;
    }

    private static BlockPos randomSurface(
            WorldGenLevel level,
            RandomSource random,
            int minX,
            int minZ,
            MaterialOrePolicy.DimensionBand dimension
    ) {
        int x = minX + random.nextInt(16);
        int z = minZ + random.nextInt(16);
        return surfacePosition(level, new BlockPos(x, 0, z), dimension);
    }

    private static BlockPos surfacePosition(
            WorldGenLevel level,
            BlockPos origin,
            MaterialOrePolicy.DimensionBand dimension
    ) {
        int x = origin.getX();
        int z = origin.getZ();
        if (dimension != MaterialOrePolicy.DimensionBand.NETHER) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            return validSurface(level, pos) ? pos : null;
        }

        // WORLD_SURFACE_WG points at the Nether roof. Search a reachable cavern floor instead.
        int top = Math.min(level.getMaxBuildHeight() - 16, 120);
        int bottom = Math.max(level.getMinBuildHeight() + 2, 8);
        for (int y = top; y >= bottom; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (validSurface(level, pos) && level.getBlockState(pos.above()).isAir()) return pos;
        }
        return null;
    }

    private static boolean validSurface(WorldGenLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) return false;
        BlockPos below = pos.below();
        BlockState support = level.getBlockState(below);
        return !support.isAir()
                && support.getFluidState().isEmpty()
                && support.isFaceSturdy(level, below, Direction.UP);
    }

    private static boolean placePebble(WorldGenLevel level, BlockPos pos, StoneMaterial stone, RandomSource random) {
        if (!validSurface(level, pos)) return false;
        var holder = BlockRegistry.getPebbleWorldgenBlock(stone);
        if (holder == null) return false;
        BlockState pebble = holder.get().defaultBlockState();
        if (!pebble.canSurvive(level, pos)) return false;
        return level.setBlock(pos, pebble, 2);
    }

    private static boolean representsStone(StoneMaterial stone, Block block) {
        try {
            ResourceLocation natural = new MaterialOreHost(stone).hostBlock();
            if (BuiltInRegistries.BLOCK.getOptional(natural).orElse(null) == block) return true;
        } catch (RuntimeException ignored) {
        }
        return representsPart(stone, MaterialPart.STONE, block)
                || representsPart(stone, MaterialPart.COBBLED_STONE, block);
    }

    private static boolean representsPart(StoneMaterial stone, MaterialPart part, Block block) {
        if (stone.isWithout(part)) return false;
        if (stone.hasExistingPart(part)) {
            return BuiltInRegistries.BLOCK.getOptional(stone.existingPart(part)).orElse(null) == block;
        }
        var holder = BlockRegistry.getStructureMaterialBlock(part.registryName(stone));
        return holder != null && holder.get() == block;
    }

    private static StoneMaterial defaultBaseRock(MaterialOrePolicy.DimensionBand dimension) {
        return StoneMaterials.ALL.stream()
                .filter(stone -> stone.dimension() == dimension)
                .filter(StoneMaterial::isBaseRock)
                .filter(stone -> stone.generatedForms().contains(MaterialPart.PEBBLE))
                .findFirst()
                .orElse(null);
    }

    private static MaterialOrePolicy.DimensionBand dimension(WorldGenLevel level) {
        String id = level.getLevel().dimension().location().toString();
        return switch (id) {
            case "minecraft:overworld" -> MaterialOrePolicy.DimensionBand.OVERWORLD;
            case "minecraft:the_nether" -> MaterialOrePolicy.DimensionBand.NETHER;
            case "minecraft:the_end" -> MaterialOrePolicy.DimensionBand.END;
            default -> null;
        };
    }

    private record ScoredStone(StoneMaterial stone, int score, int evidence, boolean slope) {
    }
}
