package net.mads.industron.material.structure;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Exposes a calculated IndustrialMaterial to the decorative structure pipeline. */
public record MetalMaterial(IndustrialMaterial source) implements StructureMaterial {
    public MetalMaterial {
        if (source == null) {
            throw new IllegalArgumentException("Metal structure material source cannot be null");
        }
        if (!source.properties().metal()) {
            throw new IllegalArgumentException(source.id() + " is not classified as a metal");
        }
    }

    @Override
    public String id() {
        return source.id();
    }

    @Override
    public String displayName() {
        return source.displayName();
    }

    @Override
    public int color() {
        return source.color();
    }

    @Override
    public StructureModel model() {
        return MetalModel.ALL;
    }

    @Override
    public List<MaterialComponent> components() {
        return source.components();
    }

    @Override
    public Map<StructureMaterialPart, ResourceLocation> existingParts() {
        return Map.of();
    }

    @Override
    public Set<StructureMaterialPart> generatedForms() {
        return Set.of();
    }

    @Override
    public String formula() {
        return source.formula();
    }

    @Override
    public String formula(boolean nested) {
        return source.formula(nested);
    }

    @Override
    public int componentTemperature() {
        return source.componentTemperature();
    }
}
