package net.mads.industron.material.structure;

import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface StructureMaterial extends IndustrialSubstance {
    StructureModel model();

    List<MaterialComponent> components();

    Map<StructureMaterialPart, ResourceLocation> existingParts();

    Set<StructureMaterialPart> generatedForms();

    default boolean hasExistingPart(StructureMaterialPart part) {
        return existingParts().containsKey(part);
    }

    default ResourceLocation existingPart(StructureMaterialPart part) {
        return existingParts().get(part);
    }

    @Override
    default String formula() {
        return formula(false);
    }

    @Override
    default String formula(boolean nested) {
        if (components().isEmpty()) {
            return "";
        }

        StringBuilder formula = new StringBuilder();
        for (MaterialComponent component : components()) {
            IndustrialSubstance substance = component.substance();
            String componentFormula = substance.formula(true);
            if (componentFormula.isBlank()) {
                componentFormula = substance.displayName();
            }
            formula.append(componentFormula);
            if (component.amount() > 1) {
                formula.append(component.amount());
            }
        }

        String result = formula.toString();
        return nested ? "(" + result + ")" : result;
    }

    @Override
    default int componentTemperature() {
        // Structure materials do not have their own thermodynamic model yet.
        return 20;
    }
}
