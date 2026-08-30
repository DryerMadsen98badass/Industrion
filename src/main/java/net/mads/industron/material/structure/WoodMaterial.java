package net.mads.industron.material.structure;

import net.mads.industron.material.MaterialPart;

import net.mads.industron.material.MaterialComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WoodMaterial implements StructureMaterial {
    private static final Set<MaterialPart> DEFAULT_FORMS = Set.of(
            MaterialPart.TINY_WOOD_PULP,
            MaterialPart.SMALL_WOOD_PULP,
            MaterialPart.WOOD_PULP
    );

    private final String id;
    private final String displayName;
    private final int color;
    private final WoodModel model;
    private final List<MaterialComponent> components;
    private final Map<MaterialPart, ResourceLocation> existingParts;

    public WoodMaterial(String id, String displayName, int color, WoodModel model) {
        this(id, displayName, color, model, List.of(), Map.of());
    }

    private WoodMaterial(
            String id,
            String displayName,
            int color,
            WoodModel model,
            List<MaterialComponent> components,
            Map<MaterialPart, ResourceLocation> existingParts
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Wood material id cannot be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Wood material display name cannot be blank: " + id);
        }
        if (model == null) {
            throw new IllegalArgumentException("Wood model cannot be null: " + id);
        }
        this.id = id;
        this.displayName = displayName;
        this.color = color & 0x00FFFFFF;
        this.model = model;
        this.components = List.copyOf(components);
        this.existingParts = Map.copyOf(existingParts);
    }

    public WoodMaterial contains(MaterialComponent... additions) {
        List<MaterialComponent> updated = new ArrayList<>(components);
        if (additions != null) {
            for (MaterialComponent component : additions) {
                if (component == null) {
                    throw new IllegalArgumentException("Wood material component cannot be null: " + id);
                }
                updated.add(component);
            }
        }
        return new WoodMaterial(id, displayName, color, model, updated, existingParts);
    }

    public WoodMaterial existing(MaterialPart part, String resourceLocation) {
        ResourceLocation parsed = ResourceLocation.tryParse(resourceLocation);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid existing resource location '" + resourceLocation + "' for " + id);
        }
        return existing(part, parsed);
    }

    public WoodMaterial existing(MaterialPart part, ResourceLocation resourceLocation) {
        if (part == null || resourceLocation == null) {
            throw new IllegalArgumentException("Existing wood part and resource cannot be null: " + id);
        }
        Map<MaterialPart, ResourceLocation> updated = new EnumMap<>(MaterialPart.class);
        updated.putAll(existingParts);
        ResourceLocation previous = updated.put(part, resourceLocation);
        if (previous != null && !previous.equals(resourceLocation)) {
            throw new IllegalArgumentException(
                    "Wood material " + id + " already maps " + part + " to " + previous
            );
        }
        return new WoodMaterial(id, displayName, color, model, components, updated);
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
    public WoodModel model() {
        return model;
    }

    @Override
    public List<MaterialComponent> components() {
        return components;
    }

    @Override
    public Map<MaterialPart, ResourceLocation> existingParts() {
        return existingParts;
    }

    @Override
    public Set<MaterialPart> generatedForms() {
        return DEFAULT_FORMS;
    }
}
