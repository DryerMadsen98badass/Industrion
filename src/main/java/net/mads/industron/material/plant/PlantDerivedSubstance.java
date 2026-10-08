package net.mads.industron.material.plant;

import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.CompositionColor;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialFormulaFormatter;

import java.util.List;

/**
 * Material identity of one generated plant-processing form.
 *
 * <p>The item is still registered from {@link PlantMaterial}, but processing and fuel code reads
 * this part-specific substance so every intermediate has an explicit deterministic composition.</p>
 */
public record PlantDerivedSubstance(
        PlantMaterial parent,
        PlantPart part,
        List<MaterialComponent> components
) implements IndustrialSubstance {
    public PlantDerivedSubstance {
        if (parent == null || part == null) {
            throw new IllegalArgumentException("Plant derived substance requires parent and part");
        }
        if (!part.generatedProcessingForm()) {
            throw new IllegalArgumentException("Plant derived substance requires a generated processing part: " + part);
        }
        components = List.copyOf(components);
        if (components.isEmpty()) {
            throw new IllegalArgumentException("Generated plant form " + parent.id() + "/" + part + " has no composition");
        }
    }

    @Override
    public String id() {
        return part.registryName(parent);
    }

    @Override
    public String displayName() {
        return part.readableName(parent);
    }

    @Override
    public int color() {
        return parent.optionalColor().orElseGet(() -> CompositionColor.blend(components));
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
