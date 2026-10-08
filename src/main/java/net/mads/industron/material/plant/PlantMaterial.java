package net.mads.industron.material.plant;

import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.CompositionColor;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialFormulaFormatter;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Material identity for one plant species/source.
 *
 * <p>Physical forms, composition and the plant climate profile share one immutable definition.
 * World growth and placement remain in their runtime systems; processing still follows the
 * canonical {@code .contains(...)} composition.</p>
 */
public final class PlantMaterial implements IndustrialSubstance {
    private final net.mads.industron.climate.PlantClimateProfile climate;
    private final String id;
    private final String displayName;
    private final Optional<Integer> color;
    private final List<MaterialComponent> components;
    private final Set<PlantPart> requestedGeneratedParts;
    private final Map<PlantPart, ResourceLocation> existingParts;
    private final Map<PlantPart, ResourceLocation> customPartTextures;

    public PlantMaterial(String id, String displayName) {
        this(id, displayName, Optional.empty(), List.of(), Set.of(), Map.of(), Map.of(), net.mads.industron.climate.PlantClimateProfile.TEMPERATE);
    }

    public PlantMaterial(String id, String displayName, int color) {
        this(id, displayName, Optional.of(color & 0x00FFFFFF), List.of(), Set.of(), Map.of(), Map.of(), net.mads.industron.climate.PlantClimateProfile.TEMPERATE);
    }

