package net.mads.industron.material.structure;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialFormulaFormatter;

import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface StructureMaterial extends IndustrialSubstance {
    MachineTier tier();

    StructureModel model();

    List<MaterialComponent> components();

    Map<MaterialPart, ResourceLocation> existingParts();

    Set<MaterialPart> generatedForms();

    default boolean hasExistingPart(MaterialPart part) {
        return existingParts().containsKey(part);
    }

    default ResourceLocation existingPart(MaterialPart part) {
        return existingParts().get(part);
    }

    @Override
    default String formula() {
        return formula(false);
    }

    @Override
    default String formula(boolean nested) {
        return MaterialFormulaFormatter.compound(components(), nested);
    }

    @Override
    default int componentTemperature() {
        // Structure materials do not have their own thermodynamic model yet.
        return 20;
    }
}
