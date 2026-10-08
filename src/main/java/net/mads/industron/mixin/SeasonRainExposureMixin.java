package net.mads.industron.mixin;
import net.mads.industron.climate.ClimateContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Level.class)
public abstract class SeasonRainExposureMixin {
    @Inject(method="isRainingAt",at=@At("HEAD"))
    private void industron$begin(BlockPos pos,CallbackInfoReturnable<Boolean> cir){ClimateContext.push((Level)(Object)this);}
    @Inject(method="isRainingAt",at=@At("RETURN"))
    private void industron$end(BlockPos pos,CallbackInfoReturnable<Boolean> cir){ClimateContext.pop();}
}
