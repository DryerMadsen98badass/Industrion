package net.mads.industron.climate;

import net.mads.industron.material.defenitions.PlantMaterials;
import net.mads.industron.material.plant.PlantMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;

/** Profiles are resolved from canonical material definitions, including .existing vanilla items. */
public final class PlantClimates {
    private PlantClimates() {}
    private static Map<ResourceLocation,PlantClimateProfile> items;
    private static Map<Block,PlantClimateProfile> blocks;
    private static Map<ResourceLocation,PlantClimateProfile> profiles() {
        if(items==null){var map=new HashMap<ResourceLocation,PlantClimateProfile>();var blockMap=new HashMap<Block,PlantClimateProfile>();
            for(PlantMaterial plant:PlantMaterials.ALL) {
                boolean active=false;
                for(var id:plant.existingParts().values()) {
                    var item=BuiltInRegistries.ITEM.get(id);
                    if(item instanceof net.minecraft.world.item.BlockItem blockItem && net.mads.industron.farming.CalendarPlants.supports(blockItem.getBlock().defaultBlockState())) {
                        blockMap.putIfAbsent(blockItem.getBlock(),plant.climate());active=true;
                    }
                }
                if(active)for(var id:plant.existingParts().values())map.putIfAbsent(id,plant.climate());
            }
            items=Map.copyOf(map);blocks=Map.copyOf(blockMap);}
        return items;
    }
    public static PlantClimateProfile item(ItemStack stack){return profiles().get(BuiltInRegistries.ITEM.getKey(stack.getItem()));}
    public static PlantClimateProfile block(BlockState state) {
        profiles();Block b=state.getBlock();
        if(b instanceof KelpPlantBlock)b=Blocks.KELP;
        if(b==Blocks.ATTACHED_MELON_STEM)b=Blocks.MELON_STEM;
        if(b==Blocks.ATTACHED_PUMPKIN_STEM)b=Blocks.PUMPKIN_STEM;
        var profile=blocks.get(b);
        return profile!=null?profile:(b instanceof KelpBlock)?PlantClimateProfile.AQUATIC:PlantClimateProfile.TEMPERATE;
    }
}
