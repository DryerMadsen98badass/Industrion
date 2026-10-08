package net.mads.industron.mixin;
import net.mads.industron.climate.ClimateContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerLevel.class)
public abstract class SeasonPrecipitationMixin {
    @Inject(method="tickPrecipitation",at=@At("HEAD"))
    private void industron$begin(BlockPos pos,CallbackInfo ci){ClimateContext.push((ServerLevel)(Object)this);}
    @Inject(method="tickPrecipitation",at=@At("RETURN"))
    private void industron$end(BlockPos pos,CallbackInfo ci){ClimateContext.pop();}
}