    private PlantMaterial(
            String id,
            String displayName,
            Optional<Integer> color,
            List<MaterialComponent> components,
            Set<PlantPart> requestedGeneratedParts,
            Map<PlantPart, ResourceLocation> existingParts,
            Map<PlantPart, ResourceLocation> customPartTextures,
            net.mads.industron.climate.PlantClimateProfile climate
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Plant material id cannot be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Plant material display name cannot be blank: " + id);
        }
        this.climate = java.util.Objects.requireNonNull(climate);
        this.id = id;
        this.displayName = displayName;
        this.color = color == null ? Optional.empty() : color.map(value -> value & 0x00FFFFFF);
        this.components = List.copyOf(components);
        this.requestedGeneratedParts = Set.copyOf(requestedGeneratedParts);
        this.existingParts = Map.copyOf(existingParts);
        this.customPartTextures = Map.copyOf(customPartTextures);
    }

    /**
     * Explicit visual override. When omitted the client samples the main .existing(...) item's
     * vanilla texture; common-side code falls back to deterministic .contains(...) color.
     */
    public PlantMaterial color(int color) {
        return new PlantMaterial(
                id, displayName, Optional.of(color & 0x00FFFFFF), components,
                requestedGeneratedParts, existingParts, customPartTextures, climate
        );
    }

    public PlantMaterial contains(MaterialComponent... additions) {
        List<MaterialComponent> updated = new ArrayList<>(components);
        if (additions != null) {
            for (MaterialComponent component : additions) {
                if (component == null) {
                    throw new IllegalArgumentException("Plant material component cannot be null: " + id);
                }
                updated.add(component);
            }
        }
        return new PlantMaterial(
                id, displayName, color, updated, requestedGeneratedParts, existingParts, customPartTextures, climate
        );
    }

    /**
     * Explicitly requests extra generated forms. Normal biomass/compost/fibre/string forms are
     * planned automatically and do not need to be repeated here.
     */
    public PlantMaterial parts(PlantPart... parts) {
        EnumSet<PlantPart> updated = requestedGeneratedParts.isEmpty()
                ? EnumSet.noneOf(PlantPart.class)
                : EnumSet.copyOf(requestedGeneratedParts);
        if (parts != null) {
            for (PlantPart part : parts) {
                if (part == null) {
                    throw new IllegalArgumentException("Plant material part cannot be null: " + id);
                }
                if (!part.generatedProcessingForm()) {
                    throw new IllegalArgumentException(
                            "Plant part " + part + " is not a generated processing form: " + id
                    );
                }
                updated.add(part);
            }
        }
        return new PlantMaterial(id, displayName, color, components, updated, existingParts, customPartTextures, climate);
    }

    public PlantMaterial existing(PlantPart part, String resourceLocation) {
        ResourceLocation parsed = ResourceLocation.tryParse(resourceLocation);
        if (parsed == null) {
            throw new IllegalArgumentException(
                    "Invalid existing plant resource location '" + resourceLocation + "' for " + id
            );
        }
        return existing(part, parsed);
    }

    public PlantMaterial existing(PlantPart part, ResourceLocation resourceLocation) {
        if (part == null || resourceLocation == null) {
            throw new IllegalArgumentException("Existing plant part and resource cannot be null: " + id);
        }
        if (part.generatedProcessingForm()) {
            throw new IllegalArgumentException(
                    "Generated plant processing part " + part + " must not be mapped as an existing raw plant form: " + id
            );
        }
        Map<PlantPart, ResourceLocation> updated = new EnumMap<>(PlantPart.class);
        updated.putAll(existingParts);
        ResourceLocation previous = updated.put(part, resourceLocation);
        if (previous != null && !previous.equals(resourceLocation)) {
            throw new IllegalArgumentException(
                    "Plant material " + id + " already maps " + part + " to " + previous
            );
        }
        return new PlantMaterial(
                id, displayName, color, components, requestedGeneratedParts, updated, customPartTextures, climate
        );
    }

    /** Overrides the shared generated texture for exactly one generated plant part. */
    public PlantMaterial texture(PlantPart part, String texture) {
        ResourceLocation parsed = ResourceLocation.tryParse(texture);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid plant texture '" + texture + "' for " + id);
        }
        return texture(part, parsed);
    }

    public PlantMaterial texture(PlantPart part, ResourceLocation texture) {
        if (part == null || texture == null || !part.generatedProcessingForm()) {
            throw new IllegalArgumentException("Plant texture override requires a generated processing part: " + id);
        }
        Map<PlantPart, ResourceLocation> updated = new EnumMap<>(PlantPart.class);
        updated.putAll(customPartTextures);
        updated.put(part, texture);
        return new PlantMaterial(
                id, displayName, color, components, requestedGeneratedParts, existingParts, updated, climate
        );
    }

    public net.mads.industron.climate.PlantClimateProfile climate() { return climate; }
    public PlantMaterial climate(net.mads.industron.climate.PlantClimateProfile profile) {
        return new PlantMaterial(id, displayName, color, components, requestedGeneratedParts,
            existingParts, customPartTextures, profile);
    }

    public boolean hasExistingPart(PlantPart part) {
        return existingParts.containsKey(part);
    }

    public ResourceLocation existingPart(PlantPart part) {
        return existingParts.get(part);
    }

    public boolean hasColor() {
        return color.isPresent();
    }

    public Optional<Integer> optionalColor() {
        return color;
    }

    public boolean hasCustomPartTexture(PlantPart part) {
        return customPartTextures.containsKey(part);
    }

    public ResourceLocation customPartTexture(PlantPart part) {
        return customPartTextures.get(part);
    }

    public List<MaterialComponent> components() {
        return components;
    }

    public boolean explicitlyRequests(PlantPart part) {
        return requestedGeneratedParts.contains(part);
    }

    public Set<PlantPart> requestedGeneratedParts() {
        return requestedGeneratedParts;
    }

    public Map<PlantPart, ResourceLocation> existingParts() {
        return existingParts;
    }

    /**
     * Deterministic visual source used when no explicit .color(...) is declared.
     * Prefer the actual harvested/main plant form over seeds or storage forms.
     */
    public Optional<ResourceLocation> mainExistingVisual() {
        PlantPart[] priority = {
                PlantPart.CROP, PlantPart.PLANT, PlantPart.STEM, PlantPart.ROOT,
                PlantPart.FRUIT, PlantPart.FRUIT_BLOCK, PlantPart.FLOWER, PlantPart.VINE,
                PlantPart.AQUATIC, PlantPart.FUNGUS, PlantPart.LEAVES, PlantPart.SAPLING,
                PlantPart.DRIED, PlantPart.BLOCK, PlantPart.CARPET, PlantPart.BALE,
                PlantPart.COMPRESSED_BLOCK, PlantPart.SEEDS
        };
        for (PlantPart part : priority) {
            ResourceLocation id = existingParts.get(part);
            if (id != null) return Optional.of(id);
        }
        return existingParts.values().stream().findFirst();
    }

    /** Storage uses the same primary physical source as material visuals. No speculative item forms. */
    public Optional<ResourceLocation> storageItem() { return mainExistingVisual(); }

    public String storageBlockId() { return PlantPart.STORAGE_BLOCK.registryName(this); }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    /** Common-side fallback; client rendering resolves the existing-item visual when no override exists. */
    @Override
    public int color() {
        return color.orElseGet(() -> CompositionColor.blend(components));
    }

    @Override
    public String formula() {
        return formula(false);
    }

    @Override
    public String formula(boolean nested) {
        return MaterialFormulaFormatter.compound(components, nested);
    }

    @Override
    public int componentTemperature() {
        return 20;
    }
}
