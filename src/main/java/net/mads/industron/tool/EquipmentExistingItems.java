package net.mads.industron.tool;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reuse vanilla armour identity and worn models through explicit .existing mappings. */
public final class EquipmentExistingItems {
    private static final Map<String, ResourceLocation> EXISTING=new LinkedHashMap<>();
    static {
        existing("diamond","helmet","minecraft:diamond_helmet");
        existing("diamond","chestplate","minecraft:diamond_chestplate");
        existing("diamond","leggings","minecraft:diamond_leggings");
        existing("diamond","boots","minecraft:diamond_boots");
        existing("netherite","helmet","minecraft:netherite_helmet");
        existing("netherite","chestplate","minecraft:netherite_chestplate");
        existing("netherite","leggings","minecraft:netherite_leggings");
        existing("netherite","boots","minecraft:netherite_boots");
    }
    private EquipmentExistingItems(){}
    private static void existing(String material,String family,String id){
        EXISTING.put(material+"/"+family,ResourceLocation.parse(id));
    }
    public static net.mads.industron.recipe.recipetypes.assembly.ToolDefinition definition(net.minecraft.world.item.ItemStack stack) {
        if (stack.get(ToolComponents.PARTS.get()) == null) return null;
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (var entry:EXISTING.entrySet()) {
            if(!entry.getValue().equals(id))continue;
            String family=entry.getKey().substring(entry.getKey().indexOf('/')+1);
            return net.mads.industron.recipe.recipes.assembly.ToolDefinitions.ALL.stream().filter(d->d.id().equals(family)).findFirst().orElse(null);
        }
        return null;
    }
    public static Item get(String material,String family){
        ResourceLocation id=EXISTING.get(material+"/"+family);
        return id==null?null:BuiltInRegistries.ITEM.get(id);
    }
}
