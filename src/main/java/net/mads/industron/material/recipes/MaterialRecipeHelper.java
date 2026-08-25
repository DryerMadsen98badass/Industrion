package net.mads.industron.material.recipes;

import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;

public final class MaterialRecipeHelper {
    private MaterialRecipeHelper() {
    }

    public static boolean hasItems(IndustrialMaterial material, MaterialPart... parts) {
        for (MaterialPart part : parts) {
            if (!material.has(part) || part.isFluid()) {
                return false;
            }
        }
        return true;
    }

    public static String itemId(IndustrialMaterial material, MaterialPart part) {
        if (material.hasExistingPart(part)) {
            return material.existingPart(part).toString();
        }
        return Industron.MOD_ID + ":" + part.registryName(material);
    }
}
