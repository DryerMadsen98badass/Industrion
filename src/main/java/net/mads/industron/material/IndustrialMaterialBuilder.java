package net.mads.industron.material;

import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.chemistry.ChemicalStructure;
import net.mads.industron.material.chemistry.ChemistryDefinition;
import net.mads.industron.material.chemistry.ChemistryPhase;
import net.mads.industron.material.chemistry.MaterialClassification;
import net.mads.industron.material.chemistry.MaterialSource;
import net.mads.industron.material.chemistry.MaterialSourceType;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Declarative builder for fictional compound materials.
 *
 * <p>{@code .contains(...)} is the normal source of chemical composition. Formula, bulk bond
 * topology, phase, properties and tier are inferred from it and the component properties. It never
 * means "generate a synthesis recipe" by itself; acquisition is resolved separately by the
 * chemistry/source/geology systems.</p>
 */
public final class IndustrialMaterialBuilder {
    private final String id;
    private final String displayName;
    private final int color;
    private final MaterialContentProfile contentProfile;
    private final List<MaterialComponent> components = new ArrayList<>();
    private ChemicalStructure structure;
    private ChemistryPhase phase;
    private final Set<MaterialClassification> classifications = EnumSet.noneOf(MaterialClassification.class);
    private final Set<MaterialSource> sources = new LinkedHashSet<>();
    private final Map<String, Double> propertyOverrides = new LinkedHashMap<>();
    private final Set<MaterialPart> explicitParts = EnumSet.noneOf(MaterialPart.class);
    private final Map<MaterialPart, ResourceLocation> existingParts = new EnumMap<>(MaterialPart.class);
    private final Set<MaterialPart> existingRecipeParts = EnumSet.noneOf(MaterialPart.class);
    private final Map<MaterialPart, ResourceLocation> customPartTextures = new EnumMap<>(MaterialPart.class);
    private String itemMaterialSet = "dull";
    private String blockMaterialSet = "dull";
    private MachineTier tierOverride;

    public IndustrialMaterialBuilder(String id, String displayName, int color) {
        this(id, displayName, color, MaterialContentProfile.AUTO);
    }

    public IndustrialMaterialBuilder(
            String id,
            String displayName,
            int color,
            MaterialContentProfile contentProfile
    ) {
        this.id = normalizeId(id);
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("Material display name cannot be blank");
        if (color < -1 || color > 0xFFFFFF) throw new IllegalArgumentException("Material color must be 24-bit RGB or -1 for automatic: " + id);
        this.displayName = displayName;
        this.color = color;
        this.contentProfile = contentProfile == null ? MaterialContentProfile.AUTO : contentProfile;
    }

    public IndustrialMaterialBuilder contains(MaterialComponent... values) {
        if (values == null || values.length == 0) throw new IllegalArgumentException(".contains(...) requires at least one component");
        for (MaterialComponent value : values) {
            if (value == null) throw new IllegalArgumentException("Material component cannot be null");
            if (value.amount() <= 0) throw new IllegalArgumentException("Material component amount must be positive: " + value);
            components.add(value);
        }
        return this;
    }

    /**
     * Optional advanced override. Normal materials should not need this; the chemistry engine
     * infers bulk topology from .contains(...).
     */
    public IndustrialMaterialBuilder structure(ChemicalStructure value) {
        structure = value;
        return this;
    }

    public IndustrialMaterialBuilder phase(ChemistryPhase value) {
        phase = value;
        return this;
    }

    public IndustrialMaterialBuilder classification(MaterialClassification... values) {
        if (values != null) classifications.addAll(List.of(values));
        return this;
    }

    public IndustrialMaterialBuilder source(MaterialSourceType type, String sourceId) {
        sources.add(new MaterialSource(type, sourceId));
        return this;
    }

    public IndustrialMaterialBuilder property(String propertyId, double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Material property value must be finite: " + propertyId);
        propertyOverrides.put(normalizeProperty(propertyId), value);
        return this;
    }

    public IndustrialMaterialBuilder parts(MaterialPart... values) {
        if (values != null) explicitParts.addAll(List.of(values));
        return this;
    }

    public IndustrialMaterialBuilder existing(MaterialPart part, String resourceLocation) {
        return existing(part, ResourceLocation.parse(resourceLocation));
    }

    public IndustrialMaterialBuilder existing(MaterialPart part, ResourceLocation resourceLocation) {
        existingParts.put(part, resourceLocation);
        explicitParts.add(part);
        return this;
    }

    public IndustrialMaterialBuilder existingRecipe(MaterialPart... values) {
        if (values != null) existingRecipeParts.addAll(List.of(values));
        return this;
    }

    public IndustrialMaterialBuilder texture(MaterialPart part, String resourceLocation) {
        customPartTextures.put(part, ResourceLocation.parse(resourceLocation));
        return this;
    }

    public IndustrialMaterialBuilder itemMaterialSet(String value) {
        itemMaterialSet = value;
        return this;
    }

    public IndustrialMaterialBuilder blockMaterialSet(String value) {
        blockMaterialSet = value;
        return this;
    }

