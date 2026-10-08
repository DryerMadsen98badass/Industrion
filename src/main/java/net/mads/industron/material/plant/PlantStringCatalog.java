package net.mads.industron.material.plant;

import net.mads.industron.Industron;
import net.mads.industron.material.defenitions.PlantMaterials;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Canonical exact item ids that satisfy Industron's string/cord role. */
public final class PlantStringCatalog {
    public static final ResourceLocation TAG_ID =
            ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "strings");
    public static final ResourceLocation VANILLA_STRING =
            ResourceLocation.withDefaultNamespace("string");

    private PlantStringCatalog() {
    }

    public static List<ResourceLocation> allItemIds() {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        ids.add(VANILLA_STRING);
        for (PlantMaterial material : PlantMaterials.ALL) {
            if (!PlantMaterialGenerator.generates(material, PlantPart.STRING)) continue;
            ids.add(ResourceLocation.fromNamespaceAndPath(
                    Industron.MOD_ID,
                    PlantPart.STRING.registryName(material)
            ));
        }
        return List.copyOf(new ArrayList<>(ids));
    }

    /** Stable recipe-id-safe key for an exact string option. */
    public static String recipeKey(ResourceLocation id) {
        return id.getNamespace() + "_" + id.getPath().replace('/', '_');
    }
}
