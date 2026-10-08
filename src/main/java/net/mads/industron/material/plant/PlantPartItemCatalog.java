package net.mads.industron.material.plant;

import net.mads.industron.Industron;
import net.mads.industron.material.defenitions.PlantMaterials;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Canonical physical item ids that satisfy one {@link PlantPart} role. */
public final class PlantPartItemCatalog {
    private PlantPartItemCatalog() {
    }

    public static List<ResourceLocation> allItemIds(PlantPart part) {
        if (part == null) return List.of();

        Set<ResourceLocation> ids = new LinkedHashSet<>();
        if (part == PlantPart.STRING) {
            ids.add(PlantStringCatalog.VANILLA_STRING);
        }

        for (PlantMaterial material : PlantMaterials.ALL) {
            ResourceLocation existing = material.existingParts().get(part);
            if (existing != null) {
                ids.add(existing);
                continue;
            }
            if (!PlantMaterialGenerator.generates(material, part)) continue;
            ids.add(ResourceLocation.fromNamespaceAndPath(
                    Industron.MOD_ID,
                    part.registryName(material)
            ));
        }
        return List.copyOf(new ArrayList<>(ids));
    }

    public static boolean contains(PlantPart part, ResourceLocation itemId) {
        return itemId != null && allItemIds(part).contains(itemId);
    }
}
