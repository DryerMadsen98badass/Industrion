package net.mads.industron.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Four times juvenile growth, three times breeding cooldown; save loading never scales again. */
@Mixin(AgeableMob.class)
public abstract class FarmingAnimalMixin {
    @org.spongepowered.asm.mixin.Shadow protected int age;
    @Unique private int industron$beforeThermalAge;
    @Unique private boolean industron$loadingAge;
    @Unique private long industron$lastFeedDay=Long.MIN_VALUE;
    @Inject(method="aiStep",at=@At("HEAD"))
    private void industron$rememberAge(CallbackInfo ci){industron$beforeThermalAge=age;}
    @Inject(method="aiStep",at=@At("RETURN"))
    private void industron$thermalGrowth(CallbackInfo ci) {
        AgeableMob mob=(AgeableMob)(Object)this;
        if(mob instanceof Animal && !mob.level().isClientSide && mob.tickCount%2==0
            && net.mads.industron.climate.EntityClimate.strained(mob) && age!=0 && industron$beforeThermalAge!=0)
            age=industron$beforeThermalAge;
    }
    @Inject(method="ageUp(IZ)V",at=@At("HEAD"),cancellable=true)
    private void industron$limitFeeding(int seconds,boolean forced,CallbackInfo ci) {
        AgeableMob mob=(AgeableMob)(Object)this;
        if(forced && mob instanceof Animal && !mob.level().isClientSide) {
            long day=mob.level().getServer().overworld().getDayTime()/24000;
            if(industron$lastFeedDay==day)ci.cancel();else industron$lastFeedDay=day;
        }
    }
    @Inject(method="addAdditionalSaveData",at=@At("RETURN"))
    private void industron$saveFeed(CompoundTag tag,CallbackInfo ci) {tag.putLong("IndustronLastFeedDay",industron$lastFeedDay);}
    @Inject(method="readAdditionalSaveData",at=@At("HEAD"))
    private void industron$loadStart(CompoundTag tag,CallbackInfo ci) {industron$loadingAge=true;}
    @Inject(method="readAdditionalSaveData",at=@At("RETURN"))
    private void industron$loadEnd(CompoundTag tag,CallbackInfo ci) {industron$loadingAge=false;industron$lastFeedDay=tag.contains("IndustronLastFeedDay")?tag.getLong("IndustronLastFeedDay"):Long.MIN_VALUE;}
    @ModifyVariable(method="setAge",at=@At("HEAD"),argsOnly=true)
    private int industron$slowAnimals(int age) {
        AgeableMob mob=(AgeableMob)(Object)this;
        if(industron$loadingAge || !(mob instanceof Animal))return age;
        if(age==-24000 && mob.getAge()>=0)return -96000;
        if(age==6000 && mob.getAge()<=0)return 18000;
        return age;
    }
}
