package net.mads.industron.worldgen;

import com.mojang.serialization.Codec;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Final-pass replacement for vanilla clay worldgen.
 *
 * <p>Mojang still owns the geometry: river/lake disks, Lush Cave clay floors/pools and their
 * water/dripleaf placement are generated normally. At TOP_LAYER_MODIFICATION, after every vanilla
 * decoration step that can place clay, this feature changes only {@code minecraft:clay} states to
 * the clay selected by Industron's deterministic geology plan.</p>
 *
 * <p>Performance stays bounded: palette {@link LevelChunkSection#maybeHas} checks skip every section
 * that contains no clay, so we never scan the full vertical chunk just to discover that there was
 * no vanilla clay there.</p>
 */
public final class GeologyClayReplacementFeature extends Feature<NoneFeatureConfiguration> {
    public GeologyClayReplacementFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        int chunkX = Math.floorDiv(context.origin().getX(), 16);
        int chunkZ = Math.floorDiv(context.origin().getZ(), 16);
        ChunkAccess chunk = level.getChunk(chunkX, chunkZ);

        BlockPos sample = new BlockPos(chunkX * 16 + 8, 0, chunkZ * 16 + 8);
        IndustrialMaterial selected = GeologyDepositFeature.selectClayFor(level, sample);
        Block replacement = clayBlock(selected);
        if (replacement == null || replacement == Blocks.CLAY) {
            return false;
        }

        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean changed = false;

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section == null || section.hasOnlyAir() || !section.maybeHas(state -> state.is(Blocks.CLAY))) {
                continue;
            }

            int baseY = chunk.getSectionYFromSectionIndex(sectionIndex) << 4;
            for (int localY = 0; localY < 16; localY++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    for (int localX = 0; localX < 16; localX++) {
                        BlockState state = section.getBlockState(localX, localY, localZ);
                        if (!state.is(Blocks.CLAY)) continue;

                        pos.set(chunkX * 16 + localX, baseY + localY, chunkZ * 16 + localZ);
                        level.setBlock(pos, replacement.defaultBlockState(), 2);
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    private static Block clayBlock(IndustrialMaterial material) {
        if (material == null || !material.has(MaterialPart.CLAY_BLOCK)) return null;
        if (material.hasExistingPart(MaterialPart.CLAY_BLOCK)) {
            ResourceLocation id = material.existingPart(MaterialPart.CLAY_BLOCK);
            return id == null ? null : BuiltInRegistries.BLOCK.get(id);
        }
        DeferredHolder<Block, ? extends Block> holder = BlockRegistry.getMaterialBlock(material, MaterialPart.CLAY_BLOCK);
        return holder == null ? null : holder.get();
    }
}
