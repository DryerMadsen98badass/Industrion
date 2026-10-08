package net.mads.industron.material.structure;

import net.mads.industron.material.MaterialCategory;
import net.mads.industron.material.defenitions.IndustrialMaterials;

import java.util.List;

/** Industrial materials that qualify for the generic metal structure library. */
public final class MetalMaterials {
    public static final List<MetalMaterial> ALL = IndustrialMaterials.ALL.stream()
            .filter(MaterialCategory.METAL::matches)
            .map(MetalMaterial::new)
            .toList();

    private MetalMaterials() {
    }
}
