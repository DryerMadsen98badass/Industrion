package net.mads.industron.mixin;
import net.mads.industron.climate.EntityClimate;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Animal.class)
public abstract class AnimalThermalMixin {
    @Inject(method="canFallInLove",at=@At("HEAD"),cancellable=true)
    private void industron$breeding(CallbackInfoReturnable<Boolean> cir) {
        Animal animal=(Animal)(Object)this;
        if(!animal.level().isClientSide&&EntityClimate.strained(animal))cir.setReturnValue(false);
    }
}
