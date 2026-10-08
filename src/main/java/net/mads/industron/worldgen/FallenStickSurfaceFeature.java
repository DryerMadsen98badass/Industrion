package net.mads.industron.worldgen;

import com.mojang.serialization.Codec;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Places small collectible sticks only around the matching local tree/stem material. */
public final class FallenStickSurfaceFeature extends Feature<NoneFeatureConfiguration> {
    private static final int TREE_RADIUS = 6;
    private static final int VERTICAL_BELOW = 2;
    private static final int VERTICAL_ABOVE = 13;

    public FallenStickSurfaceFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = surfacePosition(level, context.origin());
        if (pos == null || !validSurface(level, pos)) return false;

        WoodMaterial wood = nearestTreeMaterial(level, pos);
        if (wood == null) return false;
        return placeFallenStick(level, pos, wood, context.random());
    }

    private static WoodMaterial nearestTreeMaterial(WorldGenLevel level, BlockPos pos) {
        WoodMaterial best = null;
        int bestDistance = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -TREE_RADIUS; dx <= TREE_RADIUS; dx++) {
            for (int dz = -TREE_RADIUS; dz <= TREE_RADIUS; dz++) {
                int horizontal = dx * dx + dz * dz;
                if (horizontal > TREE_RADIUS * TREE_RADIUS) continue;
                for (int dy = -VERTICAL_BELOW; dy <= VERTICAL_ABOVE; dy++) {
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    Block block = level.getBlockState(cursor).getBlock();
                    WoodMaterial wood = materialForTreeBlock(block);
                    if (wood == null) continue;
                    int distance = horizontal + dy * dy;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = wood;
                    }
                }
            }
        }
        return best;
    }

    /** Resolves the WoodMaterial represented by a natural tree/stem block. */
    public static WoodMaterial materialForTreeBlock(Block block) {
        for (WoodMaterial wood : WoodMaterials.ALL) {
            if (matchesPart(wood, MaterialPart.LOG, block)
                    || matchesPart(wood, MaterialPart.WOOD, block)
                    || matchesPart(wood, MaterialPart.STRIPPED_LOG, block)
                    || matchesPart(wood, MaterialPart.STRIPPED_WOOD, block)) {
                return wood;
            }
            // Natural bamboo uses the plant block rather than bamboo_block (the LOG mapping).
            if (wood == WoodMaterials.BAMBOO) {
                Block bamboo = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.withDefaultNamespace("bamboo")).orElse(null);
                if (bamboo == block) return wood;
            }
        }
        return null;
    }

    /**
     * Resolves the material from the block that is about to grow into a tree feature.
     * Reading the sapling here is more reliable than guessing the species from a nearby
     * root log after the feature has already replaced the sapling.
     */
    public static WoodMaterial materialForGrowthSourceBlock(Block block) {
        for (WoodMaterial wood : WoodMaterials.ALL) {
            if (matchesPart(wood, MaterialPart.SAPLING, block)) return wood;
        }

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id.equals(ResourceLocation.withDefaultNamespace("crimson_fungus"))) return WoodMaterials.CRIMSON;
        if (id.equals(ResourceLocation.withDefaultNamespace("warped_fungus"))) return WoodMaterials.WARPED;

        return materialForTreeBlock(block);
    }

    /** Places the already-registered world-only fallen-stick block for this wood material. */
    public static boolean placeFallenStick(LevelAccessor level, BlockPos pos, WoodMaterial wood, RandomSource random) {
        if (wood == null || !validSurface(level, pos)) return false;
        var holder = BlockRegistry.getFallenStickBlock(wood);
        if (holder == null) return false;

        BlockState state = holder.get().randomState(random);
        if (!state.canSurvive(level, pos)) return false;
        return level.setBlock(pos, state, 2);
    }

    private static boolean matchesPart(WoodMaterial wood, MaterialPart part, Block block) {
        if (wood.hasExistingPart(part)) {
            return BuiltInRegistries.BLOCK.getOptional(wood.existingPart(part)).orElse(null) == block;
        }
        var holder = BlockRegistry.getStructureMaterialBlock(part.registryName(wood));
        return holder != null && holder.get() == block;
    }

    private static BlockPos surfacePosition(WorldGenLevel level, BlockPos origin) {
        String dimension = level.getLevel().dimension().location().toString();
        int x = origin.getX();
        int z = origin.getZ();
        if (!dimension.equals("minecraft:the_nether")) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            return validSurface(level, pos) ? pos : null;
        }

        int top = Math.min(level.getMaxBuildHeight() - 16, 120);
        int bottom = Math.max(level.getMinBuildHeight() + 2, 8);
        for (int y = top; y >= bottom; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (validSurface(level, pos) && level.getBlockState(pos.above()).isAir()) return pos;
        }
        return null;
    }

    private static boolean validSurface(LevelAccessor level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) return false;
        BlockPos below = pos.below();
        BlockState support = level.getBlockState(below);
        return !support.isAir()
                && support.getFluidState().isEmpty()
                && support.isFaceSturdy(level, below, Direction.UP);
    }
}
