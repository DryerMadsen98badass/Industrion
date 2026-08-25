package net.mads.industron.material.structure;

import net.mads.industron.material.MaterialComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StoneMaterial implements StructureMaterial {
    private static final Set<StructureMaterialPart> DEFAULT_FORMS = Set.of(
            StructureMaterialPart.TINY_DUST,
            StructureMaterialPart.SMALL_DUST,
            StructureMaterialPart.DUST
    );

    private final String id;
    private final String displayName;
    private final int color;
    private final StoneModel model;
    private final List<MaterialComponent> components;
    private final Map<StructureMaterialPart, ResourceLocation> existingParts;

    public StoneMaterial(String id, String displayName, int color, StoneModel model) {
        this(id, displayName, color, model, List.of(), Map.of());
    }

    private StoneMaterial(
            String id,
            String displayName,
            int color,
            StoneModel model,
            List<MaterialComponent> components,
            Map<StructureMaterialPart, ResourceLocation> existingParts
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Stone material id cannot be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Stone material display name cannot be blank: " + id);
        }
        if (model == null) {
            throw new IllegalArgumentException("Stone model cannot be null: " + id);
        }
        this.id = id;
        this.displayName = displayName;
        this.color = color & 0x00FFFFFF;
        this.model = model;
        this.components = List.copyOf(components);
        this.existingParts = Map.copyOf(existingParts);
    }

    public StoneMaterial contains(MaterialComponent... additions) {
        List<MaterialComponent> updated = new ArrayList<>(components);
        if (additions != null) {
            for (MaterialComponent component : additions) {
                if (component == null) {
                    throw new IllegalArgumentException("Stone material component cannot be null: " + id);
                }
                updated.add(component);
            }
        }
        return new StoneMaterial(id, displayName, color, model, updated, existingParts);
    }

    public StoneMaterial existing(StructureMaterialPart part, String resourceLocation) {
        ResourceLocation parsed = ResourceLocation.tryParse(resourceLocation);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid existing resource location '" + resourceLocation + "' for " + id);
        }
        return existing(part, parsed);
    }

    public StoneMaterial existing(StructureMaterialPart part, ResourceLocation resourceLocation) {
        if (part == null || resourceLocation == null) {
            throw new IllegalArgumentException("Existing stone part and resource cannot be null: " + id);
        }
        Map<StructureMaterialPart, ResourceLocation> updated = new EnumMap<>(StructureMaterialPart.class);
        updated.putAll(existingParts);
        updated.put(part, resourceLocation);
        return new StoneMaterial(id, displayName, color, model, components, updated);
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
    public StoneModel model() {
        return model;
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
        return DEFAULT_FORMS;
    }
}
