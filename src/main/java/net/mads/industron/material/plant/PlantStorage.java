package net.mads.industron.material.plant;

import net.mads.industron.material.defenitions.PlantMaterials;
import net.minecraft.resources.ResourceLocation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One world-only storage block for every plant with a declared physical source. */
public final class PlantStorage {
    public static final int CAPACITY=64;
    private PlantStorage() {}
    public static List<PlantMaterial> materials() {
        return PlantMaterials.ALL.stream().filter(m -> m.storageItem().isPresent()).toList();
    }
    private static Map<ResourceLocation,PlantMaterial> byItem;
    public static PlantMaterial forItem(ResourceLocation id) {
        if(byItem==null) {
            var map=new LinkedHashMap<ResourceLocation,PlantMaterial>();
            for(var material:materials())map.putIfAbsent(material.storageItem().orElseThrow(),material);
            byItem=Map.copyOf(map);
        }
        return byItem.get(id);
    }
    public static int height(int count) { return Math.max(1,Math.min(8,(count+7)/8))*2; }
}
