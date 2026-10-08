package net.mads.industron.mixin;
import net.minecraft.world.entity.animal.Sheep;
import net.mads.industron.climate.EntityClimate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Sheep.class)
public abstract class SheepThermalMixin {
    @Inject(method="ate",at=@At("HEAD"),cancellable=true)
    private void industron$wool(CallbackInfo ci) {
        Sheep sheep=(Sheep)(Object)this;
        if(!sheep.level().isClientSide&&EntityClimate.strained(sheep))ci.cancel();
    }
}
