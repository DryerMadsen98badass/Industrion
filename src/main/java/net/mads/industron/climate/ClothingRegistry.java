package net.mads.industron.climate;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.bus.api.IEventBus;
/** Clothing gives insulation, not armour defense. Existing leather visuals are reused. */
public final class ClothingRegistry {
    private ClothingRegistry() {}
    public static final DeferredRegister<ArmorMaterial> MATERIALS=DeferredRegister.create(Registries.ARMOR_MATERIAL,"industron");
    public static final DeferredHolder<ArmorMaterial,ArmorMaterial> WOOL=MATERIALS.register("wool_clothing",()->{
        Map<ArmorItem.Type,Integer> defense=new EnumMap<>(ArmorItem.Type.class);
        for(ArmorItem.Type type:ArmorItem.Type.values())defense.put(type,0);
        return new ArmorMaterial(defense,10,SoundEvents.ARMOR_EQUIP_LEATHER,()->Ingredient.of(ItemTags.WOOL),
            List.of(new ArmorMaterial.Layer(ResourceLocation.withDefaultNamespace("leather"))),0,0);
    });
    public static void register(IEventBus bus){MATERIALS.register(bus);}
}
