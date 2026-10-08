package net.mads.industron.material;

import net.mads.industron.material.defenitions.IndustrialMaterials;

import java.util.List;

/** Single lookup/filter facade over {@link IndustrialMaterials#ALL}. */
public final class MaterialCatalog {
    private MaterialCatalog() {
    }

    public static List<IndustrialMaterial> all() {
        return IndustrialMaterials.ALL;
    }

    public static List<IndustrialMaterial> all(MaterialCategory category) {
        if (category == null) return all();
        return IndustrialMaterials.ALL.stream()
                .filter(category::matches)
                .toList();
    }

    public static IndustrialMaterial find(String id) {
        if (id == null || id.isBlank()) return null;
        return IndustrialMaterials.ALL.stream()
                .filter(material -> material.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    public static IndustrialMaterial require(String id) {
        IndustrialMaterial material = find(id);
        if (material == null) {
            throw new IllegalStateException("Unknown IndustrialMaterial: " + id);
        }
        return material;
    }
}
