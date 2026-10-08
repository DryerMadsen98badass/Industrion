package net.mads.industron.climate;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.mads.industron.tool.MaterialEquipment;
import net.mads.industron.tool.ToolComponents;
import net.mads.industron.tool.ToolMaterialResolver;

/** Thermal insulation is independent of protection, toughness and electrical insulation. */
public final class ClothingInsulation {
    public static final TagKey<Item> WARM=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("industron","warm_clothing"));
    public static final TagKey<Item> LIGHT=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("industron","light_clothing"));
    private ClothingInsulation() {}
    public static double item(ItemStack stack) {
        if(stack.is(WARM))return .25;
        if(stack.is(LIGHT))return .12;
        if(stack.getItem() instanceof MaterialEquipment && stack.has(ToolComponents.PARTS.get())) {
            var data=stack.get(ToolComponents.PARTS.get());
            for(String key:data.materials().values()) {
                var material=ToolMaterialResolver.resolve(key);
                if(material!=null && (material.id().contains("leather")||material.id().contains("hide")))return .12;
            }
        }
        return 0;
    }
    public static double total(LivingEntity entity){double value=0;for(ItemStack stack:entity.getArmorSlots())value+=item(stack);return Math.min(1,value);}
}
