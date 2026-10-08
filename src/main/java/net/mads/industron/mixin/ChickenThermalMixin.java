package net.mads.industron.mixin;
import net.minecraft.world.entity.animal.Chicken;
import net.mads.industron.climate.EntityClimate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Chicken.class)
public abstract class ChickenThermalMixin {
    @Shadow public int eggTime;
    @Inject(method="aiStep",at=@At("HEAD"))
    private void industron$slowEggs(CallbackInfo ci) {
        Chicken chicken=(Chicken)(Object)this;
        if(!chicken.level().isClientSide&&chicken.tickCount%2==0&&EntityClimate.strained(chicken))eggTime++;
    }
}