    /**
     * Explicit gameplay tier for clay definitions. Compound materials otherwise keep their
     * chemistry-derived tier.
     */
    public IndustrialMaterialBuilder tier(MachineTier value) {
        if (contentProfile != MaterialContentProfile.CLAY) {
            throw new IllegalStateException("Explicit .tier(...) is only supported for clay materials");
        }
        if (value == null) throw new IllegalArgumentException("Clay tier cannot be null");
        tierOverride = value;
        return this;
    }

    public IndustrialMaterial build() {
        if (components.isEmpty()) throw new IllegalStateException("Compound material " + id + " is missing .contains(...)");
        validateContentProfile();

        MaterialProperties resolvedProperties;
        MachineTier resolvedTier;
        ChemistryPhase resolvedPhase;
        Set<MaterialClassification> resolvedClassifications;

        if (contentProfile == MaterialContentProfile.CLAY) {
            ClayMaterialPropertyCalculator.Result resolved = ClayMaterialPropertyCalculator.calculate(
                    color,
                    List.copyOf(components),
                    tierOverride
            );
            resolvedProperties = resolved.properties();
            resolvedTier = resolved.tier();
            resolvedPhase = resolved.phase();
            resolvedClassifications = resolved.classifications();
        } else {
            CompoundMaterialPropertyCalculator.Result resolved = CompoundMaterialPropertyCalculator.calculate(
                    id,
                    displayName,
                    color,
                    List.copyOf(components),
                    Optional.ofNullable(structure),
                    Optional.ofNullable(phase),
                    Set.copyOf(classifications),
                    Set.copyOf(sources),
                    Map.copyOf(propertyOverrides)
            );
            resolvedProperties = resolved.properties();
            resolvedTier = resolved.tier();
            resolvedPhase = resolved.phase();
            resolvedClassifications = resolved.classifications();
        }

        Set<MaterialPart> parts = CompoundMaterialFormGenerator.partsFor(
                contentProfile,
                resolvedProperties,
                resolvedPhase,
                resolvedClassifications,
                sources,
                explicitParts,
                List.copyOf(components)
        );
        EnumSet<MaterialPart> withExisting = parts.isEmpty()
                ? EnumSet.noneOf(MaterialPart.class)
                : EnumSet.copyOf(parts);
        withExisting.addAll(existingParts.keySet());

        IndustrialMaterial material = new IndustrialMaterial(
                id,
                displayName,
                0,
                resolvedTier,
                contentProfile,
                resolvedProperties,
                itemMaterialSet,
                blockMaterialSet,
                Set.copyOf(withExisting),
                Map.copyOf(existingParts),
                Set.copyOf(existingRecipeParts),
                Map.copyOf(customPartTextures),
                resolvedProperties.radioactivity(),
                Optional.empty(),
                List.copyOf(components),
                0,
                false,
                Map.of(),
                List.of(),
                Optional.empty(),
                0,
                Optional.empty(),
                0,
                Optional.empty(),
                true
        );

        ChemistryDefinition.Builder chemistry = ChemistryDefinition.material(id)
                .phase(resolvedPhase);
        if (structure != null) chemistry.structure(structure);
        for (MaterialClassification classification : resolvedClassifications) chemistry.classification(classification);
        for (MaterialSource source : sources) chemistry.source(source.type(), source.id());
        for (Map.Entry<String, Double> property : propertyOverrides.entrySet()) chemistry.property(property.getKey(), property.getValue());
        chemistry.build();

        IndustrialMaterials.registerCompound(material);
        return material;
    }

