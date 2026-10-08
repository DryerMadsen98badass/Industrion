package net.mads.industron.machine.foundry;

import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialPartBlock;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.mads.industron.runtime.BoundedIdentityCache;

import java.util.Optional;

/** Resolves any clay BRICKS block to its material without a hard-coded Foundry brick list. */
public final class FoundryBrickResolver {
    private FoundryBrickResolver() {
    }

    private static final BoundedIdentityCache<net.minecraft.world.level.block.Block, Optional<BrickInfo>> CACHE = new BoundedIdentityCache<>(4096);
    public static synchronized void clearCache() { CACHE.clear(); }
    public static synchronized Optional<BrickInfo> resolve(BlockState state) {
        if (state == null) return Optional.empty();
        return CACHE.computeIfAbsent(state.getBlock(), ignored -> compute(state));
    }
    private static Optional<BrickInfo> compute(BlockState state) {
        if (state == null) {
            return Optional.empty();
        }

        if (state.getBlock() instanceof MaterialPartBlock materialBlock
                && materialBlock.part() == MaterialPart.BRICKS
                && isFoundryBrickMaterial(materialBlock.material())) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (blockId != null) {
                return Optional.of(new BrickInfo(materialBlock.material(), blockId));
            }
        }

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null) {
            return Optional.empty();
        }

        // Existing part mappings (for example minecraft:bricks) do not implement MaterialPartBlock.
        // Resolve them from the material catalogue so future clay definitions work automatically.
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            if (!isFoundryBrickMaterial(material)) {
                continue;
            }
            ResourceLocation expected = material.hasExistingPart(MaterialPart.BRICKS)
                    ? material.existingPart(MaterialPart.BRICKS)
                    : ResourceLocation.fromNamespaceAndPath(
                            Industron.MOD_ID,
                            MaterialPart.BRICKS.registryName(material)
                    );
            if (blockId.equals(expected)) {
                return Optional.of(new BrickInfo(material, blockId));
            }
        }

        return Optional.empty();
    }

    private static boolean isFoundryBrickMaterial(IndustrialMaterial material) {
        return material != null
                && material.isClayMaterial()
                && material.has(MaterialPart.BRICKS);
    }

    public record BrickInfo(IndustrialMaterial material, ResourceLocation blockId) {
        public ResourceLocation modelId() {
            return ResourceLocation.fromNamespaceAndPath(
                    blockId.getNamespace(),
                    "block/" + blockId.getPath()
            );
        }

        public int maxOperatingTemperature() {
            return material.properties().maxOperatingTemperature();
        }
    }
}
