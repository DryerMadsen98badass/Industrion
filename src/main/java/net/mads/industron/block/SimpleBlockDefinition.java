package net.mads.industron.block;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.machine.MachineTierStats;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SimpleBlockDefinition {

    public static final int TICKS_PER_SMELTED_ITEM = 200;

    private final String id;
    private final String displayName;
    private final String texture;
    private final FaceTextures faceTextures;
    private final Integer color;
    private final double furnaceFuelItems;
    private final MachineTier breakingTier;
    private final BlockStrength strength;
    private final Set<ToolDefinition> miningTools;
    private final EnumSet<SimpleBlockVariant> variants;
    private final List<MaterialComponent> components;

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
                MachineTier.LV,
                null,
                Set.of(Tool.PICKAXE),
                EnumSet.noneOf(SimpleBlockVariant.class),
                List.of()
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
                MachineTier.LV,
                null,
                Set.of(Tool.PICKAXE),
                EnumSet.noneOf(SimpleBlockVariant.class),
                List.of()
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
                MachineTier.LV,
                null,
                Set.of(Tool.PICKAXE),
                EnumSet.noneOf(SimpleBlockVariant.class),
                List.of()
        );
    }

    private SimpleBlockDefinition(
            String id,
            String displayName,
            String texture,
            FaceTextures faceTextures,
            Integer color,
            double furnaceFuelItems,
            MachineTier breakingTier,
            BlockStrength strength,
            Set<ToolDefinition> miningTools,
            EnumSet<SimpleBlockVariant> variants,
            List<MaterialComponent> components
    ) {
        validateId(id);
        validateDisplayName(id, displayName);
        validateTexture(id, texture);
        validateFaceTextures(id, faceTextures);
        validateFuel(id, furnaceFuelItems);
        validateBreakingTier(id, breakingTier);

        this.id = id;
        this.displayName = displayName;
        this.texture = texture;
        this.faceTextures = faceTextures;

        this.color = color != null
                ? normalizeColor(color)
                : null;

        this.furnaceFuelItems = furnaceFuelItems;
        this.breakingTier = breakingTier;
        this.strength = strength;
        this.miningTools = validateMiningTools(
                id,
                miningTools
        );
        this.variants = variants.clone();
        this.components = List.copyOf(components == null ? List.of() : components);
    }

    public SimpleBlockDefinition furnaceFuel(
            double items
    ) {
        return copy(
                color,
                items,
                breakingTier,
                strength,
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
                breakingTier,
                strength,
                miningTools,
                variants
        );
    }

    public SimpleBlockDefinition breakingTier(
            MachineTier tier
    ) {
        validateBreakingTier(id, tier);

        return copy(
                color,
                furnaceFuelItems,
                tier,
                strength,
                miningTools,
                variants
        );
    }

    public SimpleBlockDefinition strength(float hardness) {
        return strength(hardness, hardness);
    }

    public SimpleBlockDefinition strength(float hardness, float resistance) {
        BlockStrength updated = BlockStrength.of(hardness, resistance);
        return copy(
                color,
                furnaceFuelItems,
                breakingTier,
                updated,
                miningTools,
                variants
        );
    }

    public SimpleBlockDefinition mineableWith(
            ToolDefinition tool,
            ToolDefinition... moreTools
    ) {
        if (tool == null) {
            throw new IllegalArgumentException(
                    "Simple block mining tool cannot be null: "
                            + id
            );
        }

        Set<ToolDefinition> updated = new LinkedHashSet<>();
        updated.add(tool);

        if (moreTools != null) {
            for (ToolDefinition moreTool : moreTools) {
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
                breakingTier,
                strength,
                updated,
                variants
        );
    }

    /** Adds exact material-unit composition carried by one block item. */
    public SimpleBlockDefinition contains(MaterialComponent... additions) {
        List<MaterialComponent> updated = new ArrayList<>(components);
        if (additions != null) {
            for (MaterialComponent component : additions) {
                if (component == null) {
                    throw new IllegalArgumentException("Simple block material component cannot be null: " + id);
                }
                updated.add(component);
            }
        }
        return new SimpleBlockDefinition(
                id, displayName, texture, faceTextures, color, furnaceFuelItems,
                breakingTier, strength, miningTools, variants, updated
        );
    }

    /** Convenience overload for one contained substance. */
    public SimpleBlockDefinition contains(IndustrialSubstance substance, int amount) {
        return contains(new MaterialComponent(substance, amount));
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
                breakingTier,
                strength,
                miningTools,
                EnumSet.allOf(
                        SimpleBlockVariant.class
                ),
                components
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
                breakingTier,
                strength,
                miningTools,
                updated,
                components
        );
    }

    private SimpleBlockDefinition copy(
            Integer newColor,
            double newFuelItems,
            MachineTier newBreakingTier,
            BlockStrength newStrength,
            Set<ToolDefinition> newMiningTools,
            EnumSet<SimpleBlockVariant> newVariants
    ) {
        return new SimpleBlockDefinition(
                id,
                displayName,
                texture,
                faceTextures,
                newColor,
                newFuelItems,
                newBreakingTier,
                newStrength,
                newMiningTools,
                newVariants,
                components
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


    public MachineTier breakingTier() {
        return breakingTier;
    }

    public float hardness() {
        return strength != null ? strength.hardness() : MachineTierStats.blockHardness(breakingTier);
    }

    public float resistance() {
        return strength != null ? strength.resistance() : MachineTierStats.blockResistance(breakingTier);
    }

    public boolean hasExplicitStrength() {
        return strength != null;
    }

    /** Exact material units represented by one block. */
    public List<MaterialComponent> components() {
        return components;
    }

    public Set<SimpleBlockVariant> variants() {
        return Collections.unmodifiableSet(
                variants
        );
    }

    public Set<ToolDefinition> miningTools() {
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

    private static void validateBreakingTier(
            String id,
            MachineTier breakingTier
    ) {
        if (breakingTier == null || breakingTier == MachineTier.NONE) {
            throw new IllegalArgumentException(
                    "Simple block breaking tier must be a real machine tier: "
                            + id
            );
        }
    }

    private static Set<ToolDefinition> validateMiningTools(
            String id,
            Set<ToolDefinition> miningTools
    ) {
        if (miningTools == null
                || miningTools.isEmpty()) {
            throw new IllegalArgumentException(
                    "Simple block mining tools cannot be empty: "
                            + id
            );
        }

        return Collections.unmodifiableSet(new LinkedHashSet<>(miningTools));
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