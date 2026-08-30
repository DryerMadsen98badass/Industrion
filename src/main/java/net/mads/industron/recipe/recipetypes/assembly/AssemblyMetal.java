package net.mads.industron.recipe.recipetypes.assembly;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;

import java.util.Objects;

/** Stable fixed material identity used by assembly recipes, or the free {@link Metal#ANY} selector. */
public record AssemblyMetal(String id) {
    public AssemblyMetal {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) throw new IllegalArgumentException("Metal id cannot be blank");
    }

    public boolean isAny() {
        return "any".equals(id);
    }

    public IndustrialMaterial resolve() {
        if (isAny()) {
            throw new IllegalStateException("Metal.ANY does not resolve to one fixed IndustrialMaterial");
        }
        return IndustrialMaterials.ALL.stream()
                .filter(material -> material.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown IndustrialMaterial for Metal." + id.toUpperCase(java.util.Locale.ROOT)));
    }
}
