package net.mads.industron.validation;

import net.mads.industron.material.ElementDefinition;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialMaterials;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StructureMaterials;

import java.util.List;

/** Immutable snapshot of the domain definitions visible to one validation pass. */
public record ValidationContext(
        ValidationStage stage,
        List<ElementDefinition> elements,
        List<IndustrialMaterial> materials,
        List<StructureMaterial> structureMaterials
) {
    public ValidationContext {
        if (stage == null) throw new IllegalArgumentException("Validation stage cannot be null");
        elements = List.copyOf(elements);
        materials = List.copyOf(materials);
        structureMaterials = List.copyOf(structureMaterials);
    }

    public static ValidationContext current(ValidationStage stage) {
        return new ValidationContext(
                stage,
                IndustrialMaterials.ELEMENT_DEFINITIONS,
                IndustrialMaterials.ALL,
                StructureMaterials.ALL
        );
    }
}
