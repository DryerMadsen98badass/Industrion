package net.mads.industron.material.plant;

import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.defenitions.CompoundMaterials;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Deterministic permanent-form rules derived only from PlantPart + declared .contains(...). */
public final class PlantMaterialGenerator {
    private PlantMaterialGenerator() {
    }

    public static boolean hasProcessSource(PlantMaterial material) {
        return material.existingParts().keySet().stream().anyMatch(PlantPart::biomassSource);
    }

    /** Compatibility name retained for callers written before route intermediates were separated. */
    public static boolean hasBiomassSource(PlantMaterial material) {
        return hasProcessSource(material);
    }

    /** A separable fibre route exists only for a physical fibre-capable source and direct Sylvara. */
    public static boolean hasFiberFraction(PlantMaterial material) {
        return material.existingParts().keySet().stream().anyMatch(PlantPart::fiberSource)
                && sylvaraAmount(material) > 0;
    }

    public static int totalCompositionAmount(PlantMaterial material) {
        return material.components().stream().mapToInt(MaterialComponent::amount).sum();
    }

    public static int sylvaraAmount(PlantMaterial material) {
        return material.components().stream()
                .filter(component -> component.substance().id().equals(CompoundMaterials.SYLVARA.id()))
                .mapToInt(MaterialComponent::amount)
                .sum();
    }

    public static int residueAmount(PlantMaterial material) {
        return Math.max(0, totalCompositionAmount(material) - sylvaraAmount(material));
    }

    /** Permanent generated products only. Route-only biomass/residue/compost/etc. are not PlantParts. */
    public static List<PlantPart> generatedItemForms(PlantMaterial material) {
        if (material.components().isEmpty()) {
            if (hasProcessSource(material) || !material.requestedGeneratedParts().isEmpty()) {
                throw new IllegalStateException(
                        "Plant material " + material.id() + " has processable parts but no .contains(...) composition"
                );
            }
            return List.of();
        }

        Set<PlantPart> wanted = EnumSet.noneOf(PlantPart.class);
        if (hasFiberFraction(material)) {
            wanted.add(PlantPart.FIBER);
            wanted.add(PlantPart.STRING);
        }
        wanted.addAll(material.requestedGeneratedParts());

        List<PlantPart> forms = new ArrayList<>();
        for (PlantPart part : PlantPart.values()) {
            if (!part.generatedProcessingForm() || !wanted.contains(part)) continue;
            validatePartComposition(material, part);
            forms.add(part);
        }
        return List.copyOf(forms);
    }

    public static boolean generates(PlantMaterial material, PlantPart part) {
        return generatedItemForms(material).contains(part);
    }

    /** Explicit composition for every permanent generated plant product. */
    public static List<MaterialComponent> componentsFor(PlantMaterial material, PlantPart part) {
        return switch (part) {
            case FIBER, STRING -> {
                int sylvara = sylvaraAmount(material);
                if (sylvara <= 0) yield List.of();
                yield List.of(new MaterialComponent(CompoundMaterials.SYLVARA, 1));
            }
            default -> throw new IllegalArgumentException("Not a generated plant product: " + part);
        };
    }

    public static PlantDerivedSubstance substanceFor(PlantMaterial material, PlantPart part) {
        if (!generates(material, part)) {
            throw new IllegalArgumentException("Plant material " + material.id() + " does not generate " + part);
        }
        return new PlantDerivedSubstance(material, part, componentsFor(material, part));
    }

    private static void validatePartComposition(PlantMaterial material, PlantPart part) {
        if (componentsFor(material, part).isEmpty()) {
            throw new IllegalStateException(
                    "Generated plant form " + part.registryName(material) + " would have an empty .contains(...) composition"
            );
        }
    }
}
