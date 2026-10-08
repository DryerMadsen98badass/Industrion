package net.mads.industron.tool;
import net.mads.industron.registry.ItemRegistry;
import net.mads.industron.recipe.recipes.assembly.ToolDefinitions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import java.util.Map;

/** Replace newly generated vanilla gear; existing player inventories are left intact. */
public final class EquipmentReplacement {
    private EquipmentReplacement(){}
    public static ItemStack replace(ItemStack original){
        return replace(original, false);
    }
    public static ItemStack replace(ItemStack original, boolean netherSource){
        if(original.isEmpty())return original;
        if (original.get(ToolComponents.PARTS.get()) != null) return original;
        var id=BuiltInRegistries.ITEM.getKey(original.getItem());
        if(!id.getNamespace().equals("minecraft"))return original;
        String path=id.getPath();String family=null;
        if(path.endsWith("_sword"))family="sword";
        else if(path.equals("bow")||path.equals("crossbow")||path.equals("shield")||path.equals("shears")||path.equals("fishing_rod"))family=path;
        else for(String f:new String[]{"pickaxe","axe","shovel","hoe","helmet","chestplate","leggings","boots"}) {
            if(path.endsWith("_"+f)){family=f;break;}
        }
        if(family==null)return original;
        var holder=ItemRegistry.getComposedTool(family);if(holder==null)return original;
        var definition=ToolDefinitions.ALL.stream().filter(d->d.id().equals(((MaterialEquipment)holder.get()).definition().id())).findFirst().orElseThrow();
        var preferred=preferredMaterial(path,netherSource);
        Map<String,net.mads.industron.material.IndustrialSubstance> materials=new java.util.LinkedHashMap<>();
        for(var slot:definition.parts()) {
            var selected=preferred;
            boolean woodenRole=slot.role().equals("handle")||slot.role().equals("stock")
                    ||(slot.role().equals("body")&&(family.equals("bow")||family.equals("fishing_rod")||family.equals("shield")));
            if(woodenRole)selected=net.mads.industron.material.defenitions.WoodMaterials.OAK;
            if(!ToolMaterialRules.allows(selected,slot.part())) {
                selected=ToolMaterialRules.candidates(slot.part()).stream()
                        .filter(m->m instanceof net.mads.industron.material.IndustrialMaterial metal
                                && metal.tier().equals(net.mads.industron.machine.MachineTier.ULV))
                        .findFirst().orElseThrow(()->new IllegalStateException("No early replacement material for "+slot.part()));
            }
            materials.put(slot.role(),selected);
        }
        ItemStack result=ToolStackFactory.create(definition,materials);
        if(original.has(DataComponents.ENCHANTMENTS)) result.set(DataComponents.ENCHANTMENTS,original.get(DataComponents.ENCHANTMENTS));
        if(original.has(DataComponents.CUSTOM_NAME)) result.set(DataComponents.CUSTOM_NAME,original.get(DataComponents.CUSTOM_NAME));
        if(original.isDamageableItem()) result.setDamageValue((int)Math.floor((double)original.getDamageValue()/original.getMaxDamage()*result.getMaxDamage()));
        result.setCount(original.getCount());
        return result;
    }

    private static net.mads.industron.material.IndustrialSubstance preferredMaterial(String path,boolean netherSource){
        if(path.startsWith("wooden_"))return net.mads.industron.material.defenitions.WoodMaterials.OAK;
        if(path.startsWith("stone_"))return net.mads.industron.material.defenitions.StoneMaterials.STONE;
        String material=path.startsWith("diamond_")?"diamond":path.startsWith("netherite_")?"netherite":
                path.startsWith("golden_")?(netherSource?net.mads.industron.progression.ProgressionMaterials.GOLD:
                        net.mads.industron.progression.ProgressionMaterials.COPPER):net.mads.industron.progression.ProgressionMaterials.IRON;
        return net.mads.industron.material.MaterialCatalog.find(material);
    }
}
