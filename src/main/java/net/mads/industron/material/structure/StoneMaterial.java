package net.mads.industron.material.structure;

import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialFormulaFormatter;
import net.mads.industron.material.MaterialOrePolicy;

import net.mads.industron.material.MaterialComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StoneMaterial implements StructureMaterial {
    private static final Set<MaterialPart> DEFAULT_FORMS = Set.of(
            MaterialPart.TINY_DUST,
            MaterialPart.SMALL_DUST,
            MaterialPart.DUST
    );

    private final String id;
    private final String displayName;
    private final int color;
    private final StoneModel model;
    private final List<MaterialComponent> components;
    private final Map<MaterialPart, ResourceLocation> existingParts;
    private final Set<MaterialPart> withoutParts;
    private final MaterialOrePolicy.DimensionBand dimension;

    public StoneMaterial(String id, String displayName, int color, StoneModel model) {
        this(id, displayName, color, model, List.of(), Map.of(), Set.of(), MaterialOrePolicy.DimensionBand.OVERWORLD);
    }

    private StoneMaterial(
            String id,
            String displayName,
            int color,
            StoneModel model,
            List<MaterialComponent> components,
            Map<MaterialPart, ResourceLocation> existingParts,
            Set<MaterialPart> withoutParts,
            MaterialOrePolicy.DimensionBand dimension
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
        this.withoutParts = Set.copyOf(withoutParts);
        this.dimension = dimension == null ? MaterialOrePolicy.DimensionBand.OVERWORLD : dimension;
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
        return new StoneMaterial(id, displayName, color, model, updated, existingParts, withoutParts, dimension);
    }

    public StoneMaterial existing(MaterialPart part, String resourceLocation) {
        ResourceLocation parsed = ResourceLocation.tryParse(resourceLocation);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid existing resource location '" + resourceLocation + "' for " + id);
        }
        return existing(part, parsed);
    }

    public StoneMaterial existing(MaterialPart part, ResourceLocation resourceLocation) {
        if (part == null || resourceLocation == null) {
            throw new IllegalArgumentException("Existing stone part and resource cannot be null: " + id);
        }
        if (withoutParts.contains(part)) {
            throw new IllegalArgumentException("Stone material " + id + " excludes " + part + " with .without(...)");
        }
        Map<MaterialPart, ResourceLocation> updated = new EnumMap<>(MaterialPart.class);
        updated.putAll(existingParts);
        ResourceLocation previous = updated.put(part, resourceLocation);
        if (previous != null && !previous.equals(resourceLocation)) {
            throw new IllegalArgumentException(
                    "Stone material " + id + " already maps " + part + " to " + previous
            );
        }
        return new StoneMaterial(id, displayName, color, model, components, updated, withoutParts, dimension);
    }

    public StoneMaterial without(MaterialPart part) {
        if (part == null) {
            throw new IllegalArgumentException("Stone material part cannot be null: " + id);
        }
        if (existingParts.containsKey(part)) {
            throw new IllegalArgumentException(
                    "Stone material " + id + " already maps " + part + " to " + existingParts.get(part)
            );
        }
        Set<MaterialPart> updated = EnumSet.noneOf(MaterialPart.class);
        updated.addAll(withoutParts);
        updated.add(part);
        return new StoneMaterial(id, displayName, color, model, components, existingParts, updated, dimension);
    }

    public boolean isWithout(MaterialPart part) {
        return withoutParts.contains(part);
    }

    public Set<MaterialPart> withoutParts() {
        return withoutParts;
    }

    /**
     * Declares the only dimension where this stone may be used by geology/worldgen.
     * The value is independent of StoneModel; model families never decide placement.
     */
    public StoneMaterial dimension(MaterialOrePolicy.DimensionBand dimension) {
        if (dimension == null) {
            throw new IllegalArgumentException("Stone dimension cannot be null: " + id);
        }
        return new StoneMaterial(id, displayName, color, model, components, existingParts, withoutParts, dimension);
    }

    public MaterialOrePolicy.DimensionBand dimension() {
        return dimension;
    }

    /** Compatibility alias for older definitions. Prefer {@link #dimension(MaterialOrePolicy.DimensionBand)}. */
    @Deprecated(forRemoval = false)
    public StoneMaterial oreHostDimension(MaterialOrePolicy.DimensionBand dimension) {
        return dimension(dimension);
    }

    /** Compatibility alias for older callers. Prefer {@link #dimension()}. */
    @Deprecated(forRemoval = false)
    public MaterialOrePolicy.DimensionBand oreHostDimension() {
        return dimension();
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

    /**
     * Stone .contains(...) amounts are independent centrifuge chances, not stoichiometric counts.
     * The formula therefore shows each contained trace material once while preserving each
     * trace material's own internal formula.
     */
    @Override
    public String formula(boolean nested) {
        return MaterialFormulaFormatter.compound(components, nested, false);
    }

    @Override
    public Map<MaterialPart, ResourceLocation> existingParts() {
        return existingParts;
    }

    @Override
    public Set<MaterialPart> generatedForms() {
        if (withoutParts.isEmpty()) {
            return DEFAULT_FORMS;
        }
        Set<MaterialPart> forms = EnumSet.copyOf(DEFAULT_FORMS);
        forms.removeAll(withoutParts);
        return Set.copyOf(forms);
    }
}
