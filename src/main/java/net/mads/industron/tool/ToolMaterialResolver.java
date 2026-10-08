package net.mads.industron.tool;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.WoodMaterial;

/** Stable material identities stored on dynamic tool ItemStacks and in assembly save data. */
public final class ToolMaterialResolver {
    private static final String INDUSTRIAL = "industrial/";
    private static final String WOOD = "wood/";
    private static final String STONE = "stone/";

    private ToolMaterialResolver() {
    }

    public static String key(IndustrialSubstance material) {
        if (material instanceof WoodMaterial) return WOOD + material.id();
        if (material instanceof StoneMaterial) return STONE + material.id();
        if (material instanceof IndustrialMaterial) return INDUSTRIAL + material.id();
        throw new IllegalArgumentException("Unsupported material identity: " + material.id());
    }

    public static IndustrialSubstance resolve(String key) {
        if (key == null || key.isBlank()) return null;
        if (key.startsWith(INDUSTRIAL)) {
            String id = key.substring(INDUSTRIAL.length());
            return IndustrialMaterials.ALL.stream().filter(material -> material.id().equals(id)).findFirst().orElse(null);
        }
        if (key.startsWith(WOOD)) {
            String id = key.substring(WOOD.length());
            return WoodMaterials.ALL.stream().filter(material -> material.id().equals(id)).findFirst().orElse(null);
        }
        if (key.startsWith(STONE)) {
            String id = key.substring(STONE.length());
            return StoneMaterials.ALL.stream().filter(material -> material.id().equals(id)).findFirst().orElse(null);
        }
        return null;
    }
}
