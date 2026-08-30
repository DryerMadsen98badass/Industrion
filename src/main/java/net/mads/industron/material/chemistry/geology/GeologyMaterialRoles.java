package net.mads.industron.material.chemistry.geology;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.chemistry.MaterialAnalysis;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.structure.StoneMaterial;

import java.util.LinkedHashSet;
import java.util.Set;

/** Shared geology-role rules. These roles come from definition ownership and StoneMaterial occurrence. */
public final class GeologyMaterialRoles {
    private GeologyMaterialRoles() {
    }

    /**
     * Broad mineral role used by chemistry processing. A trace mineral in stone is still a mineral
     * input even when it deliberately owns no ore block of its own.
     */
    public static boolean isOreMineral(MaterialAnalysis analysis) {
        if (analysis == null || analysis.source().composition().isEmpty()) return false;
        return isDedicatedOre(analysis) || stoneTraceMineralIds().contains(analysis.source().id());
    }

    /** Only an OreMaterials definition owns a dedicated ore deposit/worldgen. */
    public static boolean isDedicatedOre(MaterialAnalysis analysis) {
        return analysis != null
                && analysis.source().backingMaterial() instanceof IndustrialMaterial material
                && material.isOreMaterial();
    }

    public static Set<String> stoneTraceMineralIds() {
        Set<String> result = new LinkedHashSet<>();
        for (StoneMaterial stone : StoneMaterials.ALL) {
            for (MaterialComponent component : stone.components()) {
                result.add(component.substance().id());
            }
        }
        return Set.copyOf(result);
    }
}
