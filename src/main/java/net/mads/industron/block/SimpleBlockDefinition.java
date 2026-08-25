package net.mads.industron.block;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class SimpleBlockDefinition {

    public static final int TICKS_PER_SMELTED_ITEM = 200;

    private final String id;
    private final String displayName;
    private final String texture;
    private final FaceTextures faceTextures;
    private final Integer color;
    private final double furnaceFuelItems;
    private final MiningTier miningTier;
    private final EnumSet<MiningTool> miningTools;
    private final EnumSet<SimpleBlockVariant> variants;

    public SimpleBlockDefinition(
            String id,
            String displayName
    ) {
        this(
                id,
                displayName,
                id,
                null,
                null,
                0,
                MiningTier.STONE,
                EnumSet.of(MiningTool.PICKAXE),
                EnumSet.noneOf(SimpleBlockVariant.class)
        );
    }

    public SimpleBlockDefinition(
            String id,
            String displayName,
            String texture,
            int color
    ) {
        this(
                id,
                displayName,
                texture,
                null,
                normalizeColor(color),
                0,
                MiningTier.STONE,
                EnumSet.of(MiningTool.PICKAXE),
                EnumSet.noneOf(SimpleBlockVariant.class)
        );
    }

    public SimpleBlockDefinition(
            String id,
            String displayName,
            String frontTexture,
            String backTexture,
            String leftTexture,
            String rightTexture,
            String topTexture,
            String bottomTexture
    ) {
        this(
                id,
                displayName,
                frontTexture,
                new FaceTextures(
                        frontTexture,
                        backTexture,
                        leftTexture,
                        rightTexture,
                        topTexture,
                        bottomTexture
                ),
                null,
                0,
                MiningTier.STONE,
                EnumSet.of(MiningTool.PICKAXE),
                EnumSet.noneOf(SimpleBlockVariant.class)
        );
    }

    private SimpleBlockDefinition(
            String id,
            String displayName,
            String texture,
            FaceTextures faceTextures,
            Integer color,
            double furnaceFuelItems,
            MiningTier miningTier,
            EnumSet<MiningTool> miningTools,
            EnumSet<SimpleBlockVariant> variants
    ) {
        validateId(id);
        validateDisplayName(id, displayName);
        validateTexture(id, texture);
        validateFaceTextures(id, faceTextures);
        validateFuel(id, furnaceFuelItems);
        validateMiningTier(id, miningTier);

        this.id = id;
        this.displayName = displayName;
        this.texture = texture;
        this.faceTextures = faceTextures;

        this.color = color != null
                ? normalizeColor(color)
                : null;

        this.furnaceFuelItems = furnaceFuelItems;
        this.miningTier = miningTier;
        this.miningTools = validateMiningTools(
                id,
                miningTools
        );
        this.variants = variants.clone();
    }

    public SimpleBlockDefinition furnaceFuel(
            double items
    ) {
        return copy(
                color,
                items,
                miningTier,
                miningTools,
                variants
        );
    }

    public SimpleBlockDefinition color(
            int color
    ) {
        return copy(
                normalizeColor(color),
                furnaceFuelItems,
                miningTier,
                miningTools,
                variants
        );
    }

    public SimpleBlockDefinition wood() {
        return miningTier(
                MiningTier.WOOD
        );
    }

    public SimpleBlockDefinition stone() {
        return miningTier(
                MiningTier.STONE
        );
    }

    public SimpleBlockDefinition iron() {
        return miningTier(
                MiningTier.IRON
        );
    }

    public SimpleBlockDefinition diamond() {
        return miningTier(
                MiningTier.DIAMOND
        );
    }

    public SimpleBlockDefinition netherite() {
        return miningTier(
                MiningTier.NETHERITE
        );
    }

    public SimpleBlockDefinition miningTier(
            MiningTier tier
    ) {
        validateMiningTier(id, tier);

        return copy(
                color,
                furnaceFuelItems,
                tier,
                miningTools,
                variants
        );
    }

    public SimpleBlockDefinition mineableWith(
            MiningTool tool,
            MiningTool... moreTools
    ) {
        if (tool == null) {
            throw new IllegalArgumentException(
                    "Simple block mining tool cannot be null: "
                            + id
            );
        }

        EnumSet<MiningTool> updated =
                EnumSet.of(tool);

        if (moreTools != null) {
            for (MiningTool moreTool : moreTools) {
                if (moreTool == null) {
                    throw new IllegalArgumentException(
                            "Simple block mining tool cannot be null: "
                                    + id
                    );
                }

                updated.add(moreTool);
            }
        }

        return copy(
                color,
                furnaceFuelItems,
                miningTier,
                updated,
                variants
        );
    }

    public SimpleBlockDefinition slab() {
        return withVariant(
                SimpleBlockVariant.SLAB
        );
    }

    public SimpleBlockDefinition stair() {
        return withVariant(
                SimpleBlockVariant.STAIR
        );
    }

    public SimpleBlockDefinition wall() {
        return withVariant(
                SimpleBlockVariant.WALL
        );
    }

    public SimpleBlockDefinition fence() {
        return withVariant(
                SimpleBlockVariant.FENCE
        );
    }

    public SimpleBlockDefinition fenceGate() {
        return withVariant(
                SimpleBlockVariant.FENCE_GATE
        );
    }

    public SimpleBlockDefinition button() {
        return withVariant(
                SimpleBlockVariant.BUTTON
        );
    }

    public SimpleBlockDefinition pressurePlate() {
        return withVariant(
                SimpleBlockVariant.PRESSURE_PLATE
        );
    }

    public SimpleBlockDefinition all() {
        return new SimpleBlockDefinition(
                id,
                displayName,
                texture,
                faceTextures,
                color,
                furnaceFuelItems,
                miningTier,
                miningTools,
                EnumSet.allOf(
                        SimpleBlockVariant.class
                )
        );
    }

    private SimpleBlockDefinition withVariant(
            SimpleBlockVariant variant
    ) {
        EnumSet<SimpleBlockVariant> updated =
                variants.clone();

        updated.add(variant);

        return new SimpleBlockDefinition(
                id,
                displayName,
                texture,
                faceTextures,
                color,
                furnaceFuelItems,
                miningTier,
                miningTools,
                updated
        );
    }

    private SimpleBlockDefinition copy(
            Integer newColor,
            double newFuelItems,
            MiningTier newMiningTier,
            EnumSet<MiningTool> newMiningTools,
            EnumSet<SimpleBlockVariant> newVariants
    ) {
        return new SimpleBlockDefinition(
                id,
                displayName,
                texture,
                faceTextures,
                newColor,
                newFuelItems,
                newMiningTier,
                newMiningTools,
                newVariants
        );
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String texture() {
        return texture;
    }

    public boolean hasFaceTextures() {
        return faceTextures != null;
    }

    public FaceTextures faceTextures() {
        if (faceTextures == null) {
            throw new IllegalStateException(
                    "Simple block has no custom face textures: "
                            + id
            );
        }

        return faceTextures;
    }

    public Integer color() {
        return color;
    }

    public double furnaceFuelItems() {
        return furnaceFuelItems;
    }

    public MiningTier miningTier() {
        return miningTier;
    }

    public float hardness() {
        return miningTier.hardness();
    }

    public float resistance() {
        return miningTier.resistance();
    }

    public Set<SimpleBlockVariant> variants() {
        return Collections.unmodifiableSet(
                variants
        );
    }

    public Set<MiningTool> miningTools() {
        return Collections.unmodifiableSet(
                miningTools
        );
    }

    public boolean hasVariant(
            SimpleBlockVariant variant
    ) {
        return variants.contains(variant);
    }

    public boolean hasColor() {
        return color != null;
    }

    public int blockColor() {
        return color != null
                ? color
                : 0xFFFFFFFF;
    }

    public boolean isFurnaceFuel() {
        return furnaceFuelItems > 0;
    }

    public int furnaceBurnTimeTicks() {
        if (!isFurnaceFuel()) {
            return 0;
        }

        double ticks =
                furnaceFuelItems
                        * TICKS_PER_SMELTED_ITEM;

        if (ticks > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Fuel burn time is too large for block: "
                            + id
            );
        }

        return Math.max(
                1,
                (int) Math.round(ticks)
        );
    }

    public String variantId(
            SimpleBlockVariant variant
    ) {
        return id
                + variant.suffix();
    }

    public String variantDisplayName(
            SimpleBlockVariant variant
    ) {
        return displayName
                + " "
                + variant.displaySuffix();
    }

    private static int normalizeColor(
            int color
    ) {
        if ((color & 0xFF000000) == 0) {
            return color | 0xFF000000;
        }

        return color;
    }

    private static void validateId(
            String id
    ) {
        if (id == null
                || id.isBlank()
                || !ResourceLocation.isValidPath(id)) {
            throw new IllegalArgumentException(
                    "Invalid simple block id: "
                            + id
            );
        }
    }

    private static void validateDisplayName(
            String id,
            String displayName
    ) {
        if (displayName == null
                || displayName.isBlank()) {
            throw new IllegalArgumentException(
                    "Simple block display name cannot be blank: "
                            + id
            );
        }
    }

    private static void validateTexture(
            String id,
            String texture
    ) {
        if (texture == null
                || texture.isBlank()) {
            throw new IllegalArgumentException(
                    "Simple block texture cannot be blank: "
                            + id
            );
        }

        if (texture.contains(":")) {
            if (ResourceLocation.tryParse(texture) == null) {
                throw new IllegalArgumentException(
                        "Invalid simple block texture: "
                                + texture
                );
            }

            return;
        }

        if (!ResourceLocation.isValidPath(texture)) {
            throw new IllegalArgumentException(
                    "Invalid simple block texture path: "
                            + texture
            );
        }
    }

    private static void validateFaceTextures(
            String id,
            FaceTextures faceTextures
    ) {
        if (faceTextures == null) {
            return;
        }

        validateTexture(id, faceTextures.front());
        validateTexture(id, faceTextures.back());
        validateTexture(id, faceTextures.left());
        validateTexture(id, faceTextures.right());
        validateTexture(id, faceTextures.top());
        validateTexture(id, faceTextures.bottom());
    }

    private static void validateFuel(
            String id,
            double furnaceFuelItems
    ) {
        if (!Double.isFinite(furnaceFuelItems)
                || furnaceFuelItems < 0) {
            throw new IllegalArgumentException(
                    "furnaceFuelItems must be a finite, "
                            + "non-negative number: "
                            + id
            );
        }
    }

    private static void validateMiningTier(
            String id,
            MiningTier miningTier
    ) {
        if (miningTier == null) {
            throw new IllegalArgumentException(
                    "Simple block mining tier cannot be null: "
                            + id
            );
        }
    }

    private static EnumSet<MiningTool> validateMiningTools(
            String id,
            EnumSet<MiningTool> miningTools
    ) {
        if (miningTools == null
                || miningTools.isEmpty()) {
            throw new IllegalArgumentException(
                    "Simple block mining tools cannot be empty: "
                            + id
            );
        }

        return miningTools.clone();
    }

    public record FaceTextures(
            String front,
            String back,
            String left,
            String right,
            String top,
            String bottom
    ) {
    }
}