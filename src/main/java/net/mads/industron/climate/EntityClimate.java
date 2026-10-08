package net.mads.industron.climate;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.tags.EntityTypeTags;

public final class EntityClimate {
    private EntityClimate() {}
    private static net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> tag(String name) {
        return net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("industron",name));
    }
    public static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> HEAT_ADAPTED=tag("heat_adapted"), COLD_ADAPTED=tag("cold_adapted"), IMMUNE=tag("climate_immune");
    public static CompoundTag state(LivingEntity entity) {
        CompoundTag root=entity.getPersistentData();
        if(!root.contains("IndustronThermal"))root.put("IndustronThermal",new CompoundTag());
        return root.getCompound("IndustronThermal");
    }
    public static double body(LivingEntity entity){CompoundTag tag=state(entity);return tag.contains("Body")?tag.getDouble("Body"):37;}
    public static boolean strained(LivingEntity entity){return ThermalRules.strained(body(entity));}
    public static double coldLimit(LivingEntity entity) {
        if(entity.getType().is(HEAT_ADAPTED))return 8;
        if(entity.getType().is(COLD_ADAPTED))return -15;
        if(entity instanceof WaterAnimal)return 1;
        if(entity instanceof PolarBear)return -40;
        if(entity instanceof SnowGolem)return -40;
        if(entity instanceof Sheep sheep)return sheep.isSheared()?0:-15;
        if(entity instanceof AbstractHorse)return -10;
        if(entity instanceof Cow)return -5;
        if(entity instanceof Camel ||entity.fireImmune())return 8;
        if(entity instanceof Animal)return 0;
        return 18;
    }
    public static double hotLimit(LivingEntity entity) {
        if(entity.getType().is(HEAT_ADAPTED))return 55;
        if(entity.getType().is(COLD_ADAPTED))return 40;
        if(entity instanceof WaterAnimal)return 28;
        if(entity instanceof SnowGolem)return 2;
        if(entity instanceof PolarBear)return 18;
        if(entity instanceof Camel ||entity.fireImmune())return 55;
        return 30;
    }
    public static boolean immune(LivingEntity entity){return entity.getType().is(EntityTypeTags.UNDEAD)||entity.getType().is(IMMUNE);}
}