    private void validateContentProfile() {
        if (contentProfile == MaterialContentProfile.CLAY) {
            Set<MaterialPart> generatedExplicit = EnumSet.noneOf(MaterialPart.class);
            generatedExplicit.addAll(explicitParts);
            generatedExplicit.removeAll(existingParts.keySet());
            if (!generatedExplicit.isEmpty()) {
                throw new IllegalStateException(
                        "clay(" + id + ") generates its complete form family automatically; remove explicit parts " + generatedExplicit
                );
            }

            Set<MaterialPart> invalidExisting = EnumSet.noneOf(MaterialPart.class);
            invalidExisting.addAll(existingParts.keySet());
            invalidExisting.removeAll(ClayMaterialRules.FORMS);
            if (!invalidExisting.isEmpty()) {
                throw new IllegalStateException(
                        "clay(" + id + ") may reuse only clay-family forms; remove existing forms " + invalidExisting
                );
            }

            Set<MaterialPart> invalidTextures = EnumSet.noneOf(MaterialPart.class);
            invalidTextures.addAll(customPartTextures.keySet());
            invalidTextures.removeAll(ClayMaterialRules.FORMS);
            if (!invalidTextures.isEmpty()) {
                throw new IllegalStateException(
                        "clay(" + id + ") may texture only clay-family forms; remove textures for " + invalidTextures
                );
            }

            Set<MaterialPart> invalidExistingRecipes = EnumSet.noneOf(MaterialPart.class);
            invalidExistingRecipes.addAll(existingRecipeParts);
            invalidExistingRecipes.removeAll(ClayMaterialRules.FORMS);
            if (!invalidExistingRecipes.isEmpty()) {
                throw new IllegalStateException(
                        "clay(" + id + ") may reuse recipes only for clay-family forms; remove recipe forms " + invalidExistingRecipes
                );
            }
            return;
        }
        if (contentProfile == MaterialContentProfile.CERAMIC_BRICK) {
            Set<MaterialPart> allowed = EnumSet.of(MaterialPart.BRICK, MaterialPart.CRACKED_BRICK);

            Set<MaterialPart> invalidExplicit = EnumSet.noneOf(MaterialPart.class);
            invalidExplicit.addAll(explicitParts);
            invalidExplicit.removeAll(allowed);
            if (!invalidExplicit.isEmpty()) {
                throw new IllegalStateException(
                        "ceramicBrick(" + id + ") owns only BRICK/CRACKED_BRICK; remove explicit forms " + invalidExplicit
                );
            }

            Set<MaterialPart> invalidExisting = EnumSet.noneOf(MaterialPart.class);
            invalidExisting.addAll(existingParts.keySet());
            invalidExisting.removeAll(allowed);
            if (!invalidExisting.isEmpty()) {
                throw new IllegalStateException(
                        "ceramicBrick(" + id + ") may reuse only BRICK/CRACKED_BRICK; remove existing forms " + invalidExisting
                );
            }

            Set<MaterialPart> invalidTextures = EnumSet.noneOf(MaterialPart.class);
            invalidTextures.addAll(customPartTextures.keySet());
            invalidTextures.removeAll(allowed);
            if (!invalidTextures.isEmpty()) {
                throw new IllegalStateException(
                        "ceramicBrick(" + id + ") may texture only BRICK/CRACKED_BRICK; remove textures for " + invalidTextures
                );
            }

            Set<MaterialPart> invalidExistingRecipes = EnumSet.noneOf(MaterialPart.class);
            invalidExistingRecipes.addAll(existingRecipeParts);
            invalidExistingRecipes.removeAll(allowed);
            if (!invalidExistingRecipes.isEmpty()) {
                throw new IllegalStateException(
                        "ceramicBrick(" + id + ") may reuse recipes only for BRICK/CRACKED_BRICK; remove recipe forms "
                                + invalidExistingRecipes
                );
            }
            return;
        }

        if (contentProfile != MaterialContentProfile.MINERAL_DUST) return;

        Set<MaterialPart> invalidExplicit = EnumSet.noneOf(MaterialPart.class);
        invalidExplicit.addAll(explicitParts);
        invalidExplicit.remove(MaterialPart.DUST);
        invalidExplicit.remove(MaterialPart.MOLTEN_FLUID);
        if (!invalidExplicit.isEmpty()) {
            throw new IllegalStateException(
                    "mineralDust(" + id + ") owns only DUST plus its chemistry-only molten form; remove explicit parts " + invalidExplicit
            );
        }

        Set<MaterialPart> invalidExisting = EnumSet.noneOf(MaterialPart.class);
        invalidExisting.addAll(existingParts.keySet());
        invalidExisting.remove(MaterialPart.DUST);
        invalidExisting.remove(MaterialPart.MOLTEN_FLUID);
        if (!invalidExisting.isEmpty()) {
            throw new IllegalStateException(
                    "mineralDust(" + id + ") owns only DUST plus its chemistry-only molten form; remove existing forms " + invalidExisting
            );
        }

        Set<MaterialPart> invalidTextures = EnumSet.noneOf(MaterialPart.class);
        invalidTextures.addAll(customPartTextures.keySet());
        invalidTextures.remove(MaterialPart.DUST);
        invalidTextures.remove(MaterialPart.MOLTEN_FLUID);
        if (!invalidTextures.isEmpty()) {
            throw new IllegalStateException(
                    "mineralDust(" + id + ") owns only DUST plus its chemistry-only molten form; remove textures for " + invalidTextures
            );
        }

        Set<MaterialPart> invalidExistingRecipes = EnumSet.noneOf(MaterialPart.class);
        invalidExistingRecipes.addAll(existingRecipeParts);
        invalidExistingRecipes.remove(MaterialPart.DUST);
        invalidExistingRecipes.remove(MaterialPart.MOLTEN_FLUID);
        if (!invalidExistingRecipes.isEmpty()) {
            throw new IllegalStateException(
                    "mineralDust(" + id + ") owns only DUST plus its chemistry-only molten form; remove existing recipe forms " + invalidExistingRecipes
            );
        }
    }

    private static String normalizeId(String value) {
        if (value == null) throw new IllegalArgumentException("Material id cannot be null");
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || !ResourceLocation.isValidPath(normalized)) {
            throw new IllegalArgumentException("Invalid material id: " + value);
        }
        return normalized;
    }

    private static String normalizeProperty(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Property id cannot be blank");
        return value.replace("_", "").trim().toLowerCase(Locale.ROOT);
    }
}
