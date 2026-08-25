package net.mads.industron.material.structure;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialPart;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A gem in the independent decorative structure pipeline.
 *
 * <p>Calculated gems wrap an {@link IndustrialMaterial}. The optional existing-base helper
 * remains available for future external bridge content, but baseline gems such as Diamond
 * and Emerald now have real calculated element/material definitions.</p>
 *
 * <p>The base gem block itself belongs to material_sets/gem (or an existing Minecraft
 * block). Only the Quartz-derived decorative shapes belong to structure_sets/gem.</p>
 */
public final class GemMaterial implements StructureMaterial {
    private final IndustrialMaterial source;
    private final String id;
    private final String displayName;
    private final int color;
    private final List<MaterialComponent> components;
    private final String formula;
    private final int componentTemperature;
    private final Map<StructureMaterialPart, ResourceLocation> existingParts;

    public GemMaterial(IndustrialMaterial source) {
        this(
                requireGem(source),
                source.id(),
                source.displayName(),
                source.color(),
                source.components(),
                source.formula(),
                source.componentTemperature(),
                baseExistingParts(source)
        );
    }

    private GemMaterial(
            IndustrialMaterial source,
            String id,
            String displayName,
            int color,
            List<MaterialComponent> components,
            String formula,
            int componentTemperature,
            Map<StructureMaterialPart, ResourceLocation> existingParts
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Gem id cannot be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Gem display name cannot be blank: " + id);
        }
        this.source = source;
        this.id = id;
        this.displayName = displayName;
        this.color = color & 0x00FFFFFF;
        this.components = components == null ? List.of() : List.copyOf(components);
        this.formula = formula == null ? "" : formula;
        this.componentTemperature = componentTemperature;
        this.existingParts = existingParts == null ? Map.of() : Map.copyOf(existingParts);
    }

    /**
     * Creates a bridge for a vanilla gem whose material/chemistry definition does not yet
     * exist in IndustrialMaterials. Its base block is reused, never duplicated.
     */
    public static GemMaterial existingBase(
            String id,
            String displayName,
            int color,
            String baseBlock
    ) {
        return existingBase(id, displayName, color, ResourceLocation.parse(baseBlock));
    }

    public static GemMaterial existingBase(
            String id,
            String displayName,
            int color,
            ResourceLocation baseBlock
    ) {
        if (baseBlock == null) {
            throw new IllegalArgumentException("Existing gem base block cannot be null: " + id);
        }
        return new GemMaterial(
                null,
                id,
                displayName,
                color,
                List.of(),
                "",
                20,
                Map.of(StructureMaterialPart.BLOCK, baseBlock)
        );
    }

    private static IndustrialMaterial requireGem(IndustrialMaterial source) {
        if (source == null) {
            throw new IllegalArgumentException("Gem structure material source cannot be null");
        }
        if (!source.properties().gemCandidate()) {
            throw new IllegalArgumentException(source.id() + " is not classified as a gem");
        }
        return source;
    }

    private static Map<StructureMaterialPart, ResourceLocation> baseExistingParts(IndustrialMaterial source) {
        if (source == null || !source.hasExistingPart(MaterialPart.BLOCK)) {
            return Map.of();
        }
        return Map.of(StructureMaterialPart.BLOCK, source.existingPart(MaterialPart.BLOCK));
    }

    public GemMaterial existing(StructureMaterialPart part, String resourceLocation) {
        return existing(part, ResourceLocation.parse(resourceLocation));
    }

    public GemMaterial existing(StructureMaterialPart part, ResourceLocation resourceLocation) {
        if (part == null || resourceLocation == null) {
            throw new IllegalArgumentException("Existing gem structure part and resource cannot be null: " + id());
        }
        EnumMap<StructureMaterialPart, ResourceLocation> updated = new EnumMap<>(StructureMaterialPart.class);
        updated.putAll(existingParts);
        ResourceLocation previous = updated.put(part, resourceLocation);
        if (previous != null && !previous.equals(resourceLocation)) {
            throw new IllegalArgumentException(
                    "Gem " + id() + " already maps " + part + " to " + previous
            );
        }
        return new GemMaterial(
                source,
                id,
                displayName,
                color,
                components,
                formula,
                componentTemperature,
                updated
        );
    }

    /** Returns true when this gem comes from the calculated IndustrialMaterial system. */
    public boolean hasIndustrialSource() {
        return source != null;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public int color() {
        return color;
    }

    @Override
    public GemModel model() {
        return GemModel.QUARTZ;
    }

    @Override
    public List<MaterialComponent> components() {
        return components;
    }

    @Override
    public Map<StructureMaterialPart, ResourceLocation> existingParts() {
        return existingParts;
    }

    @Override
    public Set<StructureMaterialPart> generatedForms() {
        return Set.of();
    }

    @Override
    public String formula() {
        return formula;
    }

    @Override
    public String formula(boolean nested) {
        if (source != null) {
            return source.formula(nested);
        }
        return formula;
    }

    @Override
    public int componentTemperature() {
        return componentTemperature;
    }
}
