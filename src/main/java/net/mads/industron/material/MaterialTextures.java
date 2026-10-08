package net.mads.industron.material;

import net.mads.industron.Industron;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Material visuals are selected dynamically from resource texture variants. */
public final class MaterialTextures {
    private static final ResourceLocation DEFAULT_BLOCK_BASE = ResourceLocation.fromNamespaceAndPath(
            Industron.MOD_ID,
            "block/material_sets/block/variant_1/base"
    );
    private static final ResourceLocation DEFAULT_BLOCK_SECONDARY = ResourceLocation.fromNamespaceAndPath(
            Industron.MOD_ID,
            "block/material_sets/block/variant_1/secondary"
    );
    private static final ResourceLocation DEFAULT_FRAME_BASE = ResourceLocation.fromNamespaceAndPath(
            Industron.MOD_ID,
            "block/material_sets/frame/variant_1/base"
    );
    private static final ResourceLocation DEFAULT_FRAME_SECONDARY = ResourceLocation.fromNamespaceAndPath(
            Industron.MOD_ID,
            "block/material_sets/frame/variant_1/secondary"
    );
    private static final ResourceLocation DEFAULT_RAW_BLOCK_BASE = ResourceLocation.fromNamespaceAndPath(
            Industron.MOD_ID,
            "block/material_sets/raw_ore_block/variant_1/base"
    );
    private static final ResourceLocation DEFAULT_RAW_BLOCK_SECONDARY = ResourceLocation.fromNamespaceAndPath(
            Industron.MOD_ID,
            "block/material_sets/raw_ore_block/variant_1/secondary"
    );

    private MaterialTextures() {
    }

    public static Optional<MaterialVariantResolver.BlockTextureSet> blockTextures(
            IndustrialMaterial material,
            MaterialPart part
    ) {
        Optional<MaterialVariantResolver.BlockTextureSet> discovered =
                MaterialVariantResolver.blockTextures(material, part);
        if (discovered.isPresent()) {
            return discovered;
        }

        // Metal/material blocks also need a guaranteed visual fallback. runData resource discovery
        // can execute before the source texture roots are visible; without this fallback Block of X
        // remains registered but receives no blockstate/model despite the grayscale template existing.
        if (part == MaterialPart.BLOCK) {
            return Optional.of(new MaterialVariantResolver.BlockTextureSet(
                    DEFAULT_BLOCK_BASE,
                    Optional.of(DEFAULT_BLOCK_SECONDARY),
                    Optional.empty(),
                    Optional.empty()
            ));
        }

        // A frame must always receive a model. Resource discovery can run before source assets are
        // visible on some runData/IDE classpaths; without a fallback the provider silently skips the
        // blockstate, block model and item model, producing the missing-texture cube. Normal dynamic
        // variant selection is still preferred whenever discovery succeeds.
        if (part == MaterialPart.FRAME) {
            return Optional.of(new MaterialVariantResolver.BlockTextureSet(
                    DEFAULT_FRAME_BASE,
                    Optional.of(DEFAULT_FRAME_SECONDARY),
                    Optional.empty(),
                    Optional.empty()
            ));
        }

        // Raw blocks use the same guaranteed fallback as frames. Variant discovery is still tried
        // first, so variant_1, variant_2, variant_3 and any future variants remain automatic.
        if (part == MaterialPart.RAW_BLOCK) {
            return Optional.of(new MaterialVariantResolver.BlockTextureSet(
                    DEFAULT_RAW_BLOCK_BASE,
                    Optional.of(DEFAULT_RAW_BLOCK_SECONDARY),
                    Optional.empty(),
                    Optional.empty()
            ));
        }

        if (part == MaterialPart.CLAY_BLOCK) {
            return Optional.of(new MaterialVariantResolver.BlockTextureSet(
                    ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID,
                            "block/material_sets/clay/block/variant_1/base"),
                    Optional.empty(), Optional.empty(), Optional.empty()
            ));
        }

        if (part == MaterialPart.BRICKS
                || part == MaterialPart.BRICK_SLAB
                || part == MaterialPart.BRICK_STAIRS
                || part == MaterialPart.BRICK_WALL) {
            return Optional.of(new MaterialVariantResolver.BlockTextureSet(
                    ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID,
                            "block/material_sets/bricks/normal/variant_1/base"),
                    Optional.empty(), Optional.empty(), Optional.empty()
            ));
        }

        return Optional.empty();
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
