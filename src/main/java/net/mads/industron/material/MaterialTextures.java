package net.mads.industron.material;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Material visuals are selected dynamically from resource texture variants. */
public final class MaterialTextures {
    private MaterialTextures() {
    }

    public static Optional<MaterialVariantResolver.BlockTextureSet> blockTextures(
            IndustrialMaterial material,
            MaterialPart part
    ) {
        return MaterialVariantResolver.blockTextures(material, part);
    }

    public static Optional<ResourceLocation> blockTexture(IndustrialMaterial material, MaterialPart part) {
        return blockTextures(material, part).map(MaterialVariantResolver.BlockTextureSet::base);
    }

    public static Optional<ResourceLocation> blockSecondaryTexture(IndustrialMaterial material, MaterialPart part) {
        return blockTextures(material, part).flatMap(MaterialVariantResolver.BlockTextureSet::secondary);
    }

    public static Optional<ResourceLocation> blockOverlayTexture(IndustrialMaterial material, MaterialPart part) {
        return blockTextures(material, part).flatMap(textures -> {
            if (textures.layer2().isPresent()) return textures.layer2();
            if (textures.secondary().isPresent()) return textures.secondary();
            return textures.overlay();
        });
    }

    public static Optional<ResourceLocation> fluidStillTexture(IndustrialMaterial material, MaterialPart part) {
        return MaterialVariantResolver.texture(material, part, "base");
    }
}
