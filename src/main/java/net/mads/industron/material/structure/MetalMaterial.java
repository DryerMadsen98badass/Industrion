package net.mads.industron.material.structure;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialCategory;

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
        if (!MaterialCategory.METAL.matches(source)) {
            throw new IllegalArgumentException(source.id() + " is not a gameplay metal");
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
    public MachineTier tier() {
        return source.tier();
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
    public Map<MaterialPart, ResourceLocation> existingParts() {
        return Map.of();
    }

    @Override
    public Set<MaterialPart> generatedForms() {
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
