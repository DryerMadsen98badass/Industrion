package net.mads.industron.farming;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.mads.industron.material.organism.*;

/** Cooking heat is not food age: off the fire these items last forever. */
public final class FarmingCooking {
    private FarmingCooking() {}
    public record Profile(String raw,String cooked,String burnt,int cook,int burn) {}
    public static Profile profile(ItemStack s) {
        if(s.isEmpty())return null;
        String id=BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
        Profile bread=new Profile("industron:bread_dough","minecraft:bread","industron:burnt_bread",1200,1800);
        Profile potato=new Profile("minecraft:potato","minecraft:baked_potato","industron:burnt_potato",1200,1800);
        if(matches(id,bread))return bread;if(matches(id,potato))return potato;
        if(id.startsWith("industron:cleaned_")) {
            var source=OrganismItemCatalog.byItem("minecraft:"+id.substring("industron:cleaned_".length()));
            if(source!=null) {
                var cooked=OrganismItemCatalog.form(source.owner(),source.part(),OrganicForm.COOKED);
                var burnt=OrganismItemCatalog.form(source.owner(),source.part(),OrganicForm.BURNT);
                return new Profile(id,cooked.itemId(),burnt.itemId(),800,1400);
            }
        }
        return null;
    }
    private static boolean matches(String id,Profile p) {return id.equals(p.raw())||id.equals(p.cooked())||id.equals(p.burnt());}
    public static boolean uncleanFish(ItemStack s) {
        String id=BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
        return id.equals("minecraft:cod")||id.equals("minecraft:salmon")||id.equals("minecraft:tropical_fish")||id.equals("minecraft:pufferfish");
    }
    public static int heat(ItemStack s,Profile p) {
        String id=BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
        int min=id.equals(p.burnt())?p.burn():id.equals(p.cooked())?p.cook():0;
        CustomData data=s.get(DataComponents.CUSTOM_DATA);
        return Math.min(p.burn(),Math.max(min,data==null?0:data.copyTag().getInt("industron_organic_heat")));
    }
    public static ItemStack tick(ItemStack s,Profile p) {
        int heat=Math.min(p.burn(),heat(s,p)+1);
        String id=heat>=p.burn()?p.burnt():heat>=p.cook()?p.cooked():p.raw();
        var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if(s.getItem()!=item)s=new ItemStack(item.builtInRegistryHolder(),s.getCount(),s.getComponentsPatch());
        CustomData data=s.get(DataComponents.CUSTOM_DATA);CompoundTag tag=data==null?new CompoundTag():data.copyTag();
        tag.putInt("industron_organic_heat",heat);s.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return s;
    }
}
